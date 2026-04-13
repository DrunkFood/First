package com.jy.eleaitender.common.entity.support;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eleaitender.common.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 插件版本实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sup_plugin_version")
@Schema(description = "插件版本")
public class SysPluginVersion extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "插件名称")
    private String pluginName;

    @Schema(description = "版本号")
    private String versionNumber;

    @Schema(description = "适配的主系统版本")
    private String compatibleMainVersion;

    @Schema(description = "文件ID")
    private Long fileId;

    @Schema(description = "发布说明")
    private String releaseNotes;

    @Schema(description = "状态: 0-草稿, 1-已发布, 2-已下线")
    private Integer status;
}
