package com.jy.eleaitender.support.service.impl;

import com.jy.eleaitender.common.entity.support.SysMenu;
import com.jy.eleaitender.support.mapper.SysMenuMapper;
import com.jy.eleaitender.common.security.LoginUser;
import com.jy.eleaitender.common.security.SecurityContextHolder;
import com.jy.eleaitender.support.service.IMenuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单服务实现
 */
@Slf4j
@Service
public class MenuServiceImpl implements IMenuService {

    @Autowired
    private SysMenuMapper menuMapper;

    @Override
    public List<SysMenu> getCurrentUserMenus() {
        LoginUser loginUser = SecurityContextHolder.getLoginUser();
        if (loginUser == null || loginUser.getUserId() == null) {
            return new ArrayList<>();
        }
        
        List<SysMenu> menus = menuMapper.selectMenusByUserId(loginUser.getUserId());
        return buildMenuTree(menus);
    }

    @Override
    public List<SysMenu> getMenuTree() {
        List<SysMenu> menus = menuMapper.selectList(null);
        return buildMenuTree(menus);
    }

    @Override
    public List<SysMenu> getMenuList() {
        return menuMapper.selectList(null);
    }

    @Override
    public SysMenu getById(Long id) {
        return menuMapper.selectById(id);
    }

    @Override
    public SysMenu createMenu(SysMenu menu) {
        menuMapper.insert(menu);
        return menu;
    }

    @Override
    public void updateMenu(SysMenu menu) {
        menuMapper.updateById(menu);
    }

    @Override
    public void deleteMenu(Long id) {
        menuMapper.deleteById(id);
    }

    @Override
    public List<Long> getMenuIdsByRoleId(Long roleId) {
        return menuMapper.selectMenuIdsByRoleId(roleId);
    }

    /**
     * 构建菜单树
     *
     * @param menus 菜单列表
     * @return 菜单树
     */
    private List<SysMenu> buildMenuTree(List<SysMenu> menus) {
        // 按 parentId 分组
        var menuMap = menus.stream()
                .filter(menu -> menu.getParentId() != null)
                .collect(Collectors.groupingBy(SysMenu::getParentId));

        // 获取根菜单（parentId = 0）
        List<SysMenu> rootMenus = menuMap.getOrDefault(0L, new ArrayList<>());
        
        // 递归设置子菜单
        for (SysMenu rootMenu : rootMenus) {
            setChildren(rootMenu, menuMap);
        }
        rootMenus.sort((a, b) -> {
            int orderA = a.getSortOrder() != null ? a.getSortOrder() : 0;
            int orderB = b.getSortOrder() != null ? b.getSortOrder() : 0;
            return orderA - orderB;
        });

        return rootMenus;
    }

    /**
     * 递归设置子菜单
     *
     * @param parentMenu 父菜单
     * @param menuMap    菜单分组
     */
    private void setChildren(SysMenu parentMenu, java.util.Map<Long, List<SysMenu>> menuMap) {
        List<SysMenu> children = menuMap.getOrDefault(parentMenu.getId(), new ArrayList<>());
        // 排序
        children.sort((a, b) -> {
            int orderA = a.getSortOrder() != null ? a.getSortOrder() : 0;
            int orderB = b.getSortOrder() != null ? b.getSortOrder() : 0;
            return orderA - orderB;
        });
        
        for (SysMenu child : children) {
            setChildren(child, menuMap);
        }
        parentMenu.setChildren(children);
    }
}
