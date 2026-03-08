package com.voicetext.common.audios.voiceclone.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 语音信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 语音ID
     */
    private String voiceId;

    /**
     * 语音名称
     */
    private String name;

    /**
     * 性别(male/female)
     */
    private String gender;

    /**
     * 语言代码
     */
    private String language;

    /**
     * 语言名称
     */
    private String languageName;

    /**
     * 示例音频URL
     */
    private String sampleUrl;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     * 是否免费
     */
    private boolean free;

    /**
     * 描述
     */
    private String description;
}
