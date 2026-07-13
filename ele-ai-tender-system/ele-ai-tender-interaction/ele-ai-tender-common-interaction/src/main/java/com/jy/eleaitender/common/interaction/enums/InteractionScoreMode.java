package com.jy.eleaitender.common.interaction.enums;

import lombok.Getter;

/**
 * 评审项评分模式
 */
@Getter
public enum InteractionScoreMode {

    /**
     * 分值模式：所有非符合性叶子评审项分值合计=100分
     */
    SCORE("SCORE", "分值模式：所有非符合性叶子评审项分值合计=100分"),

    /**
     * 权重模式：每个评分类型满分100分，类型间权重%合计=100%
     */
    WEIGHT("WEIGHT", "权重模式：每个评分类型满分100分，类型间权重%合计=100%");

    private final String code;
    private final String label;

    InteractionScoreMode(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
