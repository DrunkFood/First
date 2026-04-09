package com.jy.eletender.tenderdocument.enums;

import java.util.List;

/**
 * 评审规则分类
 */
public enum TenderDocumentRuleCategory {
    QUALIFICATION("资格审查"),
    CONFORMITY("符合性评审"),
    DETAIL("详细评审"),
    CREDIT("资信评审"),
    TECHNICAL("技术评审"),
    BUSINESS("商务评审");

    private final String displayName;

    TenderDocumentRuleCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static List<TenderDocumentRuleCategory> categoriesOf(TenderDocumentEvalMethod evalMethod) {
        return switch (evalMethod) {
            case LOWEST_PRICE -> List.of(QUALIFICATION, CONFORMITY, DETAIL);
            case COMPREHENSIVE_SCORE -> List.of(QUALIFICATION, CONFORMITY, CREDIT, TECHNICAL, BUSINESS);
        };
    }

    public static boolean supports(TenderDocumentEvalMethod evalMethod, TenderDocumentRuleCategory category) {
        return categoriesOf(evalMethod).contains(category);
    }

    public TenderDocumentReviewMode fixedReviewMode() {
        return switch (this) {
            case QUALIFICATION, CONFORMITY, DETAIL -> TenderDocumentReviewMode.PASS;
            case CREDIT, TECHNICAL, BUSINESS -> TenderDocumentReviewMode.SCORE;
        };
    }
}
