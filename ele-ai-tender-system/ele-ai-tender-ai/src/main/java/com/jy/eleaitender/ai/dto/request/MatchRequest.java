package com.jy.eleaitender.ai.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 文档匹配请求DTO
 */
@Data
@Schema(description = "文档匹配请求")
public class MatchRequest {

    @NotBlank(message = "项目类型不能为空")
    @Schema(description = "项目类型: ENGINEERING/GOODS/SERVICE")
    private String projectType;

    @Schema(description = "项目类别: LIMITED_BELOW/PROPERTY_TRADE/GOVERNMENT_PROCUREMENT")
    private String projectCategory;

    @Schema(description = "项目预算(万元)")
    private BigDecimal budget;

    @Schema(description = "项目描述/关键词")
    private String description;

    @Schema(description = "项目名称")
    private String projectName;
}
