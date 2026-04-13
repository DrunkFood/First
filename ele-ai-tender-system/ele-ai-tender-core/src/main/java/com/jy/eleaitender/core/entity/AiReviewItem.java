package com.jy.eleaitender.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eleaitender.common.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评审项实体
 * 对应表: ai_review_item
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_review_item")
@Schema(description = "评审项")
public class AiReviewItem extends BaseEntity {

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "父级ID")
    private Long parentId;

    @Schema(description = "层级")
    private Integer level;

    @Schema(description = "评审项名称")
    private String itemName;

    @Schema(description = "评审项内容")
    private String itemContent;

    @Schema(description = "排序号")
    private Integer sortOrder;
}
