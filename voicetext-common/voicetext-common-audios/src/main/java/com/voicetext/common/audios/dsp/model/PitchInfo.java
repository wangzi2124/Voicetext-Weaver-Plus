package com.voicetext.common.audios.dsp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 音高信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PitchInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 时间戳(秒)
     */
    private double timeStamp;

    /**
     * 音高频率(Hz)
     */
    private float pitch;

    /**
     * 概率(0-1)
     */
    private float probability;

    /**
     * 是否为浊音
     */
    private boolean voiced;
}
