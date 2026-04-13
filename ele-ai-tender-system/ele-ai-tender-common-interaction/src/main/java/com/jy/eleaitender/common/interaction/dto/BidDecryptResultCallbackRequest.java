package com.jy.eleaitender.common.interaction.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 投标文件解密结果回调请求
 */
@Data
public class BidDecryptResultCallbackRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String bidRecordId;

    private String projectId;

    private String tenderId;

    /**
     * 解密状态，取值参见 {@link com.jy.eleaitender.common.interaction.enums.InteractionDecryptStatus}。
     */
    private String status;

    private String errorMessage;

    private String bidRecordData;
}
