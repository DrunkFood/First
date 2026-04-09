package com.jy.eletender.common.entity.support;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eletender.common.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 主系统版本实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sup_main_version")
@Schema(description = "主系统版本")
public class SysMainVersion extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主系统名称")
    private String systemName;

    @Schema(description = "版本号")
    private String versionNumber;

    @Schema(description = "文件ID")
    private Long fileId;

    @Schema(description = "发布说明")
    private String releaseNotes;

    @Schema(description = "状态: 0-草稿, 1-已发布, 2-已下线")
    private Integer status;
}
