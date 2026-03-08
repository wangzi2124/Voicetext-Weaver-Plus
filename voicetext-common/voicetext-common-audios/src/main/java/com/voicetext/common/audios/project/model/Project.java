package com.voicetext.common.audios.project.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 音频项目模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 项目编号
     */
    private String projectNo;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 项目描述
     */
    private String projectDesc;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 创建者ID
     */
    private Long creatorId;

    /**
     * 创建者名称
     */
    private String creatorName;

    /**
     * 项目状态: 0删除 1正常 2已完成 3已归档
     */
    private Integer projectStatus;

    /**
     * 输出资源ID
     */
    private Long outputResourceId;

    /**
     * 输出音频URL
     */
    private String outputUrl;

    /**
     * 输出音频时长(秒)
     */
    private Double outputDuration;

    /**
     * 输出格式
     */
    private String outputFormat;

    /**
     * 合并配置
     */
    private MergeConfig mergeConfig;

    /**
     * 项目音频项列表
     */
    private List<ProjectAudioItem> audioItems;

    /**
     * 当前版本号
     */
    private Integer currentVersion;

    /**
     * 是否已恢复
     */
    private Boolean isRecovered;

    /**
     * 恢复自哪个项目ID
     */
    private Long recoverFromId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 删除时间
     */
    private Date deleteTime;
}
