package com.jy.eleaitender.core.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(description = "项目创建/更新请求")
public class ProjectRequest {
    @NotBlank(message = "项目名称不能为空")
    @Schema(description = "项目名称")
    private String projectName;
    
    @NotBlank(message = "项目类别不能为空")
    @Schema(description = "项目类别")
    private String projectCategory;
    
    @NotBlank(message = "项目类型不能为空")
    @Schema(description = "项目类型")
    private String projectType;

    @Schema(description = "服务子类型")
    private String serviceSubType;

    @NotNull(message = "预算金额不能为空")
    @Positive(message = "预算金额必须大于0")
    @Schema(description = "预算金额(元)")
    private BigDecimal budget;
    
    @Schema(description = "评审类型")
    private String reviewType;
    
    @Schema(description = "招标需求内容")
    private String requirementContent;
}
