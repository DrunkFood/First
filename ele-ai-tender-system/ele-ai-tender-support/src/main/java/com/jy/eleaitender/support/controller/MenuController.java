package com.jy.eleaitender.support.controller;

import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.entity.support.SysMenu;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.support.service.IMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单控制器
 */
@RestController
@RequestMapping("/api/menus")
@Tag(name = "菜单管理")
public class MenuController {

    @Autowired
    private IMenuService menuService;

    /**
     * 获取菜单树（当前用户）
     */
    @GetMapping("/tree")
    @RequireLogin
    @Operation(summary = "获取菜单树")
    public Result<List<SysMenu>> getMenuTree() {
        return Result.success(menuService.getCurrentUserMenus());
    }

    /**
     * 获取菜单列表（平铺）
     */
    @GetMapping
    @RequireLogin
    @Operation(summary = "获取菜单列表")
    public Result<List<SysMenu>> getMenuList() {
        return Result.success(menuService.getMenuList());
    }

    @GetMapping("/{id}")
    @RequireLogin
    @Operation(summary = "获取菜单详情")
    public Result<SysMenu> getById(@PathVariable Long id) {
        return Result.success(menuService.getById(id));
    }

    @PostMapping
    @RequireLogin
    @Operation(summary = "创建菜单")
    public Result<SysMenu> create(@RequestBody SysMenu menu) {
        return Result.success(menuService.createMenu(menu));
    }

    @PutMapping("/{id}")
    @RequireLogin
    @Operation(summary = "更新菜单")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysMenu menu) {
        menu.setId(id);
        menuService.updateMenu(menu);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireLogin
    @Operation(summary = "删除菜单")
    public Result<Void> delete(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return Result.success();
    }
}
