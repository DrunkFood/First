package com.jy.eletender.common.interaction.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 文件上传响应
 */
@Data
public class InteractionFileUploadResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long fileId;
    private String fileName;
    private Long fileSize;
    private String fileSha256;
}
