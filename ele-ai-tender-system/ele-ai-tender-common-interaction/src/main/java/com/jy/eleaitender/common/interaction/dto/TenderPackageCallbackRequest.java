package com.jy.eleaitender.common.interaction.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 电子标招标文件回传请求
 */
@Data
public class TenderPackageCallbackRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务类型，取值参见 {@link com.jy.eleaitender.common.interaction.enums.InteractionBizType#getCode()}。
     */
    private Integer bizType;

    private String bizId;

    private String projectId;

    private String tenderId;

    private Long fileId;

    private String fileName;
}
