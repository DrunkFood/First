package com.jy.eleaitender.common.interaction.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 投标文件预存请求
 */
@Data
public class BidDocumentPushRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long fileId;

    private String fileSha256;
}
