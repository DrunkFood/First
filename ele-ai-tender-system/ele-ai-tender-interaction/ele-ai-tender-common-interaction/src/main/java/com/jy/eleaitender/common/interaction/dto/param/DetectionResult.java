package com.jy.eleaitender.common.interaction.dto.param;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 检测结果内部类
 */
@Data
public class DetectionResult {
    /**
     * 记录ID
     */
    private Long recordId;
    /**
     * 检测分数
     */
    private Integer score = 100;
    /**
     * 检测问题VO
     */
    private List<DetectionIssue> issues = new ArrayList<>();
}

/**
 * 检测问题VO
 */
@Data
class DetectionIssue {
    /**
     * 问题位置描述
     */
    private String position;
    /**
     * 原文内容
     */
    private String original;
    /**
     * 修改内容(可直接替换原文)
     */
    private String targeted;
    /**
     * 修改建议
     */
    private String suggestion;
    /**
     * 问题原因说明
     */
    private String reason;
    /**
     * 严重程度: HIGH/MEDIUM/LOW
     */
    private String severity;
    /**
     * 检测类型
     */
    private String detectionType;
    /**
     * 相关政策引用（政策审查专用）
     */
    private String policyReference;
    /**
     * 违反的格式规则（格式检测专用）
     */
    private String ruleViolated;
}
