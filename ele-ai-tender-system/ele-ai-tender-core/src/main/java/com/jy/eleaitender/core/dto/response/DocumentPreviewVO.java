package com.jy.eleaitender.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文档预览VO
 */
@Data
@Schema(description = "文档预览")
public class DocumentPreviewVO {

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "HTML预览内容")
    private String htmlContent;

    @Schema(description = "Markdown原始内容")
    private String markdownContent;

    @Schema(description = "是否已集成")
    private Boolean integrated;
}
