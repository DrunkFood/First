package com.jy.eleaitender.common.interaction.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 标录方案查询请求
 */
@Data
public class BidRecordSchemeQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务类型，取值参见 {@link com.jy.eleaitender.common.interaction.enums.InteractionBizType#getCode()}。
     */
    private Integer bizType;

    private String bizId;

    private String projectId;

    private String tenderId;
}
