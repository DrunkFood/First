package com.jy.eleaitender.support.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.support.SysRole;
import com.jy.eleaitender.common.entity.support.SysRoleMenu;
import com.jy.eleaitender.support.mapper.SysRoleMapper;
import com.jy.eleaitender.support.mapper.SysRoleMenuMapper;
import com.jy.eleaitender.support.service.IRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

/**
 * 角色服务实现
 */
@Service
public class RoleServiceImpl implements IRoleService {

    @Autowired
    private SysRoleMapper roleMapper;
    
    @Autowired
    private SysRoleMenuMapper roleMenuMapper;

    @Override
    public Page<SysRole> getRolePage(Integer pageNum, Integer pageSize, String roleName, String roleCode, Integer status) {
        Page<SysRole> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(roleName)) {
            wrapper.like(SysRole::getRoleName, roleName);
        }
        if (StringUtils.hasText(roleCode)) {
            wrapper.like(SysRole::getRoleCode, roleCode);
        }
        if (status != null) {
            wrapper.eq(SysRole::getStatus, status);
        }
        
        wrapper.eq(SysRole::getIsDelete, 0);
        wrapper.orderByDesc(SysRole::getCreateTime);
        
        return roleMapper.selectPage(page, wrapper);
    }

    @Override
    public List<SysRole> getAllRoles() {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getStatus, 1);
        wrapper.eq(SysRole::getIsDelete, 0);
        return roleMapper.selectList(wrapper);
    }

    @Override
    public SysRole getRoleById(Long id) {
        return roleMapper.selectById(id);
    }

    @Override
    public SysRole createRole(SysRole role) {
        role.setStatus(1);
        roleMapper.insert(role);
        return role;
    }

    @Override
    public void updateRole(SysRole role) {
        roleMapper.updateById(role);
    }

    @Override
    public void deleteRole(Long id) {
        roleMapper.deleteById(id);
    }

    @Override
    public List<Long> getRoleMenus(Long roleId) {
        LambdaQueryWrapper<SysRoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleMenu::getRoleId, roleId);
        wrapper.eq(SysRoleMenu::getIsDelete, 0);
        List<SysRoleMenu> roleMenus = roleMenuMapper.selectList(wrapper);
        return roleMenus.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        // 物理删除原有权限，避免逻辑删除记录命中唯一索引(role_id, menu_id)
        roleMenuMapper.deleteByRoleIdPhysical(roleId);
        
        // 插入新权限
        if (menuIds != null && !menuIds.isEmpty()) {
            // 去重并过滤空值，避免重复插入导致唯一索引冲突
            List<Long> uniqueMenuIds = menuIds.stream()
                    .filter(id -> id != null && id > 0)
                    .collect(Collectors.collectingAndThen(
                            Collectors.toCollection(LinkedHashSet::new),
                            List::copyOf
                    ));
            for (Long menuId : uniqueMenuIds) {
                SysRoleMenu roleMenu = new SysRoleMenu();
                roleMenu.setRoleId(roleId);
                roleMenu.setMenuId(menuId);
                roleMenuMapper.insert(roleMenu);
            }
        }
    }
}
