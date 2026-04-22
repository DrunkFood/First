package com.jy.eleaitender.common.entity.core;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eleaitender.common.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目版本实体
 * 对应表: tb_project_version
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_project_version")
@Schema(description = "项目版本")
public class TbProjectVersion extends BaseEntity {

    @Schema(description = "项目ID")
    private Long projectId;

    @Schema(description = "版本号")
    private Integer versionNo;

    @Schema(description = "内容快照JSON")
    private String contentSnapshot;

    @Schema(description = "变更说明")
    private String changeDescription;
}
