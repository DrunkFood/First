package com.jy.eleaitender.ai.processor.prompt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewItemPromptTemplatesTest {

    @Test
    void reviewItemPromptShouldEnforceExactOneHundredPointScoring() {
        assertThat(SystemPromptTemplates.REVIEW_ITEM_GENERATE)
                .contains("符合性审查不计入100分")
                .contains("叶子节点score合计必须正好等于100分")
                .contains("输出前必须自行核算");

        assertThat(UserPromptTemplates.REVIEW_ITEM_GENERATE_USER_WITH_CONFIG)
                .contains("硬性计分规则")
                .contains("非符合性评审类型的叶子节点score合计必须正好等于100分")
                .contains("不要输出总分不等于100分的JSON");
    }

    @Test
    void reviewItemPromptShouldIncludeDocumentScoringRules() {
        assertThat(SystemPromptTemplates.REVIEW_ITEM_GENERATE)
                .contains("招标采购评审标准设计专家")
                .contains("中华人民共和国招标投标法")
                .contains("政府采购需求管理办法")
                .contains("综合评分法评审标准表")
                .contains("价格分")
                .contains("技术分")
                .contains("商务分")
                .contains("客观分")
                .contains("主观分")
                .contains("优得3-5分")
                .contains("清晰的分档定义")
                .contains("特定品牌")
                .contains("特定厂商");

        assertThat(UserPromptTemplates.REVIEW_ITEM_GENERATE_USER_WITH_CONFIG)
                .contains("招标（采购）需求书")
                .contains("项目类型、预算、技术/服务要求")
                .contains("模板")
                .contains("综合评分法评审标准表");
    }
}
