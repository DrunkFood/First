package com.jy.eleaitender.core.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交反馈请求
 */
@Data
@Schema(description = "提交反馈请求")
public class FeedbackSubmitRequest {

    @NotNull(message = "反馈类型不能为空")
    @Schema(description = "反馈类型: LIKE/DISLIKE")
    private String feedbackType;

    @NotNull(message = "反馈场景不能为空")
    @Schema(description = "反馈场景: GENERATION_CONTENT/CHAT_MESSAGE")
    private String feedbackScene;

    @Schema(description = "关联AI任务ID(生成内容反馈必填)")
    private Long taskId;

    @Schema(description = "关联项目ID")
    private Long projectId;

    @Schema(description = "聊天消息标识(聊天反馈时必填)")
    private String chatMessageId;

    @Schema(description = "被反馈的AI聊天消息内容摘要(最多200字)")
    private String chatContent;

    @Schema(description = "不满意原因(DISLIKE时可选填)")
    private String reason;
}
