package com.jy.eleaitender.support.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 模型配置响应VO
 * 字段名与前端对齐，apiKey脱敏
 */
@Data
@Schema(description = "模型配置视图对象")
public class ModelConfigVO {

    @Schema(description = "配置ID")
    private Long id;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "模型类型: LOCAL/CLOUD/PRIVATE")
    private String modelType;

    @Schema(description = "模型供应商: OPENAI/ZHIPU")
    private String provider;

    @Schema(description = "模型代码(如deepseek-chat)")
    private String modelCode;

    @Schema(description = "API端点URL")
    private String endpoint;

    @Schema(description = "API密钥(脱敏)")
    private String apiKey;

    @Schema(description = "最大Token数")
    private Integer maxTokens;

    @Schema(description = "Temperature")
    private Double temperature;

    @Schema(description = "Top P")
    private Double topP;

    @Schema(description = "超时时间(ms)")
    private Integer timeout;

    @Schema(description = "Token限制(0表示不限)")
    private Long tokenLimit;

    @Schema(description = "使用场景: GENERATION/OPTIMIZATION/DETECTION")
    private String usageScenario;

    @Schema(description = "高级参数(JSON)")
    private String parameters;

    @Schema(description = "是否启用: 1-启用 0-停用")
    private Integer isActive;

    @Schema(description = "Token使用量")
    private Long tokenUsage;

    @Schema(description = "累计费用")
    private BigDecimal cost;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private String createTime;

    @Schema(description = "修改时间")
    private String modifyTime;

    @Schema(description = "创建人")
    private String createName;
}
