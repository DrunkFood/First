package com.jy.eletender.common.interaction.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 投标文件解密提交响应
 */
@Data
public class BidDecryptSubmitResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String recordId;
}
