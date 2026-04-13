package com.jy.eleaitender.support.service;

import com.jy.eleaitender.common.dto.request.PhoneLoginRequest;
import com.jy.eleaitender.common.dto.request.UserLoginRequest;
import com.jy.eleaitender.common.dto.response.UserLoginResponse;
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
