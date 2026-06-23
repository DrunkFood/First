package com.jy.eleaitender.core.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 政策文件请求DTO
 */
@Data
@Schema(description = "政策文件请求")
public class PolicyFileRequest {

    @Schema(description = "文件名称", required = true)
    private String fileName;

    @Schema(description = "文件分类: LAW/REGULATION/POLICY", required = true)
    private String fileCategory;

    @Schema(description = "适用项目类别，多个用逗号分隔")
    private String applicableCategory;

    @Schema(description = "关联file_info的id")
    private Long fileId;

    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    @Schema(description = "文件格式: PDF/DOCX/DOC/XLSX")
    private String fileType;

    @Schema(description = "文件描述")
    private String description;
}
