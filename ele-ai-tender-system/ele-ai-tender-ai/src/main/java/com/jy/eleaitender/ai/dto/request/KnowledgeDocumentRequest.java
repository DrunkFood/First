package com.jy.eleaitender.ai.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "知识文档创建请求")
public class KnowledgeDocumentRequest {
    @NotBlank(message = "文档名称不能为空")
    @Schema(description = "文档名称")
    private String docName;
    @Schema(description = "文档类别")
    private String docCategory;
    @Schema(description = "文件ID")
    private Long fileId;
    @Schema(description = "文件类型")
    private String fileType;
}
