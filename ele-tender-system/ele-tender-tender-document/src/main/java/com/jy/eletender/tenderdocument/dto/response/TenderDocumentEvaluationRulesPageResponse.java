package com.jy.eletender.tenderdocument.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class TenderDocumentEvaluationRulesPageResponse {

    /**
     * 评标办法枚举名，取值见 TenderDocumentEvalMethod。
     */
    private String evalMethod;

    /**
     * 当前编制单可操作的标段列表。
     * 公开类通常是项目下多个标段，邀请类通常只有当前标段。
     */
    private List<Map<String, Object>> tenderList = new ArrayList<>();

    /**
     * 页面可切换的节点分类，取值见 TenderDocumentRuleCategory。
     */
    private List<String> nodeCategoryList = new ArrayList<>();
}
