package com.voicetext.common.audios.tts.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * TTS响应结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TtsResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 音频数据
     */
    private byte[] audioData;

    /**
     * 音频格式
     */
    private String format;

    /**
     * 音频时长(秒)
     */
    private Double duration;

    /**
     * 合成文本
     */
    private String text;

    /**
     * 使用的语音
     */
    private String voice;

    /**
     * 任务ID(异步)
     */
    private String taskId;

    /**
     * 是否来自缓存
     */
    private boolean fromCache;

    /**
     * 错误信息
     */
    private String error;
}
