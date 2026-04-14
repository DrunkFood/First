package com.jy.eleaitender.ai.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 检测问题VO
 */
@Data
@Schema(description = "检测问题项")
public class DetectionIssueVO {

    @Schema(description = "问题位置描述")
    private String position;

    @Schema(description = "原文内容")
    private String original;

    @Schema(description = "修改建议")
    private String suggestion;

    @Schema(description = "问题原因说明")
    private String reason;

    @Schema(description = "严重程度: HIGH/MEDIUM/LOW")
    private String severity;

    @Schema(description = "检测类型")
    private String detectionType;

    @Schema(description = "相关政策引用（政策审查专用）")
    private String policyReference;

    @Schema(description = "违反的格式规则（格式检测专用）")
    private String ruleViolated;
}
