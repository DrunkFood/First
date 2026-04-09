package com.jy.eletender.tenderdocument.support;

import com.jy.eletender.tenderdocument.dto.request.TenderDocumentRuleSaveRequest;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentRuleTreeNode;
import com.jy.eletender.tenderdocument.enums.TenderDocumentReviewMode;
import com.jy.eletender.tenderdocument.enums.TenderDocumentScoreType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 评审规则校验器。
 * 负责单分类树结构校验与综合评分法跨分类分值约束校验。
 */
@Component
public class TenderDocumentRuleValidator {

    /**
     * 单分类校验：校验树节点结构、叶子节点分值以及分类总分。
     */
    public Map<String, List<String>> validateSingleCategory(TenderDocumentRuleSaveRequest request) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        List<String> categoryErrors = new ArrayList<>();
        boolean scoreMode = isScoreMode(request);

        if (request.getTreeToPageData() == null || request.getTreeToPageData().isEmpty()) {
            categoryErrors.add("至少需要配置一条评审项");
        } else {
            BigDecimal topLevelTotal = BigDecimal.ZERO;
            for (TenderDocumentRuleTreeNode node : request.getTreeToPageData()) {
                categoryErrors.addAll(validateNode(node, 1, scoreMode));
                topLevelTotal = topLevelTotal.add(defaultZero(node.getHighest()));
            }
            if (scoreMode && request.getTotalScore() != null
                    && topLevelTotal.compareTo(request.getTotalScore()) != 0) {
                categoryErrors.add("分类总分必须等于顶层评审项最高分之和");
            }
        }

        if (!categoryErrors.isEmpty()) {
            errors.put(request.getNodeCategory(), categoryErrors);
        }
        return errors;
    }

    /**
     * 跨分类校验：综合评分法下的资信、技术、商务三类必须联合校验。
     */
    public Map<String, List<String>> validateCrossCategory(List<TenderDocumentRuleSaveRequest> requests, String scoreType) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        List<String> crossErrors = new ArrayList<>();
        if (requests == null || requests.isEmpty()) {
            return errors;
        }

        if (!org.springframework.util.StringUtils.hasText(scoreType)) {
            crossErrors.add("综合评分法下scoreType不能为空");
        } else if (TenderDocumentScoreType.ACTUAL.name().equals(scoreType)) {
            BigDecimal totalScore = requests.stream()
                    .map(TenderDocumentRuleSaveRequest::getTotalScore)
                    .map(this::defaultZero)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalScore.compareTo(BigDecimal.valueOf(100)) != 0) {
                crossErrors.add("实际分模式下三个打分环节总分之和必须等于100");
            }
            boolean weightIsHundred = requests.stream()
                    .allMatch(request -> defaultZero(request.getWeightRate()).compareTo(BigDecimal.valueOf(100)) == 0);
            if (!weightIsHundred) {
                crossErrors.add("实际分模式下比例固定为100");
            }
        } else if (TenderDocumentScoreType.WEIGHT.name().equals(scoreType)) {
            boolean totalIsHundred = requests.stream()
                    .allMatch(request -> defaultZero(request.getTotalScore()).compareTo(BigDecimal.valueOf(100)) == 0);
            if (!totalIsHundred) {
                crossErrors.add("权重分模式下每个环节总分必须等于100");
            }
            BigDecimal totalWeight = requests.stream()
                    .map(TenderDocumentRuleSaveRequest::getWeightRate)
                    .map(this::defaultZero)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalWeight.compareTo(BigDecimal.valueOf(100)) != 0) {
                crossErrors.add("权重分模式下比例之和必须等于100");
            }
            boolean weightNotGreaterThanHundred = requests.stream()
                    .allMatch(request -> defaultZero(request.getWeightRate()).compareTo(BigDecimal.valueOf(100)) <= 0);
            if (!weightNotGreaterThanHundred) {
                crossErrors.add("权重分模式下每个环节比例不得高于100");
            }
        }

        if (!crossErrors.isEmpty()) {
            errors.put("CROSS_CATEGORY", crossErrors);
        }
        return errors;
    }

    /**
     * 递归校验树节点。父节点校验汇总关系，叶子节点校验分值区间与主客观属性。
     */
    private List<String> validateNode(TenderDocumentRuleTreeNode node, int level, boolean scoreMode) {
        List<String> errors = new ArrayList<>();
        if (level > 3) {
            errors.add("评审项最多支持3级");
        }
        if (node.getName() == null || node.getName().trim().isEmpty()) {
            errors.add("评审项内容不能为空");
        }

        List<TenderDocumentRuleTreeNode> children = node.getChildren() == null ? List.of() : node.getChildren();
        if (children.isEmpty()) {
            if (defaultZero(node.getHighest()).compareTo(defaultZero(node.getLowest())) < 0
                    || defaultZero(node.getLowest()).compareTo(BigDecimal.ZERO) < 0) {
                errors.add("叶子节点分值区间不合法");
            }
            if (scoreMode && (node.getObjectiveType() == null || node.getObjectiveType().isBlank())) {
                errors.add("打分制叶子节点必须填写主客观分属性");
            }
        } else {
            BigDecimal childLowestTotal = BigDecimal.ZERO;
            BigDecimal childHighestTotal = BigDecimal.ZERO;
            for (TenderDocumentRuleTreeNode child : children) {
                errors.addAll(validateNode(child, level + 1, scoreMode));
                childLowestTotal = childLowestTotal.add(defaultZero(child.getLowest()));
                childHighestTotal = childHighestTotal.add(defaultZero(child.getHighest()));
            }
            if (defaultZero(node.getLowest()).compareTo(childLowestTotal) != 0) {
                errors.add("父节点最低分必须等于直接子节点最低分之和");
            }
            if (defaultZero(node.getHighest()).compareTo(childHighestTotal) != 0) {
                errors.add("父节点最高分必须等于直接子节点最高分之和");
            }
        }
        return errors;
    }

    /**
     * 只有打分制分类才校验总分与跨分类分值规则。
     */
    private boolean isScoreMode(TenderDocumentRuleSaveRequest request) {
        return TenderDocumentReviewMode.SCORE.name().equals(request.getReviewMode());
    }

    /**
     * 所有数值比较前统一空值归零，避免空指针影响校验流程。
     */
    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
