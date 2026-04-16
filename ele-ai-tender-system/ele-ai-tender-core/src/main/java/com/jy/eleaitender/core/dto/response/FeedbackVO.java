package com.jy.eleaitender.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 反馈信息响应
 */
@Data
@Schema(description = "反馈信息")
public class FeedbackVO {

    @Schema(description = "反馈ID")
    private Long id;

    @Schema(description = "反馈类型: LIKE/DISLIKE")
    private String feedbackType;

    @Schema(description = "反馈场景: GENERATION_CONTENT/CHAT_MESSAGE")
    private String feedbackScene;

    @Schema(description = "不满意原因")
    private String reason;
}
