package com.voicetext.common.audios.project.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 项目音频项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectAudioItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 明细ID
     */
    private Long detailId;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 资源ID
     */
    private Long resourceId;

    /**
     * 资源名称
     */
    private String resourceName;

    /**
     * 来源租户ID
     */
    private Long sourceTenantId;

    /**
     * 音频时长(秒)
     */
    private Double audioDuration;

    /**
     * 序号
     */
    private Integer sequenceNo;

    /**
     * 截取开始时间(秒)
     */
    private Double startTime;

    /**
     * 截取结束时间(秒)
     */
    private Double endTime;

    /**
     * 实际时长(截取后)
     */
    private Double actualDuration;

    /**
     * 音量调整系数(0-2)
     */
    private Double volumeAdjust;

    /**
     * 淡入时长(秒)
     */
    private Double fadeIn;

    /**
     * 淡出时长(秒)
     */
    private Double fadeOut;

    /**
     * 播放速度(0.5-2)
     */
    @Builder.Default
    private Double speed = 1.0;

    /**
     * 是否静音
     */
    @Builder.Default
    private Boolean muted = false;

    /**
     * 备注
     */
    private String remark;

    /**
     * OSS URL(用于预览)
     */
    private String ossUrl;

    /**
     * 波形数据(缓存)
     */
    private float[] waveform;
}
