package com.jy.eleaitender.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 评审项评分模式
 */
public enum ScoreMode {

    /** 分值模式：所有非符合性叶子评审项分值合计=100分 */
    SCORE,

    /** 权重模式：每个评分类型满分100分，类型间权重%合计=100% */
    WEIGHT;

    /**
     * 安全解析：null 或无法识别时回退 SCORE（向后兼容老数据）。
     * 标注 @JsonCreator 使 Jackson 反序列化走本方法，从而对非法值容错而非抛异常。
     */
    @JsonCreator
    public static ScoreMode fromString(String value) {
        if (value == null) return SCORE;
        try {
            return ScoreMode.valueOf(value);
        } catch (IllegalArgumentException e) {
            return SCORE;
        }
    }
}
