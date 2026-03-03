package org.dromara.common.audio.tts;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * TTS语音合成服务
 * 通过HTTP调用Coqui TTS服务
 *
 * @author VoiceText Weaver
 * @since 2026-03-01
 */
@Slf4j
@Service
public class TtsService {

    @Value("${tts.service.url:http://localhost:9001}")
    private String serviceUrl;

    @Value("${tts.service.timeout:30}")
    private int timeoutSeconds;

    @Value("${tts.output.path:./tts_output}")
    private String outputPath;

    private OkHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .build();

        // 创建输出目录
        File outputDir = new File(outputPath);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
    }

    /**
     * 文本转语音
     *
     * @param request 合成请求
     * @return 合成结果
     */
    public TtsResult synthesize(TtsRequest request) {
        log.info("Synthesizing text: {}...", truncateText(request.getText(), 50));

        try {
            // 构建请求
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("text", request.getText());
            requestBody.put("voice", request.getVoice());
            requestBody.put("speed", request.getSpeed());
            requestBody.put("language", request.getLanguage());

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            Request httpRequest = new Request.Builder()
                .url(serviceUrl + "/synthesize")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("TTS service returned error: " + response.code());
                }

                // 保存音频文件
                String filename = generateFilename(request);
                File outputFile = new File(outputPath, filename);

                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    fos.write(response.body().bytes());
                }

                // 获取音频信息
                AudioInfo audioInfo = getAudioInfo(outputFile);

                TtsResult result = new TtsResult();
                result.setSuccess(true);
                result.setAudioFile(outputFile);
                result.setDuration(audioInfo.getDuration());
                result.setFileSize(outputFile.length());
                result.setFormat(audioInfo.getFormat());

                log.info("TTS synthesis completed: {}", outputFile.getAbsolutePath());
                return result;
            }

        } catch (IOException e) {
            log.error("TTS synthesis failed", e);
            TtsResult result = new TtsResult();
            result.setSuccess(false);
            result.setError(e.getMessage());
            return result;
        }
    }

    /**
     * 异步合成
     *
     * @param request 合成请求
     * @param callbackUrl 回调URL
     * @return 任务ID
     */
    public String synthesizeAsync(TtsRequest request, String callbackUrl) {
        log.info("Async TTS: {}...", truncateText(request.getText(), 50));

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("text", request.getText());
            requestBody.put("voice", request.getVoice());
            requestBody.put("speed", request.getSpeed());
            requestBody.put("language", request.getLanguage());
            requestBody.put("callback", callbackUrl);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            Request httpRequest = new Request.Builder()
                .url(serviceUrl + "/synthesize_async")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Async TTS failed: " + response.code());
                }

                Map<String, Object> result = objectMapper.readValue(response.body().string(), Map.class);
                return (String) result.get("task_id");
            }

        } catch (IOException e) {
            log.error("Async TTS failed", e);
            throw new RuntimeException("Async TTS failed: " + e.getMessage(), e);
        }
    }

    /**
     * 获取可用音色列表
     *
     * @return 音色列表
     */
    public VoicesResponse getAvailableVoices() {
        try {
            Request request = new Request.Builder()
                .url(serviceUrl + "/voices")
                .get()
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Failed to get voices: " + response.code());
                }

                String responseBody = response.body().string();
                return objectMapper.readValue(responseBody, VoicesResponse.class);
            }

        } catch (IOException e) {
            log.error("Failed to get voices", e);
            return new VoicesResponse();
        }
    }

    /**
     * 获取音频信息
     */
    private AudioInfo getAudioInfo(File audioFile) {
        // 简化实现，实际应该使用FFmpeg或TarsosDSP获取
        AudioInfo info = new AudioInfo();
        info.setFormat(getFileExtension(audioFile));
        info.setDuration(estimateDuration(audioFile.length()));
        return info;
    }

    /**
     * 生成文件名
     */
    private String generateFilename(TtsRequest request) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomId = UUID.randomUUID().toString().substring(0, 8);
        String format = request.getFormat() != null ? request.getFormat() : "mp3";
        return String.format("tts_%s_%s.%s", timestamp, randomId, format);
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
     * 估算音频时长（基于文件大小，粗略估计）
     * MP3 128kbps 约 1MB/分钟
     */
    private double estimateDuration(long fileSize) {
        return fileSize / (128.0 * 1024 / 8 * 60);
    }

    /**
     * 截断文本
     */
    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ?
            text.substring(0, maxLength) + "..." : text;
    }

    /**
     * TTS请求参数
     */
    @Data
    public static class TtsRequest {
        private String text;           // 合成文本
        private String voice = "zh_female_1";  // 音色
        private double speed = 1.0;     // 语速 0.5-2.0
        private double pitch = 1.0;      // 音调 0.5-2.0
        private String language = "zh";  // 语言
        private String format = "mp3";   // 输出格式: mp3, wav, ogg
        private Map<String, Object> extraParams = new HashMap<>();

        public static TtsRequestBuilder builder() {
            return new TtsRequestBuilder();
        }

        public static class TtsRequestBuilder {
            private TtsRequest request = new TtsRequest();

            public TtsRequestBuilder text(String text) {
                request.setText(text);
                return this;
            }

            public TtsRequestBuilder voice(String voice) {
                request.setVoice(voice);
                return this;
            }

            public TtsRequestBuilder speed(double speed) {
                request.setSpeed(speed);
                return this;
            }

            public TtsRequestBuilder pitch(double pitch) {
                request.setPitch(pitch);
                return this;
            }

            public TtsRequestBuilder language(String language) {
                request.setLanguage(language);
                return this;
            }

            public TtsRequestBuilder format(String format) {
                request.setFormat(format);
                return this;
            }

            public TtsRequestBuilder extraParam(String key, Object value) {
                request.getExtraParams().put(key, value);
                return this;
            }

            public TtsRequest build() {
                return request;
            }
        }
    }

    /**
     * TTS结果
     */
    @Data
    public static class TtsResult {
        private boolean success;
        private File audioFile;
        private double duration;      // 音频时长（秒）
        private long fileSize;         // 文件大小（字节）
        private String format;         // 格式
        private String error;          // 错误信息
    }

    /**
     * 音频信息
     */
    @Data
    public static class AudioInfo {
        private String format;
        private double duration;
        private int sampleRate;
        private int channels;
    }

    /**
     * 音色列表响应
     */
    @Data
    public static class VoicesResponse {
        private List<VoiceInfo> voices;
        private String defaultVoice;

        @Data
        public static class VoiceInfo {
            private String id;
            private String name;
            private String language;
            private String gender;
            private String description;
        }
    }
}
