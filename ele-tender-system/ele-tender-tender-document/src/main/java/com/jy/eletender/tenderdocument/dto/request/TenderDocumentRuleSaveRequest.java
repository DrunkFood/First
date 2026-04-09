package com.jy.eletender.tenderdocument.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class TenderDocumentRuleSaveRequest {

    private String tenderId;
    private String tenderName;

    /**
     * 评标办法枚举名，取值见 TenderDocumentEvalMethod。
     */
    private String evalMethod;

    /**
     * 节点分类枚举名，取值见 TenderDocumentRuleCategory。
     */
    private String nodeCategory;

    /**
     * 评审方式枚举名，取值见 TenderDocumentReviewMode。
     * 当前由后端按 evalMethod + nodeCategory 固定推导，前端可不传。
     */
    private String reviewMode;

    /**
     * 当前分类总分。
     * 打分制下需要等于顶层节点 highest 之和；通过制下会被后端忽略。
     */
    private BigDecimal totalScore;

    /**
     * 当前分类权重比例。
     * 仅综合评分法打分节点使用，实际分模式通常固定为 100；通过制下会被后端忽略。
     */
    private BigDecimal weightRate;

    /**
     * 前端树形结构原样提交，后端会转换为平表的 parentId + level + sortNo 结构落库。
     */
    private List<TenderDocumentRuleTreeNode> treeToPageData = new ArrayList<>();
}
