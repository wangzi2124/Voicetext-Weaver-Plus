package org.dromara.audio.whisper;//package org.dromara.common.audio.whisper;
//
//import jakarta.annotation.PostConstruct;
//import lombok.Data;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//import java.io.File;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.atomic.AtomicInteger;
//
///**
// * Whisper模型管理器
// * 管理模型加载、缓存和性能监控
// *
// * @author VoiceText Weaver
// * @since 2026-03-01
// */
//@Slf4j
//@Component
//public class WhisperModelManager {
//
//    @Value("${whisper.models.path:./models/whisper}")
//    private String modelsPath;
//
//    @Value("${whisper.models.default:base}")
//    private String defaultModel;
//
//    private final Map<String, ModelInfo> availableModels = new ConcurrentHashMap<>();
//    private final Map<String, ModelStats> modelStats = new ConcurrentHashMap<>();
//
//    @PostConstruct
//    public void init() {
//        // 扫描可用模型
//        scanAvailableModels();
//
//        // 初始化统计信息
//        for (String modelName : availableModels.keySet()) {
//            modelStats.put(modelName, new ModelStats());
//        }
//
//        log.info("WhisperModelManager initialized with {} models", availableModels.size());
//    }
//
//    /**
//     * 扫描可用模型
//     */
//    private void scanAvailableModels() {
//        File modelsDir = new File(modelsPath);
//        if (!modelsDir.exists()) {
//            modelsDir.mkdirs();
//            log.info("Created models directory: {}", modelsPath);
//        }
//
//        // 标准Whisper模型
//        String[] standardModels = {"tiny", "base", "small", "medium", "large"};
//        for (String model : standardModels) {
//            ModelInfo info = new ModelInfo();
//            info.setName(model);
//            info.setPath(modelsPath + File.separator + model);
//            info.setSize(getModelSize(model));
//            info.setLoaded(false);
//            info.setRamUsage(getModelRamUsage(model));
//            availableModels.put(model, info);
//        }
//
//        // 扫描自定义模型
//        File[] files = modelsDir.listFiles((dir, name) ->
//            name.endsWith(".pt") || name.endsWith(".bin") || name.endsWith(".ggml"));
//
//        if (files != null) {
//            for (File file : files) {
//                String name = file.getName().substring(0, file.getName().lastIndexOf('.'));
//                if (!availableModels.containsKey(name)) {
//                    ModelInfo info = new ModelInfo();
//                    info.setName(name);
//                    info.setPath(file.getAbsolutePath());
//                    info.setSize(file.length());
//                    info.setLoaded(false);
//                    info.setRamUsage(estimateRamUsage(file.length()));
//                    availableModels.put(name, info);
//                }
//            }
//        }
//    }
//
//    /**
//     * 获取模型信息
//     */
//    public ModelInfo getModelInfo(String modelName) {
//        return availableModels.get(modelName);
//    }
//
//    /**
//     * 获取所有可用模型
//     */
//    public Map<String, ModelInfo> getAvailableModels() {
//        return new HashMap<>(availableModels);
//    }
//
//    /**
//     * 检查模型是否可用
//     */
//    public boolean isModelAvailable(String modelName) {
//        return availableModels.containsKey(modelName);
//    }
//
//    /**
//     * 获取推荐模型（根据音频时长）
//     */
//    public String getRecommendedModel(double audioDuration) {
//        if (audioDuration < 60) {
//            return "tiny";  // 短音频用tiny
//        } else if (audioDuration < 300) {
//            return "base";  // 5分钟内用base
//        } else if (audioDuration < 1800) {
//            return "small"; // 30分钟内用small
//        } else {
//            return "medium"; // 长音频用medium
//        }
//    }
//
//    /**
//     * 记录模型使用
//     */
//    public void recordModelUsage(String modelName, double processingTime, boolean success) {
//        ModelStats stats = modelStats.computeIfAbsent(modelName, k -> new ModelStats());
//        stats.getTotalRequests().incrementAndGet();
//        stats.getTotalProcessingTime().addAndGet((int) (processingTime * 1000));
//        if (success) {
//            stats.getSuccessfulRequests().incrementAndGet();
//        } else {
//            stats.getFailedRequests().incrementAndGet();
//        }
//    }
//
//    /**
//     * 获取模型统计
//     */
//    public ModelStats getModelStats(String modelName) {
//        return modelStats.get(modelName);
//    }
//
//    /**
//     * 获取标准模型大小（MB）
//     */
//    private long getModelSize(String modelName) {
//        switch (modelName) {
//            case "tiny": return 75;
//            case "base": return 142;
//            case "small": return 466;
//            case "medium": return 1533;
//            case "large": return 2950;
//            default: return 0;
//        }
//    }
//
//    /**
//     * 获取模型RAM使用（MB）
//     */
//    private long getModelRamUsage(String modelName) {
//        switch (modelName) {
//            case "tiny": return 400;
//            case "base": return 600;
//            case "small": return 1200;
//            case "medium": return 3000;
//            case "large": return 5000;
//            default: return 0;
//        }
//    }
//
//    /**
//     * 估算自定义模型RAM使用
//     */
//    private long estimateRamUsage(long fileSize) {
//        // 模型文件通常会被加载到内存，大约需要2-3倍文件大小
//        return fileSize * 2 / (1024 * 1024);
//    }
//
//    /**
//     * 模型信息
//     */
//    @Data
//    public static class ModelInfo {
//        private String name;
//        private String path;
//        private long size;          // 文件大小（字节）
//        private long ramUsage;       // 内存使用（MB）
//        private boolean loaded;      // 是否已加载
//        private int currentLoads;    // 当前加载数
//        private int maxConcurrent;   // 最大并发数
//    }
//
//    /**
//     * 模型统计
//     */
//    @Data
//    public static class ModelStats {
//        private AtomicInteger totalRequests = new AtomicInteger(0);
//        private AtomicInteger successfulRequests = new AtomicInteger(0);
//        private AtomicInteger failedRequests = new AtomicInteger(0);
//        private AtomicInteger totalProcessingTime = new AtomicInteger(0); // 毫秒
//
//        public double getAverageProcessingTime() {
//            int total = totalRequests.get();
//            return total > 0 ? (double) totalProcessingTime.get() / total / 1000 : 0;
//        }
//
//        public double getSuccessRate() {
//            int total = totalRequests.get();
//            return total > 0 ? (double) successfulRequests.get() / total * 100 : 0;
//        }
//    }
//}
