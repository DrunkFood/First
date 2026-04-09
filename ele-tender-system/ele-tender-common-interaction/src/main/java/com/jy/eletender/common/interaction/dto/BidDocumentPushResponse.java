package com.jy.eletender.common.interaction.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 投标文件预存响应
 */
@Data
public class BidDocumentPushResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long fileId;

    /**
     * 预存结果，取值参见 {@link com.jy.eletender.common.interaction.enums.InteractionUploadResult}。
     */
    private String uploadResult;
}
