package com.jy.eleaitender.common.interaction.enums;

import lombok.Getter;

/**
 * 交互业务类型。
 * <p>业务系统在请求中通过 {@code bizType} 字段传入对应的 {@link #getCode() code} 值。</p>
 */
@Getter
public enum InteractionBizType {

    /** 立项 — 项目初始创建场景 */
    PROJECT(1, "立项"),

    /** 答疑 — 招标答疑/澄清场景 */
    CLARIFICATION(2, "答疑");

    private final int code;
    private final String description;

    InteractionBizType(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
