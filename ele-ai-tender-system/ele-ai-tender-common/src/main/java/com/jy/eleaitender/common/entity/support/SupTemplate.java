package com.jy.eleaitender.common.entity.support;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eleaitender.common.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 招标文件模板实体
 * 对应表: sup_template
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sup_template")
@Schema(description = "招标文件模板")
public class SupTemplate extends BaseEntity {

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "适用项目类别")
    private String projectCategory;

    @Schema(description = "适用项目类型")
    private String projectType;

    @Schema(description = "模板描述")
    private String description;

    @Schema(description = "模板文件ID(关联file_info)")
    private Long fileId;

    @Schema(description = "模板用途说明")
    private String content;

    @Schema(description = "Word章节结构JSON")
    private String structureDefinition;

    @Schema(description = "版本号")
    private Integer versionNo;

    @Schema(description = "是否默认模板")
    private Integer isDefault;

    @Schema(description = "状态")
    private String status;
}
