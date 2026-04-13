package com.jy.eleaitender.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 文件上传响应DTO
 */
@Data
@Schema(description = "文件上传响应")
public class FileUploadResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文件ID")
    private Long fileId;

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    @Schema(description = "文件SHA-256")
    private String fileSha256;

    public FileUploadResponse() {}

    public FileUploadResponse(Long fileId, String fileName, Long fileSize, String fileSha256) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.fileSha256 = fileSha256;
    }
}
