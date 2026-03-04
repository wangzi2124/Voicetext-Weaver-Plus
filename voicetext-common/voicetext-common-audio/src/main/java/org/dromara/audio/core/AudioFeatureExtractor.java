//package org.dromara.common.audio.core;
//
//import lombok.extern.slf4j.Slf4j;
//import org.dromara.common.audio.dsp.AudioFeatures;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import java.io.File;
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * 音频特征提取器
// * 整合多个处理器，提供统一的特征提取接口
// *
// * @author VoiceText Weaver
// * @since 2026-03-01
// */
//@Slf4j
//@Component
//public class AudioFeatureExtractor {
//
//    @Autowired
//    private TarsosAudioProcessor tarsosProcessor;
//
//    @Autowired
//    private FFmpegAudioProcessor ffmpegProcessor;
//
//    /**
//     * 提取所有可用特征
//     *
//     * @param audioFile 音频文件
//     * @return 特征对象
//     */
//    public AudioFeatures extractAll(File audioFile) {
//        log.info("Extracting all features from: {}", audioFile.getName());
//
//        AudioFeatures features = new AudioFeatures();
//
//        // 获取基本信息（FFmpeg）
//        Map<String, Object> basicInfo = ffmpegProcessor.getAudioInfo(audioFile);
//        features.setFileName(audioFile.getName());
//        features.setFileSize(audioFile.length());
//
//        if (basicInfo.containsKey("duration")) {
//            features.setDuration((Double) basicInfo.get("duration"));
//        }
//        if (basicInfo.containsKey("sampleRate")) {
//            features.setSampleRate((Float) basicInfo.get("sampleRate"));
//        }
//        if (basicInfo.containsKey("channels")) {
//            features.setChannels((Integer) basicInfo.get("channels"));
//        }
//        if (basicInfo.containsKey("bitrate")) {
//            features.setBitrate((Integer) basicInfo.get("bitrate"));
//        }
//
//        // 提取详细特征（TarsosDSP）
//        try {
//            AudioFeatures tarsosFeatures = tarsosProcessor.extractFeatures(audioFile);
//            features.setRms(tarsosFeatures.getRms());
//            features.setPeak(tarsosFeatures.getPeak());
//            features.setZeroCrossingRate(tarsosFeatures.getZeroCrossingRate());
//            features.setSpectralCentroid(tarsosFeatures.getSpectralCentroid());
//            features.setMeanPitch(tarsosFeatures.getMeanPitch());
//            features.setMaxPitch(tarsosFeatures.getMaxPitch());
//            features.setMinPitch(tarsosFeatures.getMinPitch());
//        } catch (Exception e) {
//            log.warn("Failed to extract TarsosDSP features: {}", e.getMessage());
//        }
//
//        return features;
//    }
//
//    /**
//     * 提取特定特征
//     *
//     * @param audioFile 音频文件
//     * @param featureNames 特征名称列表
//     * @return 特征Map
//     */
//    public Map<String, Object> extractFeatures(File audioFile, String... featureNames) {
//        Map<String, Object> result = new HashMap<>();
//
//        for (String featureName : featureNames) {
//            switch (featureName.toLowerCase()) {
//                case "duration":
//                    result.put("duration", getDuration(audioFile));
//                    break;
//                case "sample_rate":
//                    result.put("sampleRate", getSampleRate(audioFile));
//                    break;
//                case "channels":
//                    result.put("channels", getChannels(audioFile));
//                    break;
//                case "rms":
//                    result.put("rms", getRMS(audioFile));
//                    break;
//                case "peak":
//                    result.put("peak", getPeak(audioFile));
//                    break;
//                case "zero_crossing_rate":
//                    result.put("zeroCrossingRate", getZeroCrossingRate(audioFile));
//                    break;
//                case "pitch":
//                    result.put("pitch", getMeanPitch(audioFile));
//                    break;
//                case "spectral_centroid":
//                    result.put("spectralCentroid", getSpectralCentroid(audioFile));
//                    break;
//                default:
//                    log.warn("Unknown feature: {}", featureName);
//            }
//        }
//
//        return result;
//    }
//
//    /**
//     * 获取音频时长
//     */
//    public double getDuration(File audioFile) {
//        Map<String, Object> info = ffmpegProcessor.getAudioInfo(audioFile);
//        return info.containsKey("duration") ? (Double) info.get("duration") : 0;
//    }
//
//    /**
//     * 获取采样率
//     */
//    public float getSampleRate(File audioFile) {
//        Map<String, Object> info = ffmpegProcessor.getAudioInfo(audioFile);
//        return info.containsKey("sampleRate") ? (Float) info.get("sampleRate") : 0;
//    }
//
//    /**
//     * 获取声道数
//     */
//    public int getChannels(File audioFile) {
//        Map<String, Object> info = ffmpegProcessor.getAudioInfo(audioFile);
//        return info.containsKey("channels") ? (Integer) info.get("channels") : 0;
//    }
//
//    /**
//     * 获取RMS音量
//     */
//    public double getRMS(File audioFile) {
//        try {
//            return tarsosProcessor.extractFeatures(audioFile).getRms();
//        } catch (Exception e) {
//            log.warn("Failed to get RMS: {}", e.getMessage());
//            return 0;
//        }
//    }
//
//    /**
//     * 获取峰值
//     */
//    public double getPeak(File audioFile) {
//        try {
//            return tarsosProcessor.extractFeatures(audioFile).getPeak();
//        } catch (Exception e) {
//            log.warn("Failed to get peak: {}", e.getMessage());
//            return 0;
//        }
//    }
//
//    /**
//     * 获取过零率
//     */
//    public double getZeroCrossingRate(File audioFile) {
//        try {
//            return tarsosProcessor.extractFeatures(audioFile).getZeroCrossingRate();
//        } catch (Exception e) {
//            log.warn("Failed to get zero crossing rate: {}", e.getMessage());
//            return 0;
//        }
//    }
//
//    /**
//     * 获取平均基频
//     */
//    public double getMeanPitch(File audioFile) {
//        try {
//            return tarsosProcessor.extractFeatures(audioFile).getMeanPitch();
//        } catch (Exception e) {
//            log.warn("Failed to get mean pitch: {}", e.getMessage());
//            return 0;
//        }
//    }
//
//    /**
//     * 获取频谱质心
//     */
//    public double getSpectralCentroid(File audioFile) {
//        try {
//            return tarsosProcessor.extractFeatures(audioFile).getSpectralCentroid();
//        } catch (Exception e) {
//            log.warn("Failed to get spectral centroid: {}", e.getMessage());
//            return 0;
//        }
//    }
//
//    /**
//     * 提取声学指纹
//     *
//     * @param audioFile 音频文件
//     * @return 指纹字符串
//     */
//    public String extractFingerprint(File audioFile) {
//        // 简化实现，实际应该使用Chromaprint等库
//        log.info("Extracting fingerprint from: {}", audioFile.getName());
//
//        try {
//            AudioFeatures features = extractAll(audioFile);
//
//            // 基于特征生成简单指纹
//            StringBuilder fingerprint = new StringBuilder();
//            fingerprint.append(String.format("%.2f", features.getDuration()));
//            fingerprint.append(String.format("%.0f", features.getSampleRate()));
//            fingerprint.append(features.getChannels());
//            fingerprint.append(String.format("%.3f", features.getRms()));
//            fingerprint.append(String.format("%.1f", features.getMeanPitch()));
//
//            // 使用MD5生成固定长度指纹
//            return md5(fingerprint.toString());
//
//        } catch (Exception e) {
//            log.error("Fingerprint extraction failed", e);
//            return null;
//        }
//    }
//
//    /**
//     * MD5哈希
//     */
//    private String md5(String input) {
//        try {
//            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
//            byte[] array = md.digest(input.getBytes());
//            StringBuilder sb = new StringBuilder();
//            for (byte b : array) {
//                sb.append(String.format("%02x", b));
//            }
//            return sb.toString();
//        } catch (java.security.NoSuchAlgorithmException e) {
//            return String.valueOf(input.hashCode());
//        }
//    }
//}
