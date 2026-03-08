package com.voicetext.common.audios.project.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 项目版本
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 版本ID
     */
    private Long versionId;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 版本号
     */
    private Integer versionNo;

    /**
     * 项目快照(JSON)
     */
    private String snapshot;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人名称
     */
    private String operatorName;

    /**
     * 操作类型: edit/merge/recover
     */
    private String operationType;

    /**
     * 操作描述
     */
    private String operationDesc;

    /**
     * 创建时间
     */
    private Date createTime;
}
