package com.jy.eleaitender.common.entity.support;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eleaitender.common.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_model_config")
@Schema(description = "AI模型配置")
public class AiModelConfig extends BaseEntity {
    @Schema(description = "模型名称")
    private String modelName;
    @Schema(description = "模型类型")
    private String modelType;
    @Schema(description = "模型供应商: OPENAI/ZHIPU，默认OPENAI")
    private String provider;
    @Schema(description = "API端点")
    private String apiEndpoint;
    @Schema(description = "API密钥(加密)")
    private String apiKey;
    @Schema(description = "模型参数JSON")
    private String modelParams;
    @Schema(description = "使用场景")
    private String usageScenario;
    @Schema(description = "是否启用")
    private Integer isActive;
    @Schema(description = "Token使用量")
    private Long tokenUsage;
    @Schema(description = "累计费用")
    private BigDecimal cost;
    @Schema(description = "备注")
    private String remark;
}
