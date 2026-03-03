package org.dromara.common.audio.core;

import lombok.extern.slf4j.Slf4j;
import org.dromara.common.audio.dsp.AudioFeatures;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FFmpeg 音频处理器实现
 * 提供基于FFmpeg的音频处理能力，支持更多格式
 *
 * @author VoiceText Weaver
 * @since 2026-03-01
 */
@Slf4j
@Component
public class FFmpegAudioProcessor implements AudioProcessor {

    private static final List<String> SUPPORTED_FORMATS = Arrays.asList(
        "mp3", "wav", "aac", "flac", "ogg", "m4a", "wma", "aiff", "ac3", "amr"
    );

    private static final String FFMPEG_PATH = "ffmpeg"; // 假设在PATH中
    private static final String FFPROBE_PATH = "ffprobe";

    @Override
    public File convert(File sourceFile, String targetFormat) {
        return convert(sourceFile, targetFormat, new HashMap<>());
    }

    @Override
    public File convert(File sourceFile, String targetFormat, Map<String, Object> params) {
        log.info("FFmpeg converting {} to {} with params: {}", sourceFile.getName(), targetFormat, params);

        String outputPath = sourceFile.getParent() + File.separator +
            sourceFile.getName().substring(0, sourceFile.getName().lastIndexOf('.')) +
            "_converted." + targetFormat;
        File outputFile = new File(outputPath);

        List<String> command = new ArrayList<>(Arrays.asList(
            FFMPEG_PATH,
            "-i", sourceFile.getAbsolutePath(),
            "-y" // 覆盖输出文件
        ));

        // 添加转换参数
        if (params.containsKey("bitrate")) {
            command.add("-b:a");
            command.add(params.get("bitrate").toString());
        }
        if (params.containsKey("sampleRate")) {
            command.add("-ar");
            command.add(params.get("sampleRate").toString());
        }
        if (params.containsKey("channels")) {
            command.add("-ac");
            command.add(params.get("channels").toString());
        }
        if (params.containsKey("quality")) {
            command.add("-q:a");
            command.add(params.get("quality").toString());
        }

        command.add(outputFile.getAbsolutePath());

        try {
            executeCommand(command);
            log.info("FFmpeg conversion completed: {}", outputFile.getAbsolutePath());
            return outputFile;
        } catch (Exception e) {
            log.error("FFmpeg conversion failed", e);
            throw new RuntimeException("FFmpeg conversion failed: " + e.getMessage(), e);
        }
    }

    @Override
    public File trim(File sourceFile, double startTime, double endTime) {
        log.info("FFmpeg trimming {} from {}s to {}s", sourceFile.getName(), startTime, endTime);

        double duration = endTime - startTime;
        String outputPath = sourceFile.getParent() + File.separator +
            sourceFile.getName().substring(0, sourceFile.getName().lastIndexOf('.')) +
            "_trimmed." + getFileExtension(sourceFile);
        File outputFile = new File(outputPath);

        List<String> command = Arrays.asList(
            FFMPEG_PATH,
            "-i", sourceFile.getAbsolutePath(),
            "-ss", String.valueOf(startTime),
            "-t", String.valueOf(duration),
            "-c", "copy", // 复制编码，避免重新编码
            "-y",
            outputFile.getAbsolutePath()
        );

        try {
            executeCommand(command);
            log.info("FFmpeg trim completed: {}", outputFile.getAbsolutePath());
            return outputFile;
        } catch (Exception e) {
            log.error("FFmpeg trim failed", e);
            throw new RuntimeException("FFmpeg trim failed: " + e.getMessage(), e);
        }
    }

    @Override
    public File merge(List<File> audioFiles) {
        log.info("FFmpeg merging {} audio files", audioFiles.size());

        // 创建文件列表文件
        File listFile = null;
        try {
            listFile = File.createTempFile("ffmpeg-list-", ".txt");
            java.nio.file.Files.write(listFile.toPath(), generateFileList(audioFiles));

            String outputPath = audioFiles.get(0).getParent() + File.separator +
                "merged_" + System.currentTimeMillis() + "." + getFileExtension(audioFiles.get(0));
            File outputFile = new File(outputPath);

            List<String> command = Arrays.asList(
                FFMPEG_PATH,
                "-f", "concat",
                "-safe", "0",
                "-i", listFile.getAbsolutePath(),
                "-c", "copy",
                "-y",
                outputFile.getAbsolutePath()
            );

            executeCommand(command);
            log.info("FFmpeg merge completed: {}", outputFile.getAbsolutePath());
            return outputFile;

        } catch (Exception e) {
            log.error("FFmpeg merge failed", e);
            throw new RuntimeException("FFmpeg merge failed: " + e.getMessage(), e);
        } finally {
            if (listFile != null && listFile.exists()) {
                listFile.delete();
            }
        }
    }

    @Override
    public AudioFeatures extractFeatures(File audioFile) {
        log.info("FFmpeg extracting features from: {}", audioFile.getName());

        AudioFeatures features = new AudioFeatures();
        features.setFileName(audioFile.getName());

        try {
            // 使用ffprobe获取音频信息
            List<String> command = Arrays.asList(
                FFPROBE_PATH,
                "-v", "quiet",
                "-print_format", "json",
                "-show_format",
                "-show_streams",
                audioFile.getAbsolutePath()
            );

            String output = executeCommandWithOutput(command);

            // 解析JSON输出（简化版，实际应该用JSON库）
            parseAudioInfo(output, features);

        } catch (Exception e) {
            log.error("FFmpeg feature extraction failed", e);
            throw new RuntimeException("FFmpeg feature extraction failed: " + e.getMessage(), e);
        }

        return features;
    }

    @Override
    public byte[] generateWaveform(File audioFile, int width, int height) {
        log.info("FFmpeg generating waveform for: {}", audioFile.getName());

        // 使用ffmpeg生成波形图
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        List<String> command = Arrays.asList(
            FFMPEG_PATH,
            "-i", audioFile.getAbsolutePath(),
            "-filter_complex", String.format("showwavespic=s=%dx%d", width, height),
            "-frames:v", "1",
            "-f", "image2pipe",
            "-vcodec", "png",
            "-"
        );

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();

            // 读取输出流
            InputStream inputStream = process.getInputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg waveform generation failed with exit code: " + exitCode);
            }

            return baos.toByteArray();

        } catch (Exception e) {
            log.error("FFmpeg waveform generation failed", e);
            throw new RuntimeException("FFmpeg waveform generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public double[] generateWaveformData(File audioFile, int points) {
        log.info("FFmpeg generating waveform data for: {} with {} points", audioFile.getName(), points);

        // 使用ffmpeg提取音频采样数据
        List<String> command = Arrays.asList(
            FFMPEG_PATH,
            "-i", audioFile.getAbsolutePath(),
            "-f", "f64le",  // 双精度浮点数LE格式
            "-acodec", "pcm_f64le",
            "-ac", "1",      // 单声道
            "-ar", "44100",  // 采样率
            "-t", "10",      // 限制时长，避免数据过大
            "-"
        );

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();

            // 读取双精度浮点数
            DataInputStream dis = new DataInputStream(process.getInputStream());
            List<Double> samples = new ArrayList<>();

            try {
                while (true) {
                    double sample = dis.readDouble();
                    samples.add(Math.abs(sample)); // 取绝对值表示振幅
                }
            } catch (EOFException e) {
                // 正常结束
            }

            process.waitFor();

            // 降采样到指定点数
            return downsample(samples.stream().mapToDouble(Double::doubleValue).toArray(), points);

        } catch (Exception e) {
            log.error("FFmpeg waveform data generation failed", e);
            throw new RuntimeException("FFmpeg waveform data generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> getAudioInfo(File audioFile) {
        Map<String, Object> info = new HashMap<>();

        try {
            List<String> command = Arrays.asList(
                FFPROBE_PATH,
                "-v", "quiet",
                "-print_format", "json",
                "-show_format",
                "-show_streams",
                audioFile.getAbsolutePath()
            );

            String output = executeCommandWithOutput(command);

            // 解析JSON（简化版）
            parseBasicInfo(output, info);

        } catch (Exception e) {
            log.error("Failed to get audio info with ffprobe", e);
        }

        return info;
    }

    @Override
    public boolean validateAudio(File audioFile) {
        try {
            List<String> command = Arrays.asList(
                FFPROBE_PATH,
                "-v", "error",
                "-i", audioFile.getAbsolutePath(),
                "-f", "null",
                "-"
            );

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();

            // 读取错误输出
            BufferedReader errorReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream())
            );
            String line;
            StringBuilder errors = new StringBuilder();
            while ((line = errorReader.readLine()) != null) {
                errors.append(line);
            }

            int exitCode = process.waitFor();
            boolean valid = exitCode == 0 && errors.length() == 0;

            if (!valid) {
                log.warn("Audio validation failed: {}", errors.toString());
            }

            return valid;

        } catch (Exception e) {
            log.warn("Audio validation failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<String> getSupportedFormats() {
        return SUPPORTED_FORMATS;
    }

    /**
     * 执行命令并返回输出
     */
    private String executeCommandWithOutput(List<String> command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream())
        );

        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed with exit code: " + exitCode);
        }

        return output.toString();
    }

    /**
     * 执行命令
     */
    private void executeCommand(List<String> command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // 记录输出
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream())
        );
        String line;
        while ((line = reader.readLine()) != null) {
            log.debug("FFmpeg: {}", line);
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed with exit code: " + exitCode);
        }
    }

    /**
     * 生成文件列表
     */
    private List<String> generateFileList(List<File> files) {
        List<String> lines = new ArrayList<>();
        for (File file : files) {
            lines.add("file '" + file.getAbsolutePath() + "'");
        }
        return lines;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(lastDot + 1) : "";
    }

    /**
     * 解析音频信息（简化版）
     */
    private void parseAudioInfo(String json, AudioFeatures features) {
        // 简化解析，实际应该用JSON库如Jackson
        Pattern durationPattern = Pattern.compile("\"duration\"\\s*:\\s*\"([^\"]+)\"");
        Matcher durationMatcher = durationPattern.matcher(json);
        if (durationMatcher.find()) {
            features.setDuration(Double.parseDouble(durationMatcher.group(1)));
        }

        Pattern bitratePattern = Pattern.compile("\"bit_rate\"\\s*:\\s*\"([^\"]+)\"");
        Matcher bitrateMatcher = bitratePattern.matcher(json);
        if (bitrateMatcher.find()) {
            features.setBitrate(Integer.parseInt(bitrateMatcher.group(1)));
        }

        Pattern sampleRatePattern = Pattern.compile("\"sample_rate\"\\s*:\\s*\"([^\"]+)\"");
        Matcher sampleRateMatcher = sampleRatePattern.matcher(json);
        if (sampleRateMatcher.find()) {
            features.setSampleRate(Float.parseFloat(sampleRateMatcher.group(1)));
        }

        Pattern channelsPattern = Pattern.compile("\"channels\"\\s*:\\s*(\\d+)");
        Matcher channelsMatcher = channelsPattern.matcher(json);
        if (channelsMatcher.find()) {
            features.setChannels(Integer.parseInt(channelsMatcher.group(1)));
        }
    }

    /**
     * 解析基本信息
     */
    private void parseBasicInfo(String json, Map<String, Object> info) {
        // 简化解析，实际应该用JSON库
        Pattern formatPattern = Pattern.compile("\"format_name\"\\s*:\\s*\"([^\"]+)\"");
        Matcher formatMatcher = formatPattern.matcher(json);
        if (formatMatcher.find()) {
            info.put("format", formatMatcher.group(1));
        }

        Pattern durationPattern = Pattern.compile("\"duration\"\\s*:\\s*\"([^\"]+)\"");
        Matcher durationMatcher = durationPattern.matcher(json);
        if (durationMatcher.find()) {
            info.put("duration", Double.parseDouble(durationMatcher.group(1)));
        }

        Pattern sizePattern = Pattern.compile("\"size\"\\s*:\\s*\"([^\"]+)\"");
        Matcher sizeMatcher = sizePattern.matcher(json);
        if (sizeMatcher.find()) {
            info.put("bitrate", Integer.parseInt(sizeMatcher.group(1)));
        }
    }

    /**
     * 降采样
     */
    private double[] downsample(double[] data, int targetPoints) {
        if (data.length <= targetPoints) {
            return data;
        }

        double[] result = new double[targetPoints];
        int step = data.length / targetPoints;

        for (int i = 0; i < targetPoints; i++) {
            int start = i * step;
            int end = Math.min(start + step, data.length);

            double max = 0;
            for (int j = start; j < end; j++) {
                max = Math.max(max, data[j]);
            }
            result[i] = max;
        }

        return result;
    }
}
