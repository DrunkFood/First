package com.jy.eleaitender.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文档匹配结果VO
 */
@Data
@Schema(description = "文档匹配结果")
public class MatchResultVO {

    @Schema(description = "历史需求ID")
    private Long requirementId;

    @Schema(description = "需求名称")
    private String requirementName;

    @Schema(description = "关联项目名称")
    private String projectName;

    @Schema(description = "项目类型")
    private String projectType;

    @Schema(description = "项目类别")
    private String projectCategory;

    @Schema(description = "匹配度(0-100)")
    private Integer similarity;

    @Schema(description = "内容预览(前200字)")
    private String contentPreview;
}
