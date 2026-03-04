package org.dromara.audio.whisper;//package org.dromara.common.audio.whisper;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.google.common.net.MediaType;
//import jakarta.annotation.PostConstruct;
//import lombok.Data;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.autoconfigure.cassandra.CassandraProperties;
//import org.springframework.stereotype.Service;
//
//
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.TimeUnit;
//
///**
// * Whisper语音识别服务
// * 通过HTTP调用Whisper ASR服务
// *
// * @author VoiceText Weaver
// * @since 2026-03-01
// */
//@Slf4j
//@Service
//public class WhisperService {
//
//    @Value("${whisper.service.url:http://localhost:9000}")
//    private String serviceUrl;
//
//    @Value("${whisper.service.timeout:60}")
//    private int timeoutSeconds;
//
//    private OkHttpClient httpClient;
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    @PostConstruct
//    public void init() {
//        this.httpClient = new OkHttpClient.Builder()
//            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
//            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
//            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
//            .build();
//    }
//
//    /**
//     * 语音转文字
//     *
//     * @param audioFile 音频文件
//     * @return 识别结果
//     */
//    public TranscriptionResult transcribe(File audioFile) {
//        return transcribe(audioFile, TranscriptionRequest.builder().build());
//    }
//
//    /**
//     * 语音转文字（带参数）
//     *
//     * @param audioFile 音频文件
//     * @param request 识别请求参数
//     * @return 识别结果
//     */
//    public TranscriptionResult transcribe(File audioFile, TranscriptionRequest request) {
//        log.info("Transcribing audio: {} with model: {}", audioFile.getName(), request.getModel());
//
//        try {
//            // 构建multipart请求
//            MultipartBody.Builder bodyBuilder = new MultipartBody.Builder()
//                .setType(MultipartBody.FORM)
//                .addFormDataPart("file", audioFile.getName(),
//                    RequestBody.create(audioFile, MediaType.parse("audio/*")));
//
//            // 添加参数
//            if (request.getModel() != null) {
//                bodyBuilder.addFormDataPart("model", request.getModel());
//            }
//            if (request.getLanguage() != null) {
//                bodyBuilder.addFormDataPart("language", request.getLanguage());
//            }
//            if (request.getTask() != null) {
//                bodyBuilder.addFormDataPart("task", request.getTask());
//            }
//            if (request.isWordTimestamps()) {
//                bodyBuilder.addFormDataPart("word_timestamps", "true");
//            }
//            if (request.isOutputJson()) {
//                bodyBuilder.addFormDataPart("output", "json");
//            }
//
//            CassandraProperties.Request httpRequest = new Request.Builder()
//                .url(serviceUrl + "/asr")
//                .post(bodyBuilder.build())
//                .build();
//
//            try (Response response = httpClient.newCall(httpRequest).execute()) {
//                if (!response.isSuccessful()) {
//                    throw new RuntimeException("Whisper service returned error: " + response.code());
//                }
//
//                String responseBody = response.body().string();
//                return parseResponse(responseBody, request.isWordTimestamps());
//            }
//
//        } catch (IOException e) {
//            log.error("Whisper transcription failed", e);
//            throw new RuntimeException("Whisper transcription failed: " + e.getMessage(), e);
//        }
//    }
//
//    /**
//     * 异步转录（返回任务ID）
//     *
//     * @param audioFile 音频文件
//     * @param request 识别请求参数
//     * @param callbackUrl 回调URL
//     * @return 任务ID
//     */
//    public String transcribeAsync(File audioFile, TranscriptionRequest request, String callbackUrl) {
//        log.info("Async transcribing audio: {}", audioFile.getName());
//
//        try {
//            MultipartBody.Builder bodyBuilder = new MultipartBody.Builder()
//                .setType(MultipartBody.FORM)
//                .addFormDataPart("file", audioFile.getName(),
//                    RequestBody.create(audioFile, MediaType.parse("audio/*")));
//
//            if (request.getModel() != null) {
//                bodyBuilder.addFormDataPart("model", request.getModel());
//            }
//            if (request.getLanguage() != null) {
//                bodyBuilder.addFormDataPart("language", request.getLanguage());
//            }
//            if (callbackUrl != null) {
//                bodyBuilder.addFormDataPart("callback", callbackUrl);
//            }
//
//            Request httpRequest = new Request.Builder()
//                .url(serviceUrl + "/asr_async")
//                .post(bodyBuilder.build())
//                .build();
//
//            try (Response response = httpClient.newCall(httpRequest).execute()) {
//                if (!response.isSuccessful()) {
//                    throw new RuntimeException("Whisper service returned error: " + response.code());
//                }
//
//                String responseBody = response.body().string();
//                Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);
//                return (String) result.get("task_id");
//            }
//
//        } catch (IOException e) {
//            log.error("Async transcription failed", e);
//            throw new RuntimeException("Async transcription failed: " + e.getMessage(), e);
//        }
//    }
//
//    /**
//     * 查询任务状态
//     *
//     * @param taskId 任务ID
//     * @return 任务状态
//     */
//    public Map<String, Object> getTaskStatus(String taskId) {
//        try {
//            Request request = new Request.Builder()
//                .url(serviceUrl + "/task/" + taskId)
//                .get()
//                .build();
//
//            try (Response response = httpClient.newCall(request).execute()) {
//                if (!response.isSuccessful()) {
//                    throw new RuntimeException("Failed to get task status: " + response.code());
//                }
//
//                String responseBody = response.body().string();
//                return objectMapper.readValue(responseBody, Map.class);
//            }
//
//        } catch (IOException e) {
//            log.error("Failed to get task status", e);
//            throw new RuntimeException("Failed to get task status: " + e.getMessage(), e);
//        }
//    }
//
//    /**
//     * 解析响应
//     */
//    private TranscriptionResult parseResponse(String responseBody, boolean hasWordTimestamps) throws IOException {
//        Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
//
//        TranscriptionResult result = new TranscriptionResult();
//
//        if (response.containsKey("text")) {
//            result.setText((String) response.get("text"));
//        }
//
//        if (response.containsKey("language")) {
//            result.setLanguage((String) response.get("language"));
//        }
//
//        if (hasWordTimestamps && response.containsKey("segments")) {
//            List<Map<String, Object>> segments = (List<Map<String, Object>>) response.get("segments");
//            List<TranscriptionSegment> segmentList = new ArrayList<>();
//
//            for (Map<String, Object> seg : segments) {
//                TranscriptionSegment segment = new TranscriptionSegment();
//                segment.setStart(((Number) seg.get("start")).doubleValue());
//                segment.setEnd(((Number) seg.get("end")).doubleValue());
//                segment.setText((String) seg.get("text"));
//
//                if (seg.containsKey("words")) {
//                    List<Map<String, Object>> words = (List<Map<String, Object>>) seg.get("words");
//                    List<WordTimestamp> wordList = new ArrayList<>();
//                    for (Map<String, Object> word : words) {
//                        WordTimestamp wt = new WordTimestamp();
//                        wt.setWord((String) word.get("word"));
//                        wt.setStart(((Number) word.get("start")).doubleValue());
//                        wt.setEnd(((Number) word.get("end")).doubleValue());
//                        wordList.add(wt);
//                    }
//                    segment.setWords(wordList);
//                }
//
//                segmentList.add(segment);
//            }
//
//            result.setSegments(segmentList);
//        }
//
//        return result;
//    }
//
//    /**
//     * 转录请求参数
//     */
//    @Data
//    public static class TranscriptionRequest {
//        private String model = "base";      // tiny, base, small, medium, large
//        private String language;             // 语言代码，如zh, en
//        private String task = "transcribe";  // transcribe 或 translate
//        private boolean wordTimestamps = false;
//        private boolean outputJson = true;
//
//        public static TranscriptionRequestBuilder builder() {
//            return new TranscriptionRequestBuilder();
//        }
//
//        public static class TranscriptionRequestBuilder {
//            private TranscriptionRequest request = new TranscriptionRequest();
//
//            public TranscriptionRequestBuilder model(String model) {
//                request.setModel(model);
//                return this;
//            }
//
//            public TranscriptionRequestBuilder language(String language) {
//                request.setLanguage(language);
//                return this;
//            }
//
//            public TranscriptionRequestBuilder task(String task) {
//                request.setTask(task);
//                return this;
//            }
//
//            public TranscriptionRequestBuilder wordTimestamps(boolean wordTimestamps) {
//                request.setWordTimestamps(wordTimestamps);
//                return this;
//            }
//
//            public TranscriptionRequest build() {
//                return request;
//            }
//        }
//    }
//
//    /**
//     * 转录结果
//     */
//    @Data
//    public static class TranscriptionResult {
//        private String text;
//        private String language;
//        private List<TranscriptionSegment> segments;
//        private double processingTime;
//        private Map<String, Object> metadata = new HashMap<>();
//    }
//
//    /**
//     * 转录片段
//     */
//    @Data
//    public static class TranscriptionSegment {
//        private double start;
//        private double end;
//        private String text;
//        private List<WordTimestamp> words;
//    }
//
//    /**
//     * 单词时间戳
//     */
//    @Data
//    public static class WordTimestamp {
//        private String word;
//        private double start;
//        private double end;
//    }
//}
