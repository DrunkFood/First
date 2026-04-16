package com.jy.eleaitender.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 反馈场景枚举
 */
@Getter
@AllArgsConstructor
public enum FeedbackScene {

    GENERATION_CONTENT("GENERATION_CONTENT", "生成内容反馈"),
    CHAT_MESSAGE("CHAT_MESSAGE", "聊天消息反馈");

    private final String code;
    private final String label;

    public static FeedbackScene fromCode(String code) {
        for (FeedbackScene scene : values()) {
            if (scene.code.equals(code)) {
                return scene;
            }
        }
        throw new IllegalArgumentException("未知的反馈场景: " + code);
    }
}
