package com.jy.eletender.support.controller;

import com.jy.eletender.common.response.Result;
import com.jy.eletender.common.dto.request.UserLoginRequest;
import com.jy.eletender.common.dto.response.UserLoginResponse;
import com.jy.eletender.common.entity.support.SysMenu;
import com.jy.eletender.common.security.SecurityContextHolder;
import com.jy.eletender.common.security.annotation.RequireLogin;
import com.jy.eletender.support.service.IAuthService;
import com.jy.eletender.support.service.IMenuService;
import com.jy.eletender.support.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证管理")
public class AuthController {

    @Autowired
    private IAuthService authService;

    @Autowired
    private IMenuService menuService;

    @Autowired
    private IUserService userService;

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<UserLoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
        UserLoginResponse response = authService.login(request);
        return Result.success(response);
    }

    @PostMapping("/logout")
    @RequireLogin
    @Operation(summary = "用户登出")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    @GetMapping("/userinfo")
    @RequireLogin
    @Operation(summary = "获取当前用户信息")
    public Result<UserLoginResponse.UserInfo> getUserInfo() {
        UserLoginResponse.UserInfo userInfo = authService.getUserInfo();
        return Result.success(userInfo);
    }

    @GetMapping("/info")
    @RequireLogin
    @Operation(summary = "获取当前用户信息（别名）")
    public Result<UserLoginResponse.UserInfo> getUserInfoAlias() {
        return getUserInfo();
    }

    @GetMapping("/menus")
    @RequireLogin
    @Operation(summary = "获取当前用户菜单")
    public Result<List<SysMenu>> getUserMenus() {
        return Result.success(menuService.getCurrentUserMenus());
    }

    @PostMapping("/change-password")
    @RequireLogin
    @Operation(summary = "修改当前用户密码")
    public Result<Void> changePassword(@RequestBody Map<String, String> params) {
        Long userId = SecurityContextHolder.getLoginUser().getUserId();
        userService.changePassword(userId, params.get("oldPassword"), params.get("newPassword"));
        return Result.success();
    }
}
