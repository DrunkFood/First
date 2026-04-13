package com.jy.eleaitender.common.entity.support;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eleaitender.common.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 系统菜单实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sup_menu")
@Schema(description = "系统菜单")
public class SysMenu extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @Schema(description = "父菜单ID，0表示根菜单")
    private Long parentId;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "菜单编码")
    private String menuCode;

    @Schema(description = "菜单类型: 1-菜单, 2-按钮")
    private Integer menuType;

    @Schema(description = "菜单URL")
    private String menuUrl;

    @Schema(description = "权限标识")
    private String permission;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "状态: 0-禁用, 1-启用")
    private Integer status;

    @TableField(exist = false)
    @Schema(description = "子菜单")
    private List<SysMenu> children;
}
