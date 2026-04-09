package com.jy.eletender.common.interaction.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 项目基本信息查询请求
 */
@Data
public class ProjectBasicInfoQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务类型，取值参见 {@link com.jy.eletender.common.interaction.enums.InteractionBizType#getCode()}。
     */
    private Integer bizType;

    private String bizId;

    private String projectId;

    private String tenderId;
}
