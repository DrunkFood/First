package com.jy.eleaitender.ai.service.impl;

import com.jy.eleaitender.ai.dto.request.ChatRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiChatServiceImplTest {

    @Test
    void buildUserPromptShouldRequireReplaceableContentMarkersWhenReplaceModeEnabled() {
        ChatRequest request = new ChatRequest();
        request.setContext("原始正文内容");
        request.setMessage("帮我优化这段内容");
        request.setReplaceMode(true);

        String prompt = new AiChatServiceImpl().buildUserPrompt(request);

        assertThat(prompt)
                .contains("参考上下文：\n原始正文内容")
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
