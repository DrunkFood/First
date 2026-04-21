package com.jy.eleaitender.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI任务类型枚举
 */
@Getter
@AllArgsConstructor
public enum AiTaskType {

    REQUIREMENT_GENERATE("REQUIREMENT_GENERATE", "需求生成"),
    REVIEW_ITEM_GENERATE("REVIEW_ITEM_GENERATE", "评审项生成"),
    DETECTION_SENSITIVE_WORD("DETECTION_SENSITIVE_WORD", "敏感词检测"),
    DETECTION_TYPO("DETECTION_TYPO", "错别字检测"),
    DETECTION_POLICY_REVIEW("DETECTION_POLICY_REVIEW", "政策文件审查"),
    DETECTION_FORMAT_CHECK("DETECTION_FORMAT_CHECK", "格式规范检测"),
    TEXT_OPTIMIZE("TEXT_OPTIMIZE", "文本优化");

    private final String code;
    private final String label;

    public static AiTaskType fromCode(String code) {
        for (AiTaskType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的AI任务类型: " + code);
    }

    public static AiTaskType mapToTaskType(DetectionType detectionType) {
        return switch (detectionType) {
            case SENSITIVE_WORD -> AiTaskType.DETECTION_SENSITIVE_WORD;
            case TYPO -> AiTaskType.DETECTION_TYPO;
            case POLICY_REVIEW -> AiTaskType.DETECTION_POLICY_REVIEW;
            case FORMAT_CHECK -> AiTaskType.DETECTION_FORMAT_CHECK;
        };
    }

}
