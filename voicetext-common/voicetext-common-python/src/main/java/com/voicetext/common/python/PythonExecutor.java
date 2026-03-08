package com.voicetext.common.python;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicetext.common.core.config.SystemConfig;
import com.voicetext.common.python.processor.BatchProcessor;
import jakarta.annotation.PreDestroy;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * 统一Python脚本执行器（优化版）
 */
@Service
@Slf4j
public class PythonExecutor {

    private final SystemConfig config;
    private final ExecutorService executorService;
    private final Path scriptDir;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final long pythonTimeoutSeconds;

    public PythonExecutor(SystemConfig config) {
        this.config = config;
        this.pythonTimeoutSeconds = config.getPython().getTimeout();
        // 优化线程池配置
        this.executorService = createExecutorService();
        // 初始化脚本目录
        this.scriptDir = initScriptDirectory();
    }

    private ExecutorService createExecutorService() {
        int coreThreads = Math.max(1, config.getPython().getMaxConcurrent() / 2);
        int maxThreads = config.getPython().getMaxConcurrent();

        return new ThreadPoolExecutor(
                coreThreads,
                maxThreads,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                r -> {
                    Thread thread = new Thread(r);
                    thread.setName("python-executor-" + thread.getId());
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    private Path initScriptDirectory() {
        try {
            Path dir = Paths.get(config.getPython().getResolvedScriptDir()).toAbsolutePath();
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                log.info("创建Python脚本目录: {}", dir);
            }
            return dir;
        } catch (IOException e) {
            log.error("初始化脚本目录失败", e);
            throw new RuntimeException("脚本目录初始化失败", e);
        }
    }

    /**
     * 执行Python脚本（统一入口）
     */
    public PythonResult execute(String scriptName, Object inputData) {
        return execute(scriptName, inputData, Collections.emptyList());
    }

    /**
     * 执行Python脚本（带参数）
     */
    public PythonResult execute(String scriptName, Object inputData, List<String> args) {
        return execute(scriptName, inputData, args, null);
    }

    /**
     * 执行Python脚本（带参数+执行模式）
     */
    public PythonResult execute(String scriptName, Object inputData, List<String> args, BatchProcessor.BatchMode model) {
        String executionId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        try {
            // 验证脚本
            Path scriptPath = validateScript(scriptName);

            // 序列化输入数据
            String jsonData = objectMapper.writeValueAsString(inputData);

            // 判断传输方式
            boolean useFileTransfer = shouldUseFileTransfer(jsonData, scriptName, model);

            // 异步执行
            Callable<PythonResult> task = useFileTransfer ?
                    () -> executeWithFile(scriptPath, jsonData, args, executionId) :
                    () -> executeWithStdin(scriptPath, jsonData, args, executionId);

            PythonResult result = executeWithTimeout(task, scriptName, executionId);
            result.setExecutionTime(System.currentTimeMillis() - startTime);
            return result;

        } catch (Exception e) {
            log.error("执行Python脚本失败, executionId={}", executionId, e);
            return PythonResult.error(executionId, "执行异常: " + e.getMessage());
        }
    }

    private Path validateScript(String scriptName) {
        Path scriptPath = scriptDir.resolve(scriptName);
        if (!Files.exists(scriptPath)) {
            throw new IllegalArgumentException("脚本不存在: " + scriptPath);
        }
        return scriptPath;
    }

    private PythonResult executeWithTimeout(Callable<PythonResult> task, String scriptName, String executionId) {
        Future<PythonResult> future = executorService.submit(task);
        try {
            return future.get(pythonTimeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.error("Python脚本执行超时（{}秒）: {}, executionId={}", pythonTimeoutSeconds, scriptName, executionId);
            return PythonResult.error(executionId, "脚本执行超时: " + pythonTimeoutSeconds + "秒");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Python脚本执行被中断: {}, executionId={}", scriptName, executionId);
            return PythonResult.error(executionId, "脚本执行被中断");
        } catch (ExecutionException e) {
            log.error("Python脚本执行异常: {}, executionId={}", scriptName, executionId, e.getCause());
            return PythonResult.error(executionId, "脚本执行异常: " + e.getCause().getMessage());
        }
    }

    /**
     * 标准输入方式执行
     */
    private PythonResult executeWithStdin(Path scriptPath, String jsonData, List<String> args, String executionId) throws Exception {
        List<String> command = buildCommand(scriptPath, args, "--stdin-mode");

        ProcessBuilder pb = new ProcessBuilder(command)
                .directory(scriptDir.toFile())
                .redirectErrorStream(false);
        pb.environment().put("PYTHONIOENCODING", "utf-8");

        Process process = null;
        ExecutorService streamReaderPool = Executors.newFixedThreadPool(2);

        try {
            process = pb.start();

            // 异步读取输出
            Future<ProcessOutput> outputFuture = streamReaderPool.submit(
                    new StreamReader(process.getInputStream(), process.getErrorStream())
            );

            // 写入输入数据
            try (BufferedWriter stdinWriter = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
                stdinWriter.write(jsonData);
                stdinWriter.flush();
            }

            // 等待进程完成
            if (!process.waitFor(pythonTimeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new TimeoutException("Python脚本执行超时");
            }

            ProcessOutput output = outputFuture.get(5, TimeUnit.SECONDS);

            return buildResult(executionId, process.exitValue(),
                    output.stdout, output.stderr, scriptPath.toString());

        } finally {
            cleanup(process, streamReaderPool);
        }
    }

    /**
     * 文件方式执行
     */
    private PythonResult executeWithFile(Path scriptPath, String jsonData, List<String> args, String executionId) throws Exception {
        Path tempFile = null;
        ExecutorService streamReaderPool = Executors.newFixedThreadPool(2);

        try {
            // 创建临时文件
            tempFile = Files.createTempFile("python_input_", ".json");
            Files.write(tempFile, jsonData.getBytes(StandardCharsets.UTF_8));

            List<String> command = buildCommand(scriptPath, args, "--data-file", tempFile.toString());

            ProcessBuilder pb = new ProcessBuilder(command)
                    .directory(scriptDir.toFile())
                    .redirectErrorStream(false);

            Process process = pb.start();

            // 异步读取输出
            Future<ProcessOutput> outputFuture = streamReaderPool.submit(
                    new StreamReader(process.getInputStream(), process.getErrorStream())
            );

            // 等待进程完成
            if (!process.waitFor(pythonTimeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new TimeoutException("Python脚本执行超时");
            }

            ProcessOutput output = outputFuture.get(5, TimeUnit.SECONDS);

            PythonResult result = buildResult(executionId, process.exitValue(),
                    output.stdout, output.stderr, scriptPath.toString());
            result.setDataFilePath(tempFile.toString());

            return result;

        } finally {
            // 清理临时文件
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    log.warn("删除临时文件失败: {}", tempFile, e);
                }
            }
            streamReaderPool.shutdownNow();
        }
    }

    /**
     * 流读取任务
     */
    private static class StreamReader implements Callable<ProcessOutput> {
        private final InputStream stdout;
        private final InputStream stderr;

        public StreamReader(InputStream stdout, InputStream stderr) {
            this.stdout = stdout;
            this.stderr = stderr;
        }

        @Override
        public ProcessOutput call() throws Exception {
            try (BufferedReader stdoutReader = new BufferedReader(
                    new InputStreamReader(stdout, StandardCharsets.UTF_8));
                 BufferedReader stderrReader = new BufferedReader(
                         new InputStreamReader(stderr, StandardCharsets.UTF_8))) {

                CompletableFuture<String> stdoutFuture = CompletableFuture.supplyAsync(() -> readAllLines(stdoutReader));
                CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(() -> readAllLines(stderrReader));

                return new ProcessOutput(stdoutFuture.get(), stderrFuture.get());
            }
        }
    }

    @lombok.Value
    private static class ProcessOutput {
        String stdout;
        String stderr;
    }

    private PythonResult buildResult(String executionId, int exitCode, String output, String error, String scriptPath) {
        return PythonResult.builder()
                .executionId(executionId)
                .exitCode(exitCode)
                .output(output)
                .error(error)
                .status(exitCode == 0 ? "SUCCESS" : "FAILED")
                .scriptPath(scriptPath)
                .build();
    }

    private void cleanup(Process process, ExecutorService pool) {
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }
        if (pool != null) {
            pool.shutdownNow();
            try {
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("流读取线程池未正常关闭");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 判断是否使用文件传输方式
     */
    private boolean shouldUseFileTransfer(String jsonData, String scriptName, BatchProcessor.BatchMode model) {
        boolean largeData = jsonData.length() > 1024 * 1024; // 1MB
        boolean sleepDepthScript = BatchProcessor.BatchType.SLEEP_DEPTH.getScriptName().equals(scriptName);
        boolean fileMode = model != null && BatchProcessor.BatchMode.DATA_FILE.getModelName().equals(model.getModelName());

        return largeData || sleepDepthScript || fileMode;
    }

    /**
     * 统一构建Python命令
     */
    private List<String> buildCommand(Path scriptPath, List<String> args, String... extraParams) {
        List<String> command = new ArrayList<>();
        command.add(config.getPython().getSmartExecutable());
        command.add(scriptPath.toString());

        if (extraParams != null) {
            Collections.addAll(command, extraParams);
        }
        if (args != null && !args.isEmpty()) {
            command.addAll(args);
        }
        return command;
    }

    /**
     * 读取所有行
     */
    private static String readAllLines(BufferedReader reader) {
        StringBuilder sb = new StringBuilder();
        try (reader) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append(System.lineSeparator());
                }
                sb.append(line);
            }
        } catch (IOException e) {
            log.error("读取流失败", e);
        }
        return sb.toString();
    }

    /**
     * Spring容器关闭时清理资源
     */
    @PreDestroy
    public void shutdown() {
        log.info("关闭PythonExecutor线程池...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.error("PythonExecutor线程池未能正常关闭");
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("PythonExecutor线程池已关闭");
    }

    @Data
    @Builder
    public static class PythonResult {
        private String executionId;
        private int exitCode;
        private String output;
        private String error;
        private String status;
        private long executionTime;
        private String scriptPath;
        private String dataFilePath;

        public static PythonResult error(String executionId, String errorMessage) {
            return PythonResult.builder()
                    .executionId(executionId)
                    .exitCode(-1)
                    .error(errorMessage)
                    .status("ERROR")
                    .build();
        }

        public boolean isSuccess() {
            return exitCode == 0 && "SUCCESS".equals(status);
        }
    }
}
