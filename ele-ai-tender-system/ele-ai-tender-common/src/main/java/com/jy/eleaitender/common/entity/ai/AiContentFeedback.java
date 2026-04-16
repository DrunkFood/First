package com.jy.eleaitender.common.entity.ai;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eleaitender.common.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI内容反馈实体
 * 对应表: ai_content_feedback
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_content_feedback")
@Schema(description = "AI内容反馈")
public class AiContentFeedback extends BaseEntity {

    @Schema(description = "关联AI任务ID, 聊天反馈无任务时为NULL")
    private Long taskId;

    @Schema(description = "反馈类型: LIKE/DISLIKE")
    private String feedbackType;

    @Schema(description = "反馈场景: GENERATION_CONTENT/CHAT_MESSAGE")
    private String feedbackScene;

    @Schema(description = "聊天消息标识, 生成内容反馈时为空字符串")
    private String chatMessageId;

    @Schema(description = "被反馈的AI聊天消息内容摘要(最多200字)")
    private String chatContent;

    @Schema(description = "不满意原因(DISLIKE时可选填)")
    private String reason;
}
