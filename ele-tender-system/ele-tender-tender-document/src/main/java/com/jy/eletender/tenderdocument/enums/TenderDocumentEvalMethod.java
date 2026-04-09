package com.jy.eletender.tenderdocument.enums;

/**
 * 评标办法
 */
public enum TenderDocumentEvalMethod {
    LOWEST_PRICE,
    COMPREHENSIVE_SCORE;

    /**
     * 从字符串解析评标办法，匹配不到综合评分法时默认返回最低评标价法。
     */
    public static TenderDocumentEvalMethod fromString(String evalMethod) {
        return COMPREHENSIVE_SCORE.name().equalsIgnoreCase(evalMethod)
                ? COMPREHENSIVE_SCORE
                : LOWEST_PRICE;
    }
}
