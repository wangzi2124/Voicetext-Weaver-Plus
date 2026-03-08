package com.voicetext.common.audios.voiceclone;
//
//import lombok.extern.slf4j.Slf4j;
//import com.voicetext.common.audio.config.AudioProperties;
//import com.voicetext.common.audio.voiceclone.model.*;
//import org.springframework.stereotype.Service;
//
//import java.io.File;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
// * 声音训练服务
// */
//@Slf4j
//@Service
//public class VoiceTrainService {
//
//    private final AudioProperties properties;
//    private final VoiceCloneService cloneService;
//    private final VoiceModelManager modelManager;
//    private final Map<String, TrainProgress> trainProgressMap = new ConcurrentHashMap<>();
//
//    public VoiceTrainService(
//            AudioProperties properties,
//            VoiceCloneService cloneService,
//            VoiceModelManager modelManager) {
//        this.properties = properties;
//        this.cloneService = cloneService;
//        this.modelManager = modelManager;
//    }
//
//    /**
//     * 创建训练任务
//     *
//     * @param request 训练请求
//     * @return 训练任务信息
//     */
//    public TrainTask createTrainTask(TrainRequest request) {
//        // 校验音频质量
//        validateAudioQuality(request.getAudioFiles());
//
//        // 生成任务ID
//        String taskId = generateTaskId();
//
//        // 创建训练任务
//        TrainTask task = TrainTask.builder()
//            .taskId(taskId)
//            .modelName(request.getModelName())
//            .tenantId(request.getTenantId())
//            .creatorId(request.getCreatorId())
//            .audioFiles(request.getAudioFiles())
//            .modelType(request.getModelType())
//            .trainConfig(request.getTrainConfig())
//            .status(TrainStatus.PENDING)
//            .createTime(new Date())
//            .build();
//
//        // 初始化进度
//        TrainProgress progress = TrainProgress.builder()
//            .taskId(taskId)
//            .status(TrainStatus.PENDING)
//            .progress(0)
//            .build();
//        trainProgressMap.put(taskId, progress);
//
//        // 提交到训练队列
//        submitTrainTask(task);
//
//        return task;
//    }
//
//    /**
//     * 获取训练进度
//     */
//    public TrainProgress getTrainProgress(String taskId) {
//        return trainProgressMap.get(taskId);
//    }
//
//    /**
//     * 取消训练任务
//     */
//    public boolean cancelTrainTask(String taskId) {
//        TrainProgress progress = trainProgressMap.get(taskId);
//        if (progress != null &&
//            (progress.getStatus() == TrainStatus.PENDING ||
//             progress.getStatus() == TrainStatus.TRAINING)) {
//            progress.setStatus(TrainStatus.CANCELLED);
//            trainProgressMap.put(taskId, progress);
//            return true;
//        }
//        return false;
//    }
//
//    /**
//     * 校验音频质量
//     */
//    private void validateAudioQuality(List<File> audioFiles) {
//        if (audioFiles == null || audioFiles.isEmpty()) {
//            throw new IllegalArgumentException("训练音频不能为空");
//        }
//
//        // 计算总时长
//        double totalDuration = 0;
//        for (File audioFile : audioFiles) {
//            // TODO: 获取音频时长
//            // double duration = audioProcessor.getDuration(audioFile);
//            // totalDuration += duration;
//        }
//
//        int minSeconds = properties.getVoiceClone().getMinTrainAudioSeconds();
//        if (totalDuration < minSeconds) {
//            throw new IllegalArgumentException(
//                String.format("训练音频总时长不足，需要至少%d秒", minSeconds));
//        }
//
//        // TODO: 检查采样率、噪音等
//    }
//
//    /**
//     * 提交训练任务
//     */
//    private void submitTrainTask(TrainTask task) {
//        // 更新状态为训练中
//        updateProgress(task.getTaskId(), TrainStatus.TRAINING, 0);
//
//        // 使用虚拟线程异步执行
//        Thread.startVirtualThread(() -> {
//            try {
//                // 预处理音频
//                File processedAudio = preprocessAudio(task.getAudioFiles());
//
//                // 调用训练服务
//                String modelId = cloneService.trainModel(
//                    task.getTaskId(),
//                    processedAudio,
//                    task.getModelName(),
//                    task.getTrainConfig(),
//                    new TrainProgressListener() {
//                        @Override
//                        public void onProgress(int progress, float loss, int epoch) {
//                            updateProgress(task.getTaskId(), progress, loss, epoch);
//                        }
//
//                        @Override
//                        public void onSampleGenerated(File sampleAudio) {
//                            // 保存示例音频
//                            saveSampleAudio(task.getTaskId(), sampleAudio);
//                        }
//                    }
//                );
//
//                // 训练完成
//                TrainProgress progress = trainProgressMap.get(task.getTaskId());
//                progress.setStatus(TrainStatus.SUCCESS);
//                progress.setProgress(100);
//                progress.setModelId(modelId);
//                progress.setEndTime(new Date());
//
//                // 保存模型信息
//                modelManager.saveModel(task, modelId);
//
//            } catch (Exception e) {
//                log.error("训练任务失败: {}", task.getTaskId(), e);
//                TrainProgress progress = trainProgressMap.get(task.getTaskId());
//                progress.setStatus(TrainStatus.FAILED);
//                progress.setError(e.getMessage());
//                progress.setEndTime(new Date());
//            }
//        });
//    }
//
//    /**
//     * 更新进度
//     */
//    private void updateProgress(String taskId, TrainStatus status, int progress) {
//        TrainProgress p = trainProgressMap.get(taskId);
//        if (p != null) {
//            p.setStatus(status);
//            p.setProgress(progress);
//            p.setUpdateTime(new Date());
//        }
//    }
//
//    private void updateProgress(String taskId, int progress, float loss, int epoch) {
//        TrainProgress p = trainProgressMap.get(taskId);
//        if (p != null) {
//            p.setProgress(progress);
//            p.setCurrentLoss(loss);
//            p.setCurrentEpoch(epoch);
//            p.setUpdateTime(new Date());
//        }
//    }
//
//    /**
//     * 预处理音频
//     */
//    private File preprocessAudio(List<File> audioFiles) {
//        // TODO: 合并音频、格式转换、降噪等
//        return audioFiles.get(0);
//    }
//
//    /**
//     * 保存示例音频
//     */
//    private void saveSampleAudio(String taskId, File sampleAudio) {
//        TrainProgress progress = trainProgressMap.get(taskId);
//        if (progress != null) {
//            List<String> samples = progress.getSampleAudios();
//            if (samples == null) {
//                samples = new ArrayList<>();
//            }
//            samples.add(sampleAudio.getAbsolutePath());
//            progress.setSampleAudios(samples);
//        }
//    }
//
//    private String generateTaskId() {
//        return "train_" + System.currentTimeMillis() + "_" +
//               (int)(Math.random() * 10000);
//    }
//}
