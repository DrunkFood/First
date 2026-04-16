package com.jy.eleaitender.common.entity.ai;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eleaitender.common.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI响应记录实体
 * 对应表: ai_response_log
 * 记录每次AI模型调用的详细信息，包括请求内容、响应内容、token消耗等
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_response_log")
@Schema(description = "AI响应记录")
public class AiResponseLog extends BaseEntity {

    @Schema(description = "模型名称")
    private String model;

    @Schema(description = "对话角色: GENERATION/OPTIMIZATION/DETECTION/CHAT")
    private String role;

    @Schema(description = "对话内容messages(JSON)")
    private String messages;

    @Schema(description = "AI响应内容")
    private String content;

    @Schema(description = "完成原因: stop/length/tool_calls等")
    private String finishReason;

    @Schema(description = "包含历史问题的总tokens大小")
    private Integer promptTokens;

    @Schema(description = "回答的tokens大小")
    private Integer completionTokens;

    @Schema(description = "本次交互计费的tokens大小")
    private Integer totalTokens;

    @Schema(description = "关联AI任务ID(ai_task.id), 对话类调用为NULL")
    private Long taskId;

    @Schema(description = "对话ID, 发起AI聊天时随机生成的UUID")
    private String conversationId;
}
