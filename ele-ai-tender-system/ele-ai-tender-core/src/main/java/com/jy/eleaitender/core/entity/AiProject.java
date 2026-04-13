package com.jy.eleaitender.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eleaitender.common.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * AI编制项目实体
 * 对应表: ai_project
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_project")
@Schema(description = "AI编制项目")
public class AiProject extends BaseEntity {

    @Schema(description = "项目编号")
    private String projectCode;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "项目类别")
    private String projectCategory;

    @Schema(description = "项目类型")
    private String projectType;

    @Schema(description = "服务子类型")
    private String serviceSubType;

    @Schema(description = "预算金额(万元)")
    private BigDecimal budget;

    @Schema(description = "评审类型")
    private String reviewType;

    @Schema(description = "项目状态")
    private String status;

    @Schema(description = "使用的模板ID")
    private Long templateId;

    @Schema(description = "关联的业务需求ID")
    private Long requirementId;

    @Schema(description = "需求来源")
    private String requirementSource;

    @Schema(description = "招标需求内容")
    private String requirementContent;
}
