package com.voicetext.common.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

/**
 * 统一系统配置类
 * 合并：application.properties中的各类配置
 */
@Configuration
@ConfigurationProperties(prefix = "photosensor")
@Data
public class SystemConfig {

    // Python配置
    private PythonConfig python = new PythonConfig();

    // 算法配置
    private AlgorithmConfig algorithm = new AlgorithmConfig();

    // 调度配置
    private ScheduleConfig schedule = new ScheduleConfig();

    // 数据库配置
    private DatabaseConfig database = new DatabaseConfig();

    @Data
    public static class PythonConfig {
        private String executable = "python3";
        private String scriptDir = "classpath:python-scripts";
        private int timeout = 300;
        private int maxConcurrent = 5;
        private String minVersion = "3.9";

        public String getResolvedScriptDir() {
            // 解析classpath路径
            if (scriptDir.startsWith("classpath:")) {
                try {
                    return new ClassPathResource(scriptDir.substring(10))
                            .getFile().getAbsolutePath();
                } catch (IOException e) {
                    return scriptDir;
                }
            }
            return scriptDir;
        }

        public String getSmartExecutable() {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) return "python";
            if (os.contains("mac")) return "python3";
            return executable;
        }
    }

    @Data
    public static class AlgorithmConfig {
        // 心率计算
        private int heartRateWindowSeconds = 30;
        private int heartRateMinChanges = 5;

        // 心率默认
        private double defaultHeartRate = 60.00;

        // 人検知
        private int activityThreshold = 1000;
        private int heartbeatThreshold = 5;

        // 风险预测 过去 20天
        private int riskReferenceDays = 20;
        private double heartRateIncreaseThreshold = 1.20;
        private double stayDurationIncreaseThreshold = 1.30;

        // 睡眠建议
        private int tipsAnalysisDays = 14;

        // 入眠时刻检测 窗口一分钟，心率下降阈值比例为5%，确认入睡的判断持续时间为3分钟
        private int sleepOnsetPrepareMinutes = 1;
        private double sleepOnsetDecreaseRatio = 0.05;
        private int sleepOnsetDecisionDuration = 3;

        // 30分钟 6 次 5分钟一次，有2次成功的
        private int wakeUpCheckCycles = 6;
        private int wakeUpSuccessThreshold = 2;
        private int wakeUpCooldownMinutes=30;
    }

    @Data
    public static class ScheduleConfig {
        private int presenceDetectionMinutes = 1;
        private int heartRateAnalysisSeconds = 30;
        private int sleepScoreSeconds = 30;
        private int sleepScoreDay = 1;
        private int sleepOnsetMinutes = 1;
        private int wakeUpDetectionMinutes = 5;
        private int riskAssessmentDays = 1;
    }

    @Data
    public static class DatabaseConfig {
        private int batchInsertSize = 1000;
        private int queryTimeoutSeconds = 30;
    }
}
