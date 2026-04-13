package com.jy.eleaitender.common.interaction.enums;

/**
 * 评标办法。
 * <p>对应 {@code ProjectBasicInfoResponse#evalMethod} 字段。</p>
 */
public enum InteractionEvalMethod {

    /** 最低评标价法 — 评审节点：资格审查、符合性评审、详细评审 */
    LOWEST_PRICE,

    /** 综合评分法 — 评审节点：资格审查、符合性评审、资信评审、技术评审、商务评审 */
    COMPREHENSIVE_SCORE
}
