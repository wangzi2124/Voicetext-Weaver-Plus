package com.voicetext.common.audios.whisper.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 转录结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptionResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 完整文本
     */
    private String text;

    /**
     * 检测到的语言
     */
    private String language;

    /**
     * 语言置信度
     */
    private Double languageConfidence;

    /**
     * 音频时长(秒)
     */
    private Double duration;

    /**
     * 分段结果
     */
    private List<TranscriptionSegment> segments;

    /**
     * 任务ID(异步调用时)
     */
    private String taskId;

    /**
     * 任务状态
     */
    private String taskStatus;

    /**
     * 错误信息
     */
    private String error;
}
