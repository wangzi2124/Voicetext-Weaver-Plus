package com.voicetext.common.audios.project.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 合并配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 输出格式
     */
    @Builder.Default
    private String outputFormat = "mp3";

    /**
     * 比特率(kbps)
     */
    @Builder.Default
    private Integer bitrate = 192;

    /**
     * 采样率(Hz)
     */
    @Builder.Default
    private Integer sampleRate = 44100;

    /**
     * 交叉淡变时长(秒)
     */
    @Builder.Default
    private Double crossFade = 0.5;

    /**
     * 是否音量归一化
     */
    @Builder.Default
    private Boolean normalize = false;

    /**
     * 目标音量(dB)
     */
    @Builder.Default
    private Double targetDb = -16.0;

    /**
     * 全局音量调整
     */
    @Builder.Default
    private Double globalVolume = 1.0;

    /**
     * 合并策略: sequential/concurrent/smart
     */
    @Builder.Default
    private String mergeStrategy = "sequential";

    /**
     * 背景音乐ID(如果有)
     */
    private Long bgmResourceId;

    /**
     * 背景音乐音量
     */
    @Builder.Default
    private Double bgmVolume = 0.3;

    /**
     * 额外参数
     */
    private Map<String, Object> extraParams;
}
