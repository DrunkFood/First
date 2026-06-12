package com.jy.eleaitender.core.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(description = "需求创建/更新请求")
public class RequirementRequest {
    @NotBlank(message = "需求名称不能为空")
    @Schema(description = "需求名称")
    private String requirementName;
    
    @NotBlank(message = "项目类别不能为空")
    @Schema(description = "项目类别")
    private String projectCategory;
    
    @NotBlank(message = "项目类型不能为空")
    @Schema(description = "项目类型")
    private String projectType;
    
    @NotNull(message = "预算价不能为空")
    @Positive(message = "预算价必须大于0")
    @Schema(description = "预算价(元)")
    private BigDecimal budget;
    
    @Schema(description = "需求描述")
    private String requirementDescription;
    
    @Schema(description = "匹配模式")
    private String matchMode;
    
    @Schema(description = "关联的项目ID")
    private Long projectId;
    
    @Schema(description = "业务需求内容")
    private String content;
}
