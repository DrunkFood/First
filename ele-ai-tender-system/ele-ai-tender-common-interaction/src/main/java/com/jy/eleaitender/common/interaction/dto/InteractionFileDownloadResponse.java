package com.jy.eleaitender.common.interaction.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 文件下载响应
 */
@Data
public class InteractionFileDownloadResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long fileId;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private byte[] content;
}
