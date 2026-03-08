package com.voicetext.common.audios.config;
//
//import lombok.Data;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.stereotype.Component;
//
//import java.util.Map;
//
///**
// * 音频处理配置属性
// */
//@Data
//@Component
//@ConfigurationProperties(prefix = "audio")
//public class AudioProperties {
//
//    /**
//     * 音频处理核心配置
//     */
//    private Processor processor = new Processor();
//
//    /**
//     * 语音识别(Whisper)配置
//     */
//    private Whisper whisper = new Whisper();
//
//    /**
//     * 语音合成(TTS)配置
//     */
//    private Tts tts = new Tts();
//
//    /**
//     * 声音克隆配置
//     */
//    private VoiceClone voiceClone = new VoiceClone();
//
//    /**
//     * 项目合并配置
//     */
//    private Project project = new Project();
//
//    @Data
//    public static class Processor {
//        /**
//         * 默认采样率
//         */
//        private int defaultSampleRate = 44100;
//
//        /**
//         * 临时文件目录
//         */
//        private String tempDir = "/tmp/audio";
//
//        /**
//         * 是否启用缓存
//         */
//        private boolean enableCache = true;
//
//        /**
//         * 波形缓存过期时间(秒)
//         */
//        private int waveformCacheExpire = 3600;
//
//        /**
//         * 支持的音频格式
//         */
//        private String[] supportedFormats = {"mp3", "wav", "flac", "aac", "ogg"};
//
//        /**
//         * 最大处理时长(秒)
//         */
//        private int maxDuration = 3600;
//    }
//
//    @Data
//    public static class Whisper {
//        /**
//         * 服务类型: http / local
//         */
//        private String type = "http";
//
//        /**
//         * HTTP服务地址
//         */
//        private String httpUrl = "http://localhost:9002";
//
//        /**
//         * 本地模型路径
//         */
//        private String modelPath = "/models/whisper";
//
//        /**
//         * 模型名称: base / small / medium / large
//         */
//        private String modelName = "base";
//
//        /**
//         * 语言(自动检测传空)
//         */
//        private String language = "";
//
//        /**
//         * 是否翻译为英文
//         */
//        private boolean translate = false;
//
//        /**
//         * 任务类型: transcribe / translate
//         */
//        private String task = "transcribe";
//
//        /**
//         * 最大并发数
//         */
//        private int maxConcurrent = 2;
//    }
//
//    @Data
//    public static class Tts {
//        /**
//         * 服务类型: coqui / edge / baidu
//         */
//        private String type = "coqui";
//
//        /**
//         * HTTP服务地址
//         */
//        private String httpUrl = "http://localhost:9003";
//
//        /**
//         * 本地模型路径
//         */
//        private String modelPath = "/models/tts";
//
//        /**
//         * 默认语音
//         */
//        private String defaultVoice = "default";
//
//        /**
//         * 默认速率
//         */
//        private float defaultSpeed = 1.0f;
//
//        /**
//         * 缓存合成结果
//         */
//        private boolean cacheEnabled = true;
//
//        /**
//         * 缓存过期时间(天)
//         */
//        private int cacheExpireDays = 7;
//    }
//
//    @Data
//    public static class VoiceClone {
//        /**
//         * 服务类型: so-vits-svc / openvoice
//         */
//        private String type = "so-vits-svc";
//
//        /**
//         * HTTP服务地址
//         */
//        private String httpUrl = "http://localhost:9004";
//
//        /**
//         * 模型存储根目录
//         */
//        private String modelRoot = "/data/voice-clone/models";
//
//        /**
//         * 训练数据目录
//         */
//        private String trainDataDir = "/data/voice-clone/train-data";
//
//        /**
//         * 最大训练时长(分钟)
//         */
//        private int maxTrainMinutes = 120;
//
//        /**
//         * 最小训练音频时长(秒)
//         */
//        private int minTrainAudioSeconds = 300;
//
//        /**
//         * 推荐训练时长(秒)
//         */
//        private int recommendTrainSeconds = 1800;
//
//        /**
//         * 音频质量要求(采样率)
//         */
//        private int requiredSampleRate = 44100;
//
//        /**
//         * 并发训练任务数
//         */
//        private int concurrentTrain = 1;
//
//        /**
//         * 训练轮次回调地址
//         */
//        private String callbackUrl = "";
//
//        /**
//         * 模型配额(每个租户)
//         */
//        private int modelQuota = 5;
//    }
//
//    @Data
//    public static class Project {
//        /**
//         * 最大项目数(每个租户)
//         */
//        private int maxProjects = 50;
//
//        /**
//         * 项目回收站保留天数
//         */
//        private int recycleDays = 30;
//
//        /**
//         * 最大合并音频数
//         */
//        private int maxMergeItems = 100;
//
//        /**
//         * 预览采样率(降低以节省带宽)
//         */
//        private int previewSampleRate = 16000;
//
//        /**
//         * 预览质量(0-10)
//         */
//        private int previewQuality = 5;
//
//        /**
//         * 是否生成波形缓存
//         */
//        private boolean generateWaveform = true;
//    }
//}
