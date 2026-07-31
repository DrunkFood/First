package com.jy.eleaitender.support.controller;

import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.dto.request.PhoneLoginRequest;
import com.jy.eleaitender.common.dto.request.ResetPasswordRequest;
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
import org.apache.commons.lang3.StringUtils;
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
        String scene = StringUtils.isNotBlank(request.getScene()) ? request.getScene() : "LOGIN";
        String code = smsService.sendSmsCode(request.getPhone(), scene, ipAddress);
        return Result.success(code);
    }

    @PostMapping("/phone-login")
    @Operation(summary = "手机验证码登录")
    public Result<UserLoginResponse> phoneLogin(@Valid @RequestBody PhoneLoginRequest request,
                                                HttpServletRequest httpRequest) {
        UserLoginResponse response = authService.phoneLogin(request, resolveClientIp(httpRequest));
        return Result.success(response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (!StringUtils.isNotBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (!StringUtils.isNotBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @PostMapping("/reset-password")
    @Operation(summary = "短信验证码重置密码")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPasswordByPhone(request);
        return Result.success();
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
