package com.voicetext.common.audios.project.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 合并结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 资源ID
     */
    private Long resourceId;

    /**
     * 输出URL
     */
    private String outputUrl;

    /**
     * 输出格式
     */
    private String format;

    /**
     * 音频时长(秒)
     */
    private Double duration;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 状态
     */
    private String status;

    /**
     * 错误信息
     */
    private String error;

    /**
     * 预计剩余时间(秒)
     */
    private Integer eta;
}
