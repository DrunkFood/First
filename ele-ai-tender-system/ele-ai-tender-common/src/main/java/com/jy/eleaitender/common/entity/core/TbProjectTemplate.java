package com.jy.eleaitender.common.entity.core;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eleaitender.common.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目模板(只读快照)实体
 * 对应表: tb_project_template
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_project_template")
@Schema(description = "项目模板(只读快照)")
public class TbProjectTemplate extends BaseEntity {

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "源模板ID(sup_template.id)")
    private Long templateId;

    @Schema(description = "模板编码(快照)")
    private String templateCode;

    @Schema(description = "模板名称(快照)")
    private String templateName;

    @Schema(description = "适用项目类别(快照)")
    private String projectCategory;

    @Schema(description = "适用项目类型(快照)")
    private String projectType;

    @Schema(description = "模板文件ID(快照)")
    private Long fileId;

    @Schema(description = "模板用途说明(快照)")
    private String content;

    @Schema(description = "Word章节结构JSON(快照)")
    private String structureDefinition;

    @Schema(description = "模板版本号(快照)")
    private Integer versionNo;
}
