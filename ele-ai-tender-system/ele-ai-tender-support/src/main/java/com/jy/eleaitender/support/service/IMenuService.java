package com.jy.eleaitender.support.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jy.eleaitender.common.entity.support.SysMenu;

import java.util.List;

/**
 * 菜单服务接口
 */
public interface IMenuService extends IService<SysMenu> {

    /**
     * 获取当前用户的菜单树
     *
     * @return 菜单树列表
     */
    List<SysMenu> getCurrentUserMenus();

    /**
     * 获取菜单树（所有菜单）
     *
     * @return 菜单树列表
     */
    List<SysMenu> getMenuTree();

    /**
     * 获取菜单列表（平铺）
     *
     * @return 菜单列表
     */
    List<SysMenu> getMenuList();

    SysMenu getById(Long id);

    SysMenu createMenu(SysMenu menu);

    void updateMenu(SysMenu menu);

    void deleteMenu(Long id);

    /**
     * 根据角色ID获取菜单ID列表
     *
     * @param roleId 角色ID
     * @return 菜单ID列表
     */
    List<Long> getMenuIdsByRoleId(Long roleId);
}
