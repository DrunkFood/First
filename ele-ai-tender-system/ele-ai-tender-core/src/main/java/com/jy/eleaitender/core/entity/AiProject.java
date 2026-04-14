package com.jy.eleaitender.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eleaitender.common.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

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

    @Schema(description = "项目基本情况描述")
    private String projectDescription;

    @Schema(description = "招标单位")
    private String tenderUnit;

    @Schema(description = "项目地点")
    private String projectLocation;

    @Schema(description = "联系人")
    private String contactPerson;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "预期发布时间")
    private Date expectedPublishTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "评审类型: INTELLIGENT/MANUAL")
    private String reviewType;

    @Schema(description = "项目状态")
    private String status;

    @Schema(description = "当前编制阶段: 1-5")
    private Integer currentPhase;

    @Schema(description = "完成进度(百分比 0-100)")
    private Integer progress;

    @Schema(description = "使用的模板ID")
    private Long templateId;

    @Schema(description = "关联的业务需求ID")
    private Long requirementId;

    @Schema(description = "需求来源: REFERENCE/SYSTEM_GENERATE")
    private String requirementSource;

    @Schema(description = "招标需求内容")
    private String requirementContent;

    @Schema(description = "生成的招标文件ID(关联file_info)")
    private Long generatedFileId;
}
