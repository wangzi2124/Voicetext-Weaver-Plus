package com.voicetext.common.audios.config;
//
//import lombok.extern.slf4j.Slf4j;
//import com.voicetext.common.audio.core.AudioProcessor;
//import com.voicetext.common.audio.core.FFmpegAudioProcessor;
//import com.voicetext.common.audio.core.TarsosAudioProcessor;
//import com.voicetext.common.audio.dsp.AudioAnalyzer;
//import com.voicetext.common.audio.dsp.VoiceActivityDetector;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
//import org.springframework.boot.context.properties.EnableConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
//
//import java.util.concurrent.Executor;
//import java.util.concurrent.ThreadPoolExecutor;
//
///**
// * 音频模块自动配置
// */
//@Slf4j
//@Configuration
//@EnableConfigurationProperties(AudioProperties.class)
//public class AudioAutoConfiguration {
//
//    @Bean
//    @ConditionalOnMissingBean
//    public AudioProcessor audioProcessor(AudioProperties properties) {
//        log.info("初始化音频处理器: {}", properties.getProcessor());
//        return new TarsosAudioProcessor(properties);
//    }
//
//    @Bean
//    @ConditionalOnMissingBean
//    public FFmpegAudioProcessor ffmpegAudioProcessor(AudioProperties properties) {
//        log.info("初始化FFmpeg处理器");
//        return new FFmpegAudioProcessor(properties);
//    }
//
//    @Bean
//    @ConditionalOnMissingBean
//    public AudioAnalyzer audioAnalyzer(AudioProcessor audioProcessor) {
//        return new AudioAnalyzer(audioProcessor);
//    }
//
//    @Bean
//    @ConditionalOnMissingBean
//    public VoiceActivityDetector voiceActivityDetector(AudioProcessor audioProcessor) {
//        return new VoiceActivityDetector(audioProcessor);
//    }
//
//    @Bean
//    @ConditionalOnProperty(prefix = "audio.whisper", name = "type", havingValue = "http")
//    public WhisperService whisperService(AudioProperties properties) {
//        log.info("初始化Whisper HTTP服务: {}", properties.getWhisper().getHttpUrl());
//        return new WhisperService(properties);
//    }
//
//    @Bean
//    @ConditionalOnProperty(prefix = "audio.tts", name = "type", havingValue = "coqui")
//    public TtsService ttsService(AudioProperties properties) {
//        log.info("初始化TTS服务: {}", properties.getTts().getHttpUrl());
//        return new TtsService(properties);
//    }
//
//    @Bean
//    @ConditionalOnProperty(prefix = "audio.voice-clone", name = "type", havingValue = "so-vits-svc")
//    public VoiceCloneService voiceCloneService(AudioProperties properties) {
//        log.info("初始化声音克隆服务: {}", properties.getVoiceClone().getHttpUrl());
//        return new VoiceCloneService(properties);
//    }
//
//    @Bean
//    public VoiceModelManager voiceModelManager(AudioProperties properties, RedisTemplate<String, Object> redisTemplate) {
//        return new VoiceModelManager(properties, redisTemplate);
//    }
//
//    @Bean
//    public VoiceTrainService voiceTrainService(
//            AudioProperties properties,
//            VoiceCloneService cloneService,
//            VoiceModelManager modelManager) {
//        return new VoiceTrainService(properties, cloneService, modelManager);
//    }
//
//    @Bean
//    public AudioProjectService audioProjectService(
//            AudioProperties properties,
//            AudioProcessor audioProcessor,
//            RedisTemplate<String, Object> redisTemplate) {
//        return new AudioProjectService(properties, audioProcessor, redisTemplate);
//    }
//
//    @Bean
//    public AudioMergeService audioMergeService(
//            AudioProperties properties,
//            AudioProcessor audioProcessor,
//            AudioProjectService projectService) {
//        return new AudioMergeService(properties, audioProcessor, projectService);
//    }
//
//    /**
//     * 音频处理线程池
//     */
//    @Bean(name = "audioTaskExecutor")
//    public Executor audioTaskExecutor(AudioProperties properties) {
//        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        executor.setCorePoolSize(5);
//        executor.setMaxPoolSize(10);
//        executor.setQueueCapacity(100);
//        executor.setThreadNamePrefix("audio-task-");
//        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
//        executor.initialize();
//        return executor;
//    }
//}
