package com.voicetext.common.audios.dsp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 语音活动检测结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VADResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 语音段落列表 [start, end]
     */
    private List<double[]> voiceSegments;

    /**
     * 静音段落列表 [start, end]
     */
    private List<double[]> silenceSegments;

    /**
     * 总语音时长(秒)
     */
    private double totalVoiceDuration;

    /**
     * 总静音时长(秒)
     */
    private double totalSilenceDuration;

    /**
     * 语音占比(0-1)
     */
    private double voiceRatio;

    /**
     * 是否包含语音
     */
    public boolean hasVoice() {
        return !voiceSegments.isEmpty();
    }

    /**
     * 获取语音段数量
     */
    public int getVoiceSegmentCount() {
        return voiceSegments.size();
    }
}
