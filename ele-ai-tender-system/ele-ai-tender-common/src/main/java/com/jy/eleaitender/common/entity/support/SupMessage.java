package com.jy.eleaitender.common.entity.support;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eleaitender.common.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 系统消息实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sup_message")
@Schema(description = "系统消息")
public class SupMessage extends BaseEntity {

    @Schema(description = "目标用户ID")
    private Long userId;

    @Schema(description = "消息标题")
    private String title;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "消息类型: SYSTEM/AUDIT/DETECTION/WARNING")
    private String messageType;

    @Schema(description = "关联业务ID")
    private Long bizId;

    @Schema(description = "关联业务类型: PROJECT/REQUIREMENT/DETECTION/TEMPLATE")
    private String bizType;

    @Schema(description = "是否已读: 0-未读, 1-已读")
    private Integer isRead;

    @Schema(description = "阅读时间")
    private Date readTime;
}
