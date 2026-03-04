package org.dromara.audio.dsp;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 音频特征对象
 * 存储音频分析结果
 *
 * @author VoiceText Weaver
 * @since 2026-03-01
 */
@Data
public class AudioFeatures {

    // 基本信息
    private String fileName;
    private Long fileSize;
    private Double duration;          // 时长（秒）
    private Float sampleRate;          // 采样率（Hz）
    private Integer channels;          // 声道数
    private Integer bitDepth;          // 位深度
    private Integer bitrate;           // 比特率（kbps）

    // 时域特征
    private Double rms;                // 均方根音量
    private Double peak;               // 峰值
    private Double zeroCrossingRate;   // 过零率

    // 频域特征
    private Double spectralCentroid;   // 频谱质心
    private Double spectralRolloff;    // 频谱滚降点
    private Double spectralFlux;       // 频谱通量
    private Double mfcc1;              // MFCC第一系数
    private Double mfcc2;              // MFCC第二系数
    private Double mfcc3;              // MFCC第三系数

    // 音高特征
    private Double meanPitch;           // 平均基频
    private Double maxPitch;            // 最大基频
    private Double minPitch;            // 最小基频
    private Double pitchStd;            // 基频标准差

    // 语音特征
    private Double voiceActivityRatio;  // 语音活动比例
    private Integer speechSegments;     // 语音片段数量
    private Double averageSpeechDuration; // 平均语音时长

    // 自定义特征
    private Map<String, Object> customFeatures = new HashMap<>();

    /**
     * 添加自定义特征
     */
    public void addCustomFeature(String name, Object value) {
        customFeatures.put(name, value);
    }

    /**
     * 获取自定义特征
     */
    public Object getCustomFeature(String name) {
        return customFeatures.get(name);
    }

    /**
     * 转换为Map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put("fileName", fileName);
        map.put("fileSize", fileSize);
        map.put("duration", duration);
        map.put("sampleRate", sampleRate);
        map.put("channels", channels);
        map.put("bitDepth", bitDepth);
        map.put("bitrate", bitrate);

        map.put("rms", rms);
        map.put("peak", peak);
        map.put("zeroCrossingRate", zeroCrossingRate);

        map.put("spectralCentroid", spectralCentroid);
        map.put("spectralRolloff", spectralRolloff);
        map.put("spectralFlux", spectralFlux);
        map.put("mfcc", new double[]{mfcc1, mfcc2, mfcc3});

        map.put("meanPitch", meanPitch);
        map.put("maxPitch", maxPitch);
        map.put("minPitch", minPitch);
        map.put("pitchStd", pitchStd);

        map.put("voiceActivityRatio", voiceActivityRatio);
        map.put("speechSegments", speechSegments);
        map.put("averageSpeechDuration", averageSpeechDuration);

        map.put("customFeatures", customFeatures);

        return map;
    }
}
