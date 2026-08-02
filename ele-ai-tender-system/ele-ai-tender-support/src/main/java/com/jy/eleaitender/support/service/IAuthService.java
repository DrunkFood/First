package com.jy.eleaitender.support.service;

import com.jy.eleaitender.common.dto.request.PhoneLoginRequest;
import com.jy.eleaitender.common.dto.request.ResetPasswordRequest;
import com.jy.eleaitender.common.dto.request.UserLoginRequest;
import com.jy.eleaitender.common.dto.response.UserLoginResponse;
import com.jy.eleaitender.common.entity.support.SysUser;
import com.jy.eleaitender.support.model.external.ExternalTokenIssueCommand;
import com.jy.eleaitender.support.model.external.ExternalTokenIssueResult;
import com.jy.eleaitender.support.model.external.ExternalUserInfoView;

/**
 * 认证服务接口
 */
public interface IAuthService {

    /**
     * 内部用户登录
     *
     * @param request 登录请求
     * @return 登录响应
     */
    UserLoginResponse login(UserLoginRequest request);

    /**
     * 手机验证码登录
     *
     * @param request 手机验证码登录请求
     * @return 登录响应
     */
    UserLoginResponse phoneLogin(PhoneLoginRequest request);

    /**
     * 手机验证码登录，并传入客户端IP地址
     *
     * @param request   手机验证码登录请求
     * @param ipAddress 客户端IP地址
     * @return 登录响应
     */
    UserLoginResponse phoneLogin(PhoneLoginRequest request, String ipAddress);

    /**
     * 短信验证码重置密码
     *
     * @param request 重置密码请求
     */
    void resetPasswordByPhone(ResetPasswordRequest request);

    /**
     * 注册系统用户
     *
     * @param username 用户名
     * @param password 密码
     * @param realName 真实姓名
     * @param phone    手机号
     * @return 用户信息
     */
    SysUser registerSysUser(String username, String password, String realName, String phone);

    /**
     * 用户登出
     */
    void logout();

    /**
     * 获取当前用户信息
     *
     * @return 用户信息
     */
    UserLoginResponse.UserInfo getUserInfo();

    /**
     * 外部系统用户换取Token
     *
     * @param appKey  应用Key
     * @param command 请求参数
     * @return Token响应
     */
    ExternalTokenIssueResult getExternalToken(String appKey, ExternalTokenIssueCommand command);

    /**
     * 获取当前外部用户信息
     *
     * @return 外部用户信息
     */
    ExternalUserInfoView getExternalUserInfo();

    /**
     * 验证外部系统签名
     *
     * @param appKey    应用Key
     * @param timestamp 时间戳
     * @param signature 签名
     * @return 是否有效
     */
    boolean verifyExternalSignature(String appKey, long timestamp, String signature);
}
