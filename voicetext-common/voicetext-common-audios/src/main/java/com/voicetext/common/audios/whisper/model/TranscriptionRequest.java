package com.voicetext.common.audios.whisper.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 转录请求参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 模型名称(base/small/medium/large)
     */
    private String model;

    /**
     * 语言代码(zh/en/ja等，为空则自动检测)
     */
    private String language;

    /**
     * 是否翻译为英文
     */
    @Builder.Default
    private boolean translate = false;

    /**
     * 是否返回时间戳
     */
    @Builder.Default
    private boolean wordTimestamps = true;

    /**
     * 初始提示(用于纠正特定词汇)
     */
    private String initialPrompt;

    /**
     * 温度参数(0-1)
     */
    @Builder.Default
    private float temperature = 0.0f;

    /**
     * 是否支持VAD预处理
     */
    @Builder.Default
    private boolean vadFilter = true;

    /**
     * 最大文本长度
     */
    private Integer maxTextLength;
}
