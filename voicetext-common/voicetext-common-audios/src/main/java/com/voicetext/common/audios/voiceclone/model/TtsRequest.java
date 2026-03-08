package com.voicetext.common.audios.voiceclone.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * TTS请求参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TtsRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 待合成文本
     */
    private String text;

    /**
     * 语音ID
     */
    private String voice;

    /**
     * 语速(0.5-2.0)
     */
    @Builder.Default
    private float speed = 1.0f;

    /**
     * 音调(0.5-2.0)
     */
    @Builder.Default
    private float pitch = 1.0f;

    /**
     * 音量(0-1)
     */
    @Builder.Default
    private float volume = 1.0f;

    /**
     * 情感(happy/sad/angry等)
     */
    private String emotion;

    /**
     * 输出格式(mp3/wav/ogg)
     */
    @Builder.Default
    private String format = "mp3";

    /**
     * 采样率
     */
    private Integer sampleRate;

    /**
     * 额外参数
     */
    private Map<String, Object> extraParams;
}
