package com.jy.eleaitender.ai.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * AI对话请求DTO
 */
@Data
@Schema(description = "AI对话请求")
public class ChatRequest {

    @NotBlank(message = "消息内容不能为空")
    @Schema(description = "用户消息")
    private String message;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "上下文内容（选中的文本等）")
    private String context;

    @Schema(description = "是否为选中文本替换模式，为true时AI需标记可直接替换正文范围")
    private Boolean replaceMode;

    @Schema(description = "对话ID, 发起聊天时前端随机生成的UUID")
    private String conversationId;

    @Schema(description = "对话历史（最近N轮）")
    private List<ChatMessage> history;

    @Data
    @Schema(description = "历史对话消息")
    public static class ChatMessage {
        @Schema(description = "角色: user/assistant")
        private String role;
        @Schema(description = "消息内容")
        private String content;
    }
}
