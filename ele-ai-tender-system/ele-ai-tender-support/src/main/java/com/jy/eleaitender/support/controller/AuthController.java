package com.jy.eleaitender.support.controller;

import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.dto.request.PhoneLoginRequest;
import com.jy.eleaitender.common.dto.request.SendSmsCodeRequest;
import com.jy.eleaitender.common.dto.request.UserLoginRequest;
import com.jy.eleaitender.common.dto.response.UserLoginResponse;
import com.jy.eleaitender.common.entity.support.SysMenu;
import com.jy.eleaitender.common.security.SecurityContextHolder;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.common.util.RsaKeyUtil;
import com.jy.eleaitender.support.service.IAuthService;
import com.jy.eleaitender.support.service.IMenuService;
import com.jy.eleaitender.support.service.ISmsService;
import com.jy.eleaitender.support.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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

    @Autowired
    private ISmsService smsService;

    @GetMapping("/public-key")
    @Operation(summary = "获取RSA公钥（用于密码加密传输）")
    public Result<RsaKeyUtil.PublicKeyInfo> getPublicKey() {
        return Result.success(RsaKeyUtil.getPublicKeyInfo());
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<UserLoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
        UserLoginResponse response = authService.login(request);
        return Result.success(response);
    }

    @PostMapping("/send-sms-code")
    @Operation(summary = "发送手机验证码")
    public Result<String> sendSmsCode(@Valid @RequestBody SendSmsCodeRequest request,
                                      HttpServletRequest httpRequest) {
        String ipAddress = httpRequest.getRemoteAddr();
        String code = smsService.sendSmsCode(request.getPhone(), "LOGIN", ipAddress);
        return Result.success(code); // 仅测试用，实际不返回验证码
    }

    @PostMapping("/phone-login")
    @Operation(summary = "手机验证码登录")
    public Result<UserLoginResponse> phoneLogin(@Valid @RequestBody PhoneLoginRequest request) {
        UserLoginResponse response = authService.phoneLogin(request);
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

        // RSA解密密码
        String oldRawPassword = RsaKeyUtil.decryptPassword(params.get("keyId"), params.get("oldPassword"));
        String newRawPassword = RsaKeyUtil.decryptPassword(params.get("keyId"), params.get("newPassword"));

        userService.changePassword(userId, oldRawPassword, newRawPassword);
        return Result.success();
    }
}
