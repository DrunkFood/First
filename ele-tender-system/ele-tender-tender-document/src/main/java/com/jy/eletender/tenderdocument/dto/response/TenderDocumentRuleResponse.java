package com.jy.eletender.tenderdocument.dto.response;

import com.jy.eletender.tenderdocument.dto.request.TenderDocumentRuleTreeNode;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class TenderDocumentRuleResponse {

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
     */
    private String reviewMode;

    /**
     * 分值类型枚举名，编制单级统一配置，取值见 TenderDocumentScoreType。
     */
    private String scoreType;

    private BigDecimal totalScore;
    private BigDecimal weightRate;

    /**
     * 返回给前端的树结构，与保存接口使用同一 DTO，便于前端直接回填编辑。
     */
    private List<TenderDocumentRuleTreeNode> treeToPageData = new ArrayList<>();
}
