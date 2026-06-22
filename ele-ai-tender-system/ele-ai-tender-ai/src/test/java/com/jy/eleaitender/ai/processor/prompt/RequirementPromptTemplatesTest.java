package com.jy.eleaitender.ai.processor.prompt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequirementPromptTemplatesTest {

    @Test
    void requirementPromptsShouldIncludeDocumentBusinessRules() {
        assertThat(SystemPromptTemplates.REQUIREMENT_OUTLINE_GENERATE)
                .contains("经验丰富的招标采购需求编制专家")
                .contains("中华人民共和国招标投标法")
                .contains("政府采购需求管理办法")
                .contains("项目招标（采购）需求书")
                .contains("施工组织设计大纲")
                .contains("材料品牌推荐表")
                .contains("质量保修责任书框架")
                .contains("技术规格参数表")
                .contains("样品/检测要求")
                .contains("包装运输标准")
                .contains("团队配置要求")
                .contains("服务水平协议（SLA）")
                .contains("故障响应时间小于30分钟")
                .contains("成果交付物清单")
                .contains("预算金额")
                .contains("资格门槛")
                .contains("价格分权重")
                .contains("货物类通常30%-40%")
                .contains("服务类通常10%-20%")
                .contains("ISO9001")
                .contains("等保三级")
                .contains("建筑装饰二级")
                .contains("不可过高导致排斥潜在供应商")
                .contains("现状痛点")
                .contains("核心目标")
                .contains("隐性需求")
                .contains("项目概况与建设目标")
                .contains("采购范围与内容")
                .contains("质量、安全、技术规格、物理特性、效率")
                .contains("国家相关标准、行业标准、地方标准")
                .contains("商务要求")
                .contains("投标人资格条件")
                .contains("验收标准")
                .contains("其他要求");

        assertThat(SystemPromptTemplates.REQUIREMENT_CHAPTER_GENERATE)
                .contains("经验丰富的招标采购需求编制专家")
                .contains("项目招标（采购）需求书")
                .contains("质量、安全、技术规格、物理特性、效率")
                .contains("国家相关标准、行业标准、地方标准")
                .contains("投标人资格条件")
                .contains("验收标准")
                .contains("同等或优于")
                .contains("不得指定唯一品牌");

        assertThat(SystemPromptTemplates.REQUIREMENT_REVIEW)
                .contains("项目招标（采购）需求书")
                .contains("项目概况与建设目标")
                .contains("采购范围与内容")
                .contains("投标人资格条件")
                .contains("国家相关标准、行业标准、地方标准");

        assertThat(UserPromptTemplates.REQUIREMENT_OUTLINE_GENERATE_USER)
                .contains("项目类型")
                .contains("项目预算金额")
                .contains("项目原始描述");
    }
}
