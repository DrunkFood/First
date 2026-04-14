package com.jy.eleaitender.common.entity.support;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eleaitender.common.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模型路由规则实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sup_model_route_rule")
@Schema(description = "模型路由规则")
public class SupModelRouteRule extends BaseEntity {

    @Schema(description = "使用场景: GENERATION/OPTIMIZATION/DETECTION")
    private String usageScenario;

    @Schema(description = "优先模型ID(关联ai_model_config.id)")
    private Long primaryModelId;

    @Schema(description = "降级模型ID(关联ai_model_config.id)")
    private Long fallbackModelId;

    @Schema(description = "优先级(值越小优先级越高)")
    private Integer priority;

    @Schema(description = "是否启用: 0-停用, 1-启用")
    private Integer isActive;

    @Schema(description = "规则描述")
    private String description;
}
