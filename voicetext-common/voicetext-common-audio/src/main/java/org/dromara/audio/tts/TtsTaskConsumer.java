//package org.dromara.common.audio.tts;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import jakarta.annotation.PostConstruct;
//import lombok.Data;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Component;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//
///**
// * TTS任务消费者
// * 处理异步TTS合成任务
// *
// * @author VoiceText Weaver
// * @since 2026-03-01
// */
//@Slf4j
//@Component
//public class TtsTaskConsumer {
//
//    private static final String TTS_TASK_QUEUE = "tts:tasks";
//    private static final String TTS_TASK_PROCESSING = "tts:processing";
//    private static final String TTS_TASK_RESULT = "tts:result:";
//
//    @Autowired
//    private RedisTemplate<String, String> redisTemplate;
//
//    @Autowired
//    private TtsService ttsService;
//
//    private final ObjectMapper objectMapper = new ObjectMapper();
//    private ExecutorService executorService;
//    private volatile boolean running = true;
//
//    @PostConstruct
//    public void init() {
//        this.executorService = Executors.newFixedThreadPool(5);
//        startConsumers();
//    }
//
//    /**
//     * 启动消费者
//     */
//    private void startConsumers() {
//        for (int i = 0; i < 5; i++) {
//            executorService.submit(this::consumeTasks);
//        }
//        log.info("Started TTS task consumers");
//    }
//
//    /**
//     * 消费任务
//     */
//    private void consumeTasks() {
//        while (running) {
//            try {
//                // 从队列获取任务
//                String taskJson = redisTemplate.opsForList().rightPop(
//                    TTS_TASK_QUEUE, 5, TimeUnit.SECONDS
//                );
//
//                if (taskJson != null) {
//                    processTask(taskJson);
//                }
//
//            } catch (Exception e) {
//                log.error("Error consuming TTS task", e);
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException ie) {
//                    Thread.currentThread().interrupt();
//                    break;
//                }
//            }
//        }
//    }
//
//    /**
//     * 处理单个任务
//     */
//    private void processTask(String taskJson) {
//        String taskId = null;
//        try {
//            // 解析任务
//            TaskMessage task = objectMapper.readValue(taskJson, TaskMessage.class);
//            taskId = task.getTaskId();
//
//            log.info("Processing TTS task: {}", taskId);
//
//            // 标记任务正在处理
//            redisTemplate.opsForValue().set(
//                TTS_TASK_PROCESSING + taskId,
//                "processing",
//                30,
//                TimeUnit.MINUTES
//            );
//
//            // 构建请求
//            TtsService.TtsRequest request = TtsService.TtsRequest.builder()
//                .text(task.getText())
//                .voice(task.getVoice())
//                .speed(task.getSpeed())
//                .language(task.getLanguage())
//                .format(task.getFormat())
//                .build();
//
//            // 执行合成
//            long startTime = System.currentTimeMillis();
//            TtsService.TtsResult result = ttsService.synthesize(request);
//            long duration = System.currentTimeMillis() - startTime;
//
//            // 保存结果
//            TaskResult taskResult = new TaskResult();
//            taskResult.setTaskId(taskId);
//            taskResult.setSuccess(result.isSuccess());
//            taskResult.setDuration(duration / 1000.0);
//
//            if (result.isSuccess()) {
//                taskResult.setAudioPath(result.getAudioFile().getAbsolutePath());
//                taskResult.setFileSize(result.getFileSize());
//                taskResult.setAudioDuration(result.getDuration());
//                taskResult.setFormat(result.getFormat());
//
//                // 可选：上传到OSS
//                // uploadToOSS(result.getAudioFile(), task.getTenantId());
//            } else {
//                taskResult.setError(result.getError());
//            }
//
//            // 保存结果到Redis
//            redisTemplate.opsForValue().set(
//                TTS_TASK_RESULT + taskId,
//                objectMapper.writeValueAsString(taskResult),
//                24,
//                TimeUnit.HOURS
//            );
//
//            // 清理处理标记
//            redisTemplate.delete(TTS_TASK_PROCESSING + taskId);
//
//            // 回调通知
//            if (task.getCallbackUrl() != null && !task.getCallbackUrl().isEmpty()) {
//                sendCallback(task.getCallbackUrl(), taskResult);
//            }
//
//            log.info("TTS task completed: {} in {}s", taskId, duration / 1000.0);
//
//        } catch (Exception e) {
//            log.error("Failed to process TTS task: " + taskId, e);
//
//            // 保存失败结果
//            if (taskId != null) {
//                try {
//                    TaskResult errorResult = new TaskResult();
//                    errorResult.setTaskId(taskId);
//                    errorResult.setSuccess(false);
//                    errorResult.setError(e.getMessage());
//
//                    redisTemplate.opsForValue().set(
//                        TTS_TASK_RESULT + taskId,
//                        objectMapper.writeValueAsString(errorResult),
//                        24,
//                        TimeUnit.HOURS
//                    );
//                } catch (Exception ex) {
//                    log.error("Failed to save error result", ex);
//                }
//            }
//        }
//    }
//
//    /**
//     * 提交任务到队列
//     *
//     * @param task 任务
//     * @return 任务ID
//     */
//    public String submitTask(TaskMessage task) {
//        try {
//            String taskId = task.getTaskId();
//            if (taskId == null) {
//                taskId = "tts_" + System.currentTimeMillis() + "_" +
//                         task.getText().hashCode();
//                task.setTaskId(taskId);
//            }
//
//            String taskJson = objectMapper.writeValueAsString(task);
//            redisTemplate.opsForList().leftPush(TTS_TASK_QUEUE, taskJson);
//
//            log.info("Submitted TTS task: {}", taskId);
//            return taskId;
//
//        } catch (Exception e) {
//            log.error("Failed to submit TTS task", e);
//            throw new RuntimeException("Failed to submit TTS task", e);
//        }
//    }
//
//    /**
//     * 获取任务结果
//     *
//     * @param taskId 任务ID
//     * @return 任务结果
//     */
//    public TaskResult getTaskResult(String taskId) {
//        try {
//            String resultJson = redisTemplate.opsForValue().get(TTS_TASK_RESULT + taskId);
//            if (resultJson != null) {
//                return objectMapper.readValue(resultJson, TaskResult.class);
//            }
//
//            // 检查是否正在处理
//            String processing = redisTemplate.opsForValue().get(TTS_TASK_PROCESSING + taskId);
//            if (processing != null) {
//                TaskResult pending = new TaskResult();
//                pending.setTaskId(taskId);
//                pending.setSuccess(false);
//                pending.setError("PROCESSING");
//                return pending;
//            }
//
//            return null;
//
//        } catch (Exception e) {
//            log.error("Failed to get task result", e);
//            return null;
//        }
//    }
//
//    /**
//     * 发送回调
//     */
//    private void sendCallback(String callbackUrl, TaskResult result) {
//        // 简化实现，实际应该使用HTTP客户端发送
//        log.info("Callback to {} with result: {}", callbackUrl, result);
//    }
//
//    /**
//     * 关闭消费者
//     */
//    public void shutdown() {
//        running = false;
//        if (executorService != null) {
//            executorService.shutdown();
//            try {
//                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
//                    executorService.shutdownNow();
//                }
//            } catch (InterruptedException e) {
//                executorService.shutdownNow();
//                Thread.currentThread().interrupt();
//            }
//        }
//    }
//
//    /**
//     * 任务消息
//     */
//    @Data
//    public static class TaskMessage {
//        private String taskId;
//        private String tenantId;
//        private String userId;
//        private String text;
//        private String voice = "zh_female_1";
//        private double speed = 1.0;
//        private String language = "zh";
//        private String format = "mp3";
//        private String callbackUrl;
//        private Map<String, Object> metadata = new HashMap<>();
//    }
//
//    /**
//     * 任务结果
//     */
//    @Data
//    public static class TaskResult {
//        private String taskId;
//        private boolean success;
//        private String audioPath;
//        private long fileSize;
//        private double audioDuration;
//        private String format;
//        private double duration;  // 处理耗时
//        private String error;
//        private Map<String, Object> metadata = new HashMap<>();
//    }
//}
