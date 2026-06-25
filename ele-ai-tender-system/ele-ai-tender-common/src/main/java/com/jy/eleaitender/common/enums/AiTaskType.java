package com.jy.eleaitender.common.enums;

import com.jy.eleaitender.common.dto.ai.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI任务类型枚举
 */
@Getter
@AllArgsConstructor
public enum AiTaskType {

    REQUIREMENT_GENERATE("REQUIREMENT_GENERATE", "需求生成", 20, RequirementGenerateParams.class),
    PROJECT_REQUIREMENT_GENERATE("PROJECT_REQUIREMENT_GENERATE", "项目需求生成", 20, RequirementGenerateParams.class),

    REVIEW_ITEM_GENERATE("REVIEW_ITEM_GENERATE", "评审项生成", 10, ReviewItemGenerateParams.class),

    DOCUMENT_INTEGRATION("DOCUMENT_INTEGRATION", "文档集成", 10, DocumentIntegrationParams.class),

    DETECTION_SENSITIVE_WORD("DETECTION_SENSITIVE_WORD", "敏感词检测", 15, DetectionParams.class),
    DETECTION_TYPO("DETECTION_TYPO", "错别字检测", 15, DetectionParams.class),
    DETECTION_POLICY_REVIEW("DETECTION_POLICY_REVIEW", "政策文件审查", 15, DetectionParams.class),
    DETECTION_FORMAT_CHECK("DETECTION_FORMAT_CHECK", "格式规范检测", 15, DetectionParams.class),

    TEXT_OPTIMIZE("TEXT_OPTIMIZE", "文本优化", 10, TextOptimizeParams.class);

    private final String code;
    private final String label;
    private final Integer timeout;
    private final Class<?> clazz;

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
