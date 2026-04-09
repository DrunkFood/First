package com.jy.eletender.tenderdocument.support;

import com.jy.eletender.tenderdocument.dto.request.TenderDocumentRuleSaveRequest;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentRuleTreeNode;
import com.jy.eletender.tenderdocument.enums.TenderDocumentReviewMode;
import com.jy.eletender.tenderdocument.enums.TenderDocumentRuleCategory;
import com.jy.eletender.tenderdocument.enums.TenderDocumentScoreAttribute;
import com.jy.eletender.tenderdocument.enums.TenderDocumentScoreType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TenderDocumentRuleValidatorTest {

    private final TenderDocumentRuleValidator validator = new TenderDocumentRuleValidator();

    @Test
    void shouldRejectParentScoreMismatch() {
        TenderDocumentRuleTreeNode child = buildScoreLeaf("叶子项", "2", "1.1", 40, 0);
        TenderDocumentRuleTreeNode parent = buildParent("父项", "1", "1", 30, 0, List.of(child));
        TenderDocumentRuleSaveRequest request = buildScoreRequest(TenderDocumentRuleCategory.CREDIT, 30, 100, List.of(parent));

        Map<String, List<String>> errors = validator.validateSingleCategory(request);

        assertThat(errors.get(TenderDocumentRuleCategory.CREDIT.name()))
                .anySatisfy(message -> assertThat(message).contains("父节点最高分"));
    }

    @Test
    void shouldRejectActualScoreWhenTotalNotHundredAcrossCategories() {
        TenderDocumentRuleSaveRequest credit = buildScoreRequest(TenderDocumentRuleCategory.CREDIT, 40, 100,
                List.of(buildScoreLeaf("资信项", "1", "1", 40, 0)));
        TenderDocumentRuleSaveRequest technical = buildScoreRequest(TenderDocumentRuleCategory.TECHNICAL, 30, 100,
                List.of(buildScoreLeaf("技术项", "1", "1", 30, 0)));
        TenderDocumentRuleSaveRequest business = buildScoreRequest(TenderDocumentRuleCategory.BUSINESS, 20, 100,
                List.of(buildScoreLeaf("商务项", "1", "1", 20, 0)));

        Map<String, List<String>> errors = validator.validateCrossCategory(List.of(credit, technical, business), TenderDocumentScoreType.ACTUAL.name());

        assertThat(errors.get("CROSS_CATEGORY"))
                .anySatisfy(message -> assertThat(message).contains("总分之和必须等于100"));
    }

    @Test
    void shouldRejectWeightScoreWhenWeightRateSumNotHundred() {
        TenderDocumentRuleSaveRequest credit = buildScoreRequest(TenderDocumentRuleCategory.CREDIT, 100, 30,
                List.of(buildScoreLeaf("资信项", "1", "1", 100, 0)));
        TenderDocumentRuleSaveRequest technical = buildScoreRequest(TenderDocumentRuleCategory.TECHNICAL, 100, 30,
                List.of(buildScoreLeaf("技术项", "1", "1", 100, 0)));
        TenderDocumentRuleSaveRequest business = buildScoreRequest(TenderDocumentRuleCategory.BUSINESS, 100, 20,
                List.of(buildScoreLeaf("商务项", "1", "1", 100, 0)));

        Map<String, List<String>> errors = validator.validateCrossCategory(List.of(credit, technical, business), TenderDocumentScoreType.WEIGHT.name());

        assertThat(errors.get("CROSS_CATEGORY"))
                .anySatisfy(message -> assertThat(message).contains("比例之和必须等于100"));
    }

    @Test
    void shouldAllowPassCategoryWithoutJectiveTypeWhenReviewModeIsPass() {
        TenderDocumentRuleSaveRequest request = new TenderDocumentRuleSaveRequest();
        request.setTenderId("T-01");
        request.setTenderName("一标段");
        request.setEvalMethod("LOWEST_PRICE");
        request.setNodeCategory(TenderDocumentRuleCategory.QUALIFICATION.name());
        request.setReviewMode(TenderDocumentReviewMode.PASS.name());
        request.setTreeToPageData(List.of(buildPassLeaf("资格项", "1", "1")));

        Map<String, List<String>> errors = validator.validateSingleCategory(request);

        assertThat(errors).isEmpty();
    }

    private TenderDocumentRuleSaveRequest buildScoreRequest(TenderDocumentRuleCategory category,
                                                            int totalScore,
                                                            int weightRate,
                                                            List<TenderDocumentRuleTreeNode> nodes) {
        TenderDocumentRuleSaveRequest request = new TenderDocumentRuleSaveRequest();
        request.setTenderId("T-01");
        request.setTenderName("一标段");
        request.setEvalMethod("COMPREHENSIVE_SCORE");
        request.setNodeCategory(category.name());
        request.setReviewMode(TenderDocumentReviewMode.SCORE.name());
        request.setTotalScore(BigDecimal.valueOf(totalScore));
        request.setWeightRate(BigDecimal.valueOf(weightRate));
        request.setTreeToPageData(nodes);
        return request;
    }

    private TenderDocumentRuleTreeNode buildParent(String name, String id, String key, int highest, int lowest,
                                                   List<TenderDocumentRuleTreeNode> children) {
        TenderDocumentRuleTreeNode node = new TenderDocumentRuleTreeNode();
        node.setId(id);
        node.setOrder(1);
        node.setKey(key);
        node.setName(name);
        node.setPass(false);
        node.setLowest(BigDecimal.valueOf(lowest));
        node.setHighest(BigDecimal.valueOf(highest));
        node.setParent(true);
        node.setChildren(children);
        return node;
    }

    private TenderDocumentRuleTreeNode buildScoreLeaf(String name, String id, String key, int highest, int lowest) {
        TenderDocumentRuleTreeNode node = new TenderDocumentRuleTreeNode();
        node.setId(id);
        node.setOrder(1);
        node.setKey(key);
        node.setName(name);
        node.setPass(false);
        node.setLowest(BigDecimal.valueOf(lowest));
        node.setHighest(BigDecimal.valueOf(highest));
        node.setStandard("评分标准");
        node.setParent(false);
        node.setObjectiveType(TenderDocumentScoreAttribute.SUBJECTIVE.name());
        node.setChildren(List.of());
        return node;
    }

    private TenderDocumentRuleTreeNode buildPassLeaf(String name, String id, String key) {
        TenderDocumentRuleTreeNode node = new TenderDocumentRuleTreeNode();
        node.setId(id);
        node.setOrder(1);
        node.setKey(key);
        node.setName(name);
        node.setLowest(BigDecimal.ZERO);
        node.setHighest(BigDecimal.ZERO);
        node.setStandard("通过即可");
        node.setParent(false);
        node.setChildren(List.of());
        return node;
    }
}
