package com.jy.eleaitender.common.entity.core;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eleaitender.common.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 招标文件模板实体
 * 对应表: ai_template
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_template")
@Schema(description = "招标文件模板")
public class AiTemplate extends BaseEntity {

    @Schema(description = "模板编码")
    private String templateCode;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "适用项目类别")
    private String projectCategory;

    @Schema(description = "适用项目类型")
    private String projectType;

    @Schema(description = "模板内容(Markdown)")
    private String content;

    @Schema(description = "模板结构定义JSON")
    private String structureDefinition;

    @Schema(description = "版本号")
    private Integer versionNo;

    @Schema(description = "是否默认模板")
    private Integer isDefault;

    @Schema(description = "状态")
    private String status;
}
