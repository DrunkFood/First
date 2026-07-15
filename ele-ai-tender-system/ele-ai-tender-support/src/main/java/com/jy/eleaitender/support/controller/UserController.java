package com.jy.eleaitender.support.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.support.SysUser;
import com.jy.eleaitender.common.logging.OperationLog;
import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.support.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理")
public class UserController {

    @Autowired
    private IUserService userService;

    @GetMapping
    @RequireLogin
    @Operation(summary = "分页查询用户")
    public Result<Page<SysUser>> getList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) Integer status) {
        return Result.success(userService.getUserPage(pageNum, pageSize, username, realName, status));
    }

    @GetMapping("/{id}")
    @RequireLogin
    @Operation(summary = "获取用户详情")
    public Result<SysUser> getById(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }

    @PostMapping
    @RequireLogin
    @Operation(summary = "创建用户")
    @OperationLog("创建用户")
    public Result<SysUser> create(@RequestBody SysUser user) {
        return Result.success(userService.createUser(user));
    }

    @PutMapping("/{id}")
    @RequireLogin
    @Operation(summary = "更新用户")
    @OperationLog("更新用户")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysUser user) {
        user.setId(id);
        userService.updateUser(user);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireLogin
    @Operation(summary = "删除用户")
    @OperationLog("删除用户")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    @PostMapping("/{id}/reset-password")
    @RequireLogin
    @Operation(summary = "重置密码")
    @OperationLog("重置用户密码")
    public Result<Void> resetPassword(
            @PathVariable Long id,
            @RequestParam(required = false) String newPassword,
            @RequestBody(required = false) Map<String, String> body) {
        if (newPassword == null && body != null) {
            newPassword = body.get("newPassword");
        }
        userService.resetPassword(id, newPassword);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @RequireLogin
    @Operation(summary = "启用/禁用用户")
    @OperationLog("变更用户状态")
    public Result<Void> changeStatus(
            @PathVariable Long id,
            @RequestParam(required = false) Integer status,
            @RequestBody(required = false) Map<String, Integer> body) {
        if (status == null && body != null) {
            status = body.get("status");
        }
        userService.changeStatus(id, status);
        return Result.success();
    }
}
