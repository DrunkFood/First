package com.jy.eleaitender.support.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.logging.OperationLog;
import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.entity.support.SysRole;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.support.service.IRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 角色控制器
 */
@RestController
@RequestMapping("/api/roles")
@Tag(name = "角色管理")
public class RoleController {

    @Autowired
    private IRoleService roleService;

    @GetMapping
    @RequireLogin
    @Operation(summary = "分页查询角色")
    public Result<Page<SysRole>> getList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) Integer status) {
        return Result.success(roleService.getRolePage(pageNum, pageSize, roleName, roleCode, status));
    }

    @GetMapping("/all")
    @RequireLogin
    @Operation(summary = "获取所有角色")
    public Result<List<SysRole>> getAll() {
        return Result.success(roleService.getAllRoles());
    }

    @GetMapping("/{id}")
    @RequireLogin
    @Operation(summary = "获取角色详情")
    public Result<SysRole> getById(@PathVariable Long id) {
        return Result.success(roleService.getRoleById(id));
    }

    @PostMapping
    @RequireLogin
    @Operation(summary = "创建角色")
    @OperationLog("创建角色")
    public Result<SysRole> create(@RequestBody SysRole role) {
        return Result.success(roleService.createRole(role));
    }

    @PutMapping("/{id}")
    @RequireLogin
    @Operation(summary = "更新角色")
    @OperationLog("更新角色")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysRole role) {
        role.setId(id);
        roleService.updateRole(role);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireLogin
    @Operation(summary = "删除角色")
    @OperationLog("删除角色")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success();
    }

    @GetMapping("/{id}/menus")
    @RequireLogin
    @Operation(summary = "获取角色的菜单权限")
    public Result<List<Long>> getRoleMenus(@PathVariable Long id) {
        return Result.success(roleService.getRoleMenus(id));
    }

    @PutMapping("/{id}/menus")
    @RequireLogin
    @Operation(summary = "分配角色菜单权限")
    @OperationLog("分配角色权限")
    public Result<Void> assignMenus(@PathVariable Long id, @RequestBody Map<String, List<Long>> params) {
        List<Long> menuIds = params.get("menuIds");
        roleService.assignMenus(id, menuIds);
        return Result.success();
    }
}
