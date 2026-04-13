package com.jy.eleaitender.common.interaction.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 投标文件预存结果回调请求
 */
@Data
public class BidDocumentResultCallbackRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long fileId;

    /**
     * 预存结果，取值参见 {@link com.jy.eleaitender.common.interaction.enums.InteractionUploadResult}。
     */
    private String uploadResult;
}
