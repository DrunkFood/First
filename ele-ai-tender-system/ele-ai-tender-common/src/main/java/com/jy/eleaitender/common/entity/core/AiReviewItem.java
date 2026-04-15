package com.jy.eleaitender.common.entity.core;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eleaitender.common.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

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

    @Schema(description = "评审类型: CONFORMITY/TECHNICAL/QUALIFICATION/COMMERCIAL")
    private String reviewType;

    @Schema(description = "分值(评审分值)")
    private BigDecimal score;

    @Schema(description = "满分值(商务评审专用)")
    private BigDecimal maxScore;

    @Schema(description = "权重(百分比)")
    private BigDecimal weight;

    @Schema(description = "客观/主观: OBJECTIVE/SUBJECTIVE")
    private String subjectivity;

    @Schema(description = "是否必审项: 0-否 1-是")
    private Integer isRequired;
}
