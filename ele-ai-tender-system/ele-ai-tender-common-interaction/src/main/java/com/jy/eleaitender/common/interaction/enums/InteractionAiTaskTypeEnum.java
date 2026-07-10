package com.jy.eleaitender.common.interaction.enums;

import lombok.Getter;

/**
 * AI任务类型枚举（对外暴露）
 */
@Getter
public enum InteractionAiTaskTypeEnum {

    REQUIREMENT_GENERATE("REQUIREMENT_GENERATE", "需求生成"),
    PROJECT_REQUIREMENT_GENERATE("PROJECT_REQUIREMENT_GENERATE", "项目需求生成"),
    REVIEW_ITEM_GENERATE("REVIEW_ITEM_GENERATE", "评审项生成"),
    DOCUMENT_INTEGRATION("DOCUMENT_INTEGRATION", "文档集成"),
    DETECTION_SENSITIVE_WORD("DETECTION_SENSITIVE_WORD", "敏感词检测"),
    DETECTION_TYPO("DETECTION_TYPO", "错别字检测"),
    DETECTION_POLICY_REVIEW("DETECTION_POLICY_REVIEW", "政策文件审查"),
    DETECTION_FORMAT_CHECK("DETECTION_FORMAT_CHECK", "格式规范检测"),
    TEXT_OPTIMIZE("TEXT_OPTIMIZE", "文本优化");

    private final String code;
    private final String label;

    InteractionAiTaskTypeEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static InteractionAiTaskTypeEnum fromCode(String code) {
        for (InteractionAiTaskTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的AI任务类型: " + code);
    }
}
