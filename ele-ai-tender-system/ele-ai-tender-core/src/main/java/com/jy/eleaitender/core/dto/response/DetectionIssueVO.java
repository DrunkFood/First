package com.jy.eleaitender.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 检测问题项VO
 */
@Data
@Schema(description = "检测问题项")
public class DetectionIssueVO {

    @Schema(description = "检测记录ID")
    private Long recordId;

    @Schema(description = "检测类型")
    private String detectionType;

    @Schema(description = "检测类型中文名")
    private String typeName;

    @Schema(description = "问题描述")
    private String description;

    @Schema(description = "问题位置(文本片段)")
    private String location;

    @Schema(description = "原始文本")
    private String original;

    @Schema(description = "替换文本(可直接替换原文)")
    private String targeted;

    @Schema(description = "建议修改内容")
    private String suggestion;

    @Schema(description = "严重程度: HIGH/MEDIUM/LOW")
    private String severity;

    @Schema(description = "是否已处理: 0-未处理 1-已接受 2-已拒绝")
    private Integer handleStatus;

    @Schema(description = "问题索引(在result JSON issues数组中的位置)")
    private Integer issueIndex;

    @Schema(description = "相关政策引用（政策审查专用）")
    private String policyReference;

    @Schema(description = "违反的格式规则（格式检测专用）")
    private String ruleViolated;
}
