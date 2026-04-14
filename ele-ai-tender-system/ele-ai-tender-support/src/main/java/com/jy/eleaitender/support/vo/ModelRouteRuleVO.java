package com.jy.eleaitender.support.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 模型路由规则 VO（带模型名称）
 */
@Data
@Schema(description = "模型路由规则视图对象")
public class ModelRouteRuleVO {

    @Schema(description = "规则ID")
    private Long id;

    @Schema(description = "使用场景")
    private String usageScenario;

    @Schema(description = "优先模型ID")
    private Long primaryModelId;

    @Schema(description = "优先模型名称")
    private String primaryModelName;

    @Schema(description = "降级模型ID")
    private Long fallbackModelId;

    @Schema(description = "降级模型名称")
    private String fallbackModelName;

    @Schema(description = "优先级")
    private Integer priority;

    @Schema(description = "是否启用")
    private Integer isActive;

    @Schema(description = "规则描述")
    private String description;
}
