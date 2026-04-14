package com.jy.eleaitender.core.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 消息响应VO
 */
@Data
@Schema(description = "消息")
public class MessageVO {

    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "消息标题")
    private String title;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "消息类型: SYSTEM/AUDIT/DETECTION/WARNING")
    private String messageType;

    @Schema(description = "关联业务ID")
    private Long bizId;

    @Schema(description = "关联业务类型")
    private String bizType;

    @Schema(description = "是否已读: 0-未读 1-已读")
    private Integer isRead;

    @Schema(description = "阅读时间")
    private Date readTime;

    @Schema(description = "创建时间")
    private Date createTime;
}
