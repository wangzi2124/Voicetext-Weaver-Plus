package com.voicetext.common.audios.dsp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 音频片段模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioSegment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 片段ID
     */
    private String segmentId;

    /**
     * 开始时间(秒)
     */
    private double startTime;

    /**
     * 结束时间(秒)
     */
    private double endTime;

    /**
     * 时长(秒)
     */
    private double duration;

    /**
     * 片段类型: voice/silence/music
     */
    private String type;

    /**
     * 置信度(0-1)
     */
    private double confidence;

    /**
     * 标签(可选)
     */
    private String label;

    /**
     * 是否为主干内容
     */
    private boolean isMainContent;

    public double getDuration() {
        return endTime - startTime;
    }
}
