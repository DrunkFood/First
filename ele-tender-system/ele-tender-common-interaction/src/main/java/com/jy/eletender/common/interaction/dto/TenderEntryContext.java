package com.jy.eletender.common.interaction.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 编制页面跳转上下文
 */
@Data
public class TenderEntryContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务类型，取值参见 {@link com.jy.eletender.common.interaction.enums.InteractionBizType#getCode()}。
     */
    private Integer bizType;

    private String bizId;

    private String projectId;

    private String tenderId;

    private String token;
}
