package com.voicetext.common.python.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicetext.common.core.config.SystemConfig;
import com.voicetext.common.python.PythonExecutor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一批量处理器
 * 合并所有批量Python脚本调用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchProcessor {

    private final PythonExecutor pythonExecutor;
    private final ObjectMapper objectMapper;
    private final SystemConfig systemConfig;

    /**
     * 批量处理类型枚举
     */
    @Getter
    public enum BatchType {
        ACTIVITY_COUNT("activity_count_sleep_batch.py", "体动计数"),
        SLEEP_DEPTH("classification_lstm_es_batch.py", "睡眠深度"),
        SLEEP_SCORE("calculate_scores_batch.py", "睡眠评分"),
        SLEEP_ONSET("sleep_onset_decision_batch.py", "入眠时刻");

        private final String scriptName;
        private final String description;

        BatchType(String scriptName, String description) {
            this.scriptName = scriptName;
            this.description = description;
        }

    }

    /**
     * 批量处理方法
     */
    @Getter
    public enum BatchMode {
        DATA_FILE("--data-file", "文件模式"),
        BATCH_MODE("--batch-mode", "批量处理模式"),
        STDIN_MODE("--stdin-mode", "标准模式");

        private final String modelName;
        private final String description;

        BatchMode(String modelName, String description) {
            this.modelName = modelName;
            this.description = description;
        }

    }

    /**
     * 批量处理请求
     *
     * @param batchType 处理类型
     * @param requests  请求列表（key: deviceId, value: 请求数据）
     * @return 批量处理结果
     */
    public BatchResponse processBatch(BatchType batchType, Map<String, Object> requests, List<String> args, BatchMode model) {
        return processBatch(batchType, requests, args, model, false);
    }

    /**
     * 批量处理请求（支持重试）
     */
    public BatchResponse processBatch(BatchType batchType, Map<String, Object> requests, List<String> args, BatchMode model, boolean retryFailed) {
        String batchId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        log.info("开始批量处理，类型: {}, 设备数: {}, 批次ID: {}",
                batchType.getDescription(), requests.size(), batchId);

        // 1. 参数验证
        if (requests.isEmpty()) {
            return BatchResponse.error(batchId, "请求列表为空");
        }

        // 2. 准备批量数据
        // 3. 执行Python脚本
        PythonExecutor.PythonResult pythonResult = pythonExecutor.execute(
                batchType.getScriptName(),
                requests,
                args,
                model
        );

        // 4. 检查执行结果
        if (!pythonResult.isSuccess()) {
            log.error("批量Python脚本执行失败: {}", pythonResult.getError());

            if (retryFailed) {
                return retryFailedItems(batchId, batchType, requests, startTime);
            }

            return BatchResponse.error(batchId,
                    "脚本执行失败: " + pythonResult.getError());
        }

        // 5. 解析结果
        try {
            return parseBatchResult(batchId, pythonResult, requests.keySet(), startTime);
        } catch (Exception e) {
            log.error("解析批量结果失败", e);
            return BatchResponse.error(batchId, "解析失败: " + e.getMessage());
        }
    }

    /**
     * 解析批量结果
     */
    @SuppressWarnings("unchecked")
    private BatchResponse parseBatchResult(String batchId,
                                           PythonExecutor.PythonResult pythonResult,
                                           Set<String> deviceIds,
                                           long startTime) throws Exception {

        Map<String, Object> resultMap = objectMapper.readValue(pythonResult.getOutput(), Map.class);

        Map<String, Object> successResults = new ConcurrentHashMap<>();
        Map<String, String> errorDetails = new ConcurrentHashMap<>();
        String batchStatus = (String) resultMap.get("status");
        log.info("Python脚本执行状态: {}", batchStatus);

        if ("SUCCESS".equals(batchStatus)) {
            // 处理成功结果
            List<Map<String, Object>> results = (List<Map<String, Object>>) resultMap.get("results");

            for (Map<String, Object> deviceResult : results) {
                String deviceId = (String) deviceResult.get("device_id");
                successResults.put(deviceId, deviceResult);
            }
        } else {
            // 处理失败结果
            List<Map<String, Object>> errors = (List<Map<String, Object>>) resultMap.get("errors");
            for (Map<String, Object> deviceResult : errors) {
                String deviceId = (String) deviceResult.get("device_id");
                String error = (String) deviceResult.get("error");
                errorDetails.put(deviceId, error);
            }
        }

        return BatchResponse.builder()
                .batchId(batchId)
                .status(batchStatus)
                .totalCount(deviceIds.size())
                .successCount(successResults.size())
                .errorCount(errorDetails.size())
                .results(successResults)
                .errors(errorDetails)
                .executionTime(System.currentTimeMillis() - startTime)
                .executionId(pythonResult.getExecutionId())
                .build();
    }

    /**
     * 逐条重试失败项
     */
    private BatchResponse retryFailedItems(String batchId,
                                           BatchType batchType,
                                           Map<String, Object> requests,
                                           long startTime) {
        log.info("开始逐条重试，批次ID: {}", batchId);

        Map<String, Object> successResults = new ConcurrentHashMap<>();
        Map<String, String> errorDetails = new ConcurrentHashMap<>();

        for (Map.Entry<String, Object> entry : requests.entrySet()) {
            String deviceId = entry.getKey();
            Object request = entry.getValue();

            try {
                // 单条处理
                Map<String, Object> singleRequest = new HashMap<>();
                singleRequest.put(deviceId, request);

                PythonExecutor.PythonResult result = pythonExecutor.execute(
                        batchType.getScriptName(),
                        singleRequest,
                        List.of("--stdin-mode")
                );

                if (result.isSuccess()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resultMap = objectMapper.readValue(result.getOutput(), Map.class);
                    successResults.put(deviceId, resultMap);
                    log.info("设备 {} 重试成功", deviceId);
                } else {
                    errorDetails.put(deviceId, result.getError());
                    log.warn("设备 {} 重试失败: {}", deviceId, result.getError());
                }
            } catch (Exception e) {
                errorDetails.put(deviceId, "重试异常: " + e.getMessage());
                log.error("设备 {} 重试异常", deviceId, e);
            }
        }

        return BatchResponse.builder()
                .batchId(batchId)
                .status(successResults.isEmpty() ? "ERROR" : "PARTIAL_SUCCESS")
                .totalCount(requests.size())
                .successCount(successResults.size())
                .errorCount(errorDetails.size())
                .results(successResults)
                .errors(errorDetails)
                .executionTime(System.currentTimeMillis() - startTime)
                .build();
    }

    /**
     * 批量处理结果
     */
    @Data
    @Builder
    public static class BatchResponse {
        private String batchId;
        private String status;          // SUCCESS, PARTIAL_SUCCESS, ERROR
        private int totalCount;
        private int successCount;
        private int errorCount;
        private Map<String, Object> results;
        private Map<String, String> errors;
        private long executionTime;
        private String executionId;
        private String errorMessage;

        public static BatchResponse error(String batchId, String errorMessage) {
            return BatchResponse.builder()
                    .batchId(batchId)
                    .status("ERROR")
                    .errorMessage(errorMessage)
                    .totalCount(0)
                    .successCount(0)
                    .errorCount(0)
                    .results(new HashMap<>())
                    .errors(new HashMap<>())
                    .build();
        }

        public boolean isSuccess() {
            return "SUCCESS".equals(status);
        }

        public boolean isPartialSuccess() {
            return "PARTIAL_SUCCESS".equals(status);
        }
    }
}
