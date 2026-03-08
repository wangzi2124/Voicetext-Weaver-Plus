package com.voicetext.common.audios.voiceclone;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.extern.slf4j.Slf4j;
//import com.voicetext.common.audio.config.AudioProperties;
//import com.voicetext.common.audio.voiceclone.model.CloneRequest;
//import com.voicetext.common.audio.voiceclone.model.CloneResult;
//import com.voicetext.common.audio.voiceclone.model.TrainConfig;
//import com.voicetext.common.audio.voiceclone.model.TrainProgressListener;
//import okhttp3.*;
//import org.springframework.stereotype.Service;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.TimeUnit;
//
///**
// * 声音克隆服务
// * 调用外部声音克隆API
// */
//@Slf4j
//@Service
//public class VoiceCloneService {
//
//    private final AudioProperties properties;
//    private final OkHttpClient httpClient;
//    private final ObjectMapper objectMapper;
//
//    public VoiceCloneService(AudioProperties properties) {
//        this.properties = properties;
//        this.objectMapper = new ObjectMapper();
//
//        this.httpClient = new OkHttpClient.Builder()
//            .connectTimeout(30, TimeUnit.SECONDS)
//            .writeTimeout(300, TimeUnit.SECONDS)
//            .readTimeout(300, TimeUnit.SECONDS)
//            .build();
//    }
//
//    /**
//     * 训练模型
//     *
//     * @param taskId 任务ID
//     * @param audioFile 训练音频
//     * @param modelName 模型名称
//     * @param config 训练配置
//     * @param listener 进度监听器
//     * @return 模型ID
//     */
//    public String trainModel(
//            String taskId,
//            File audioFile,
//            String modelName,
//            TrainConfig config,
//            TrainProgressListener listener) {
//
//        try {
//            String url = properties.getVoiceClone().getHttpUrl() + "/api/train";
//
//            // 构建multipart请求
//            MultipartBody.Builder bodyBuilder = new MultipartBody.Builder()
//                .setType(MultipartBody.FORM)
//                .addFormDataPart("task_id", taskId)
//                .addFormDataPart("model_name", modelName)
//                .addFormDataPart("file", audioFile.getName(),
//                    RequestBody.create(audioFile, MediaType.parse("audio/wav")));
//
//            // 添加配置参数
//            if (config != null) {
//                if (config.getEpochs() > 0) {
//                    bodyBuilder.addFormDataPart("epochs", String.valueOf(config.getEpochs()));
//                }
//                if (config.getBatchSize() > 0) {
//                    bodyBuilder.addFormDataPart("batch_size", String.valueOf(config.getBatchSize()));
//                }
//                if (config.getLearningRate() > 0) {
//                    bodyBuilder.addFormDataPart("learning_rate", String.valueOf(config.getLearningRate()));
//                }
//                if (config.getCallbackUrl() != null) {
//                    bodyBuilder.addFormDataPart("callback_url", config.getCallbackUrl());
//                }
//            }
//
//            Request request = new Request.Builder()
//                .url(url)
//                .post(bodyBuilder.build())
//                .addHeader("Accept", "application/json")
//                .build();
//
//            try (Response response = httpClient.newCall(request).execute()) {
//                if (!response.isSuccessful()) {
//                    throw new RuntimeException("训练服务调用失败: " + response.code());
//                }
//
//                String responseBody = response.body().string();
//                Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);
//
//                return (String) result.get("model_id");
//            }
//
//        } catch (Exception e) {
//            log.error("训练模型失败", e);
//            throw new RuntimeException("训练模型失败", e);
//        }
//    }
//
//    /**
//     * 声音克隆合成
//     *
//     * @param request 克隆请求
//     * @return 克隆结果
//     */
//    public CloneResult clone(CloneRequest request) {
//        try {
//            String url = properties.getVoiceClone().getHttpUrl() + "/api/clone";
//
//            Map<String, Object> requestBody = new HashMap<>();
//            requestBody.put("model_id", request.getModelId());
//            requestBody.put("text", request.getText());
//            requestBody.put("speed", request.getSpeed());
//            requestBody.put("pitch", request.getPitch());
//            requestBody.put("format", request.getFormat());
//
//            String jsonBody = objectMapper.writeValueAsString(requestBody);
//
//            Request httpRequest = new Request.Builder()
//                .url(url)
//                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
//                .addHeader("Accept", "audio/" + request.getFormat())
//                .build();
//
//            try (Response response = httpClient.newCall(httpRequest).execute()) {
//                if (!response.isSuccessful()) {
//                    throw new RuntimeException("克隆服务调用失败: " + response.code());
//                }
//
//                byte[] audioData = response.body().bytes();
//
//                // 获取音频时长(从响应头)
//                String durationStr = response.header("X-Audio-Duration");
//                double duration = durationStr != null ? Double.parseDouble(durationStr) : 0;
//
//                return CloneResult.builder()
//                    .audioData(audioData)
//                    .format(request.getFormat())
//                    .duration(duration)
//                    .text(request.getText())
//                    .modelId(request.getModelId())
//                    .build();
//            }
//
//        } catch (Exception e) {
//            log.error("声音克隆失败", e);
//            throw new RuntimeException("声音克隆失败", e);
//        }
//    }
//
//    /**
//     * 获取训练状态
//     */
//    public Map<String, Object> getTrainStatus(String taskId) {
//        try {
//            String url = properties.getVoiceClone().getHttpUrl() + "/api/train/" + taskId + "/status";
//
//            Request request = new Request.Builder()
//                .url(url)
//                .get()
//                .build();
//
//            try (Response response = httpClient.newCall(request).execute()) {
//                if (!response.isSuccessful()) {
//                    throw new RuntimeException("获取训练状态失败: " + response.code());
//                }
//
//                String responseBody = response.body().string();
//                return objectMapper.readValue(responseBody, Map.class);
//            }
//
//        } catch (Exception e) {
//            log.error("获取训练状态失败", e);
//            return Map.of("status", "unknown", "error", e.getMessage());
//        }
//    }
//
//    /**
//     * 删除模型
//     */
//    public boolean deleteModel(String modelId) {
//        try {
//            String url = properties.getVoiceClone().getHttpUrl() + "/api/models/" + modelId;
//
//            Request request = new Request.Builder()
//                .url(url)
//                .delete()
//                .build();
//
//            try (Response response = httpClient.newCall(request).execute()) {
//                return response.isSuccessful();
//            }
//
//        } catch (Exception e) {
//            log.error("删除模型失败", e);
//            return false;
//        }
//    }
//}
