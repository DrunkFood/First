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
}
