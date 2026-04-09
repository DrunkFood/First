package com.jy.eletender.tenderdocument;

import com.jy.eletender.tenderdocument.enums.TenderDocumentEvalMethod;
import com.jy.eletender.tenderdocument.enums.TenderDocumentRuleCategory;
import com.jy.eletender.tenderdocument.enums.TenderDocumentStatus;
import com.jy.eletender.tenderdocument.enums.TenderDocumentStepCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TenderDocumentEnumTest {

    @Test
    void shouldKeepFixedStepOrder() {
        List<TenderDocumentStepCode> orderedSteps = TenderDocumentStepCode.orderedValues();

        assertThat(orderedSteps).containsExactly(
                TenderDocumentStepCode.BASIC_INFO,
                TenderDocumentStepCode.PURCHASE_FILE,
                TenderDocumentStepCode.BID_RECORD,
                TenderDocumentStepCode.EVALUATION_RULE,
                TenderDocumentStepCode.CHECK_ITEMS,
                TenderDocumentStepCode.GENERATE_PACKAGE
        );
        assertThat(TenderDocumentStepCode.BASIC_INFO.getStepOrder()).isEqualTo(1);
        assertThat(TenderDocumentStepCode.GENERATE_PACKAGE.getStepOrder()).isEqualTo(6);
    }

    @Test
    void shouldExposeExpectedDocumentStatuses() {
        assertThat(TenderDocumentStatus.DRAFT.name()).isEqualTo("DRAFT");
        assertThat(TenderDocumentStatus.GENERATING.name()).isEqualTo("GENERATING");
        assertThat(TenderDocumentStatus.COMPLETED.name()).isEqualTo("COMPLETED");
    }

    @Test
    void shouldResolveRuleCategoriesByEvalMethod() {
        assertThat(TenderDocumentRuleCategory.categoriesOf(TenderDocumentEvalMethod.LOWEST_PRICE))
                .containsExactly(
                        TenderDocumentRuleCategory.QUALIFICATION,
                        TenderDocumentRuleCategory.CONFORMITY,
                        TenderDocumentRuleCategory.DETAIL
                );

        assertThat(TenderDocumentRuleCategory.categoriesOf(TenderDocumentEvalMethod.COMPREHENSIVE_SCORE))
                .containsExactly(
                        TenderDocumentRuleCategory.QUALIFICATION,
                        TenderDocumentRuleCategory.CONFORMITY,
                        TenderDocumentRuleCategory.CREDIT,
                        TenderDocumentRuleCategory.TECHNICAL,
                        TenderDocumentRuleCategory.BUSINESS
                );
    }
}
