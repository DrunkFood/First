package com.jy.eleaitender.ai.service.impl;

import com.jy.eleaitender.ai.dto.request.ChatRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiChatServiceImplTest {

    @Test
    void buildUserPromptShouldRequireReplaceableContentMarkersWhenReplaceModeEnabled() {
        ChatRequest request = new ChatRequest();
        request.setContext("原始正文内容");
        request.setMarkdownContext("""
                # 招标需求
                - 原始正文内容
                """);
        request.setMessage("帮我优化这段内容");
        request.setReplaceMode(true);

        String prompt = new AiChatServiceImpl().buildUserPrompt(request);

        assertThat(prompt)
                .contains("用户选中的原文：\n原始正文内容")
                .contains("完整 Markdown 上下文：\n```markdown\n# 招标需求\n- 原始正文内容\n```")
                .contains("请只修改“用户选中的原文”")
                .contains("保持它在完整 Markdown 上下文中的结构、标题层级、编号方式、列表样式和表格列数")
                .contains("如果用户选中的原文不包含选区前的小标题、编号或冒号前缀，不要在可替换正文中补入这些前缀")
                .contains("【可替换正文开始】")
                .contains("【可替换正文结束】")
                .contains("标记内只放可直接替换到编辑器正文中的内容")
                .contains("每个方案都必须单独使用一组固定标记包裹")
                .endsWith("帮我优化这段内容");
    }

    @Test
    void buildUserPromptShouldNotRequireReplaceableContentMarkersWhenReplaceModeDisabled() {
        ChatRequest request = new ChatRequest();
        request.setContext("完整需求正文");
        request.setMessage("帮我分析一下");
        request.setReplaceMode(false);

        String prompt = new AiChatServiceImpl().buildUserPrompt(request);

        assertThat(prompt)
                .contains("参考上下文：\n完整需求正文")
                .doesNotContain("【可替换正文开始】")
                .doesNotContain("【可替换正文结束】")
                .endsWith("帮我分析一下");
    }
}
