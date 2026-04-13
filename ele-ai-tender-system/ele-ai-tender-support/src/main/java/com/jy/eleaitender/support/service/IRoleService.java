package com.jy.eleaitender.support.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.support.SysRole;

import java.util.List;

/**
 * 角色服务接口
 */
public interface IRoleService {
    
    Page<SysRole> getRolePage(Integer pageNum, Integer pageSize, String roleName, String roleCode, Integer status);
    
    List<SysRole> getAllRoles();
    
    SysRole getRoleById(Long id);
    
    SysRole createRole(SysRole role);
    
    void updateRole(SysRole role);
    
    void deleteRole(Long id);
    
    List<Long> getRoleMenus(Long roleId);
    
    void assignMenus(Long roleId, List<Long> menuIds);
}
