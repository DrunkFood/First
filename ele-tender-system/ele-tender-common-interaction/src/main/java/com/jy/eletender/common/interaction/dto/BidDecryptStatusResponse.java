package com.jy.eletender.common.interaction.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 投标文件解密状态响应
 */
@Data
public class BidDecryptStatusResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String recordId;

    /**
     * 解密状态，取值参见 {@link com.jy.eletender.common.interaction.enums.InteractionDecryptStatus}。
     */
    private String status;

    private String errorMessage;

    private Date completedAt;
}
