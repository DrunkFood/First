package com.jy.eleaitender.support.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新模型配置请求DTO
 * 所有字段可选，仅更新传入的字段
 */
@Data
@Schema(description = "更新模型配置请求")
public class ModelConfigUpdateDTO {

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "模型类型: LOCAL/CLOUD/PRIVATE")
    private String modelType;

    @Schema(description = "模型代码(如deepseek-chat)")
    private String modelCode;

    @Schema(description = "API端点URL")
    private String endpoint;

    @Schema(description = "API密钥(RSA加密传输)，为空则不更新")
    private String apiKey;

    @Schema(description = "RSA密钥ID(用于解密apiKey)")
    private String keyId;

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

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "累计费用")
    private BigDecimal cost;
}
