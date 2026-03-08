package com.voicetext.common.audios.whisper.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 转录片段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscriptionSegment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 片段ID
     */
    private Integer id;

    /**
     * 开始时间(秒)
     */
    private Double start;

    /**
     * 结束时间(秒)
     */
    private Double end;

    /**
     * 片段文本
     */
    private String text;

    /**
     * 置信度
     */
    private Double confidence;

    /**
     * 词级别时间戳
     */
    private List<WordTimestamp> words;
}

/**
 * 词级别时间戳
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class WordTimestamp implements Serializable {
    private String word;
    private Double start;
    private Double end;
    private Double confidence;
}
