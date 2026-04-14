package com.jy.eleaitender.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI任务状态枚举
 */
@Getter
@AllArgsConstructor
public enum AiTaskStatus {

    PENDING("PENDING", "待处理"),
    PROCESSING("PROCESSING", "处理中"),
    COMPLETED("COMPLETED", "已完成"),
    FAILED("FAILED", "失败"),
    AI_UNAVAILABLE("AI_UNAVAILABLE", "AI服务不可用"),
    SKIPPED("SKIPPED", "已跳过");

    private final String code;
    private final String label;

    public static AiTaskStatus fromCode(String code) {
        for (AiTaskStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的AI任务状态: " + code);
    }

    /**
     * 是否为终态
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == AI_UNAVAILABLE || this == SKIPPED;
    }

    /**
     * 是否可重试
     */
    public boolean isRetryable() {
        return this == FAILED || this == AI_UNAVAILABLE;
    }
}
