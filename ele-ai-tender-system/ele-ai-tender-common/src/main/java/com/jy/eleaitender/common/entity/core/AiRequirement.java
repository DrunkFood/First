package com.jy.eleaitender.common.entity.core;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eleaitender.common.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 业务需求实体
 * 对应表: ai_requirement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_requirement")
@Schema(description = "业务需求")
public class AiRequirement extends BaseEntity {

    @Schema(description = "需求名称")
    private String requirementName;

    @Schema(description = "项目类别")
    private String projectCategory;

    @Schema(description = "项目类型")
    private String projectType;

    @Schema(description = "服务子分类")
    private String serviceSubType;

    @Schema(description = "预算价(元)")
    private BigDecimal budget;

    @Schema(description = "需求描述")
    private String requirementDescription;

    @Schema(description = "匹配模式")
    private String matchMode;

    @Schema(description = "匹配的历史文件ID")
    private Long matchedFileId;

    @Schema(description = "匹配度百分比")
    private BigDecimal matchedSimilarity;

    @Schema(description = "上传的文件ID")
    private Long uploadedFileId;

    @Schema(description = "关联的项目ID")
    private Long projectId;

    @Schema(description = "业务需求内容")
    private String content;

    @Schema(description = "自动保存内容(未提交的草稿)")
    private String autoSaveContent;

    @Schema(description = "自动保存时间")
    private Date autoSaveTime;

    @Schema(description = "状态: IN_PROGRESS/COMPLETED")
    private String status;

    @Schema(description = "完成进度(百分比 0-100)")
    private Integer progress;
}
