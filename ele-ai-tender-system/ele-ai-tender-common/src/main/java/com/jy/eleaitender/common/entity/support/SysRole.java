package com.jy.eleaitender.common.entity.support;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eleaitender.common.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统角色实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sup_role")
@Schema(description = "系统角色")
public class SysRole extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "状态: 0-禁用, 1-启用")
    private Integer status;
}
