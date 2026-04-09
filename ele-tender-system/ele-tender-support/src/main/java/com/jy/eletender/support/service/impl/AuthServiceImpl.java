package com.jy.eletender.support.service.impl;

import com.jy.eletender.common.constant.RedisKeyConstant;
import com.jy.eletender.common.constant.CommonConstant;
import com.jy.eletender.common.dto.response.ExternalTokenResponse;
import com.jy.eletender.common.dto.response.ExternalUserInfoResponse;
import com.jy.eletender.common.enums.ResponseCode;
import com.jy.eletender.common.exception.AuthException;
import com.jy.eletender.common.exception.BusinessException;
import com.jy.eletender.common.util.JwtUtil;
import com.jy.eletender.common.util.PasswordUtil;
import com.jy.eletender.common.util.SignatureUtil;
import com.jy.eletender.common.dto.request.UserLoginRequest;
import com.jy.eletender.common.dto.response.UserLoginResponse;
import com.jy.eletender.common.entity.support.SysAccessSystem;
import com.jy.eletender.common.entity.support.SysUser;
import com.jy.eletender.support.mapper.SysAccessSystemMapper;
import com.jy.eletender.support.mapper.SysUserMapper;
import com.jy.eletender.common.security.LoginUser;
import com.jy.eletender.common.security.SecurityContextHolder;
import com.jy.eletender.support.service.IAuthService;
import com.jy.eletender.support.model.external.ExternalTokenIssueCommand;
import com.jy.eletender.support.model.external.ExternalTokenIssueResult;
import com.jy.eletender.support.model.external.ExternalUserInfoView;
import lombok.extern.slf4j.Slf4j;
import io.jsonwebtoken.Claims;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现
 */
@Slf4j
@Service
public class AuthServiceImpl implements IAuthService {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysAccessSystemMapper accessSystemMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${jwt.expiration:7200}")
    private long tokenExpireSeconds;

    @Value("${jwt.external-expiration:604800}")
    private long externalTokenExpireSeconds;

    @Value("${signature.expire-seconds:300}")
    private long signatureExpireSeconds;

    private long resolveTokenExpireSeconds() {
        return tokenExpireSeconds > 0 ? tokenExpireSeconds : 2 * 60 * 60;
    }

    private long resolveExternalTokenExpireSeconds() {
        return externalTokenExpireSeconds > 0 ? externalTokenExpireSeconds : 7L * 24 * 60 * 60;
    }

    private long resolveSignatureExpireSeconds() {
        return signatureExpireSeconds > 0 ? signatureExpireSeconds : 300;
    }

    @Override
    public UserLoginResponse login(UserLoginRequest request) {
        // 查询用户
        SysUser user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw new AuthException(ResponseCode.USER_PASSWORD_ERROR);
        }

        // 验证密码
        if (!PasswordUtil.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException(ResponseCode.USER_PASSWORD_ERROR);
        }

        // 检查用户状态
        if (user.getStatus() != 1) {
            throw new AuthException(ResponseCode.USER_DISABLED);
        }

        // 生成Token
        long tokenExpireSeconds = resolveTokenExpireSeconds();
        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), tokenExpireSeconds * 1000);

        // 存储Token到Redis
        String redisKey = RedisKeyConstant.TOKEN_PREFIX + user.getId();
        redisTemplate.opsForValue().set(redisKey, token, 
                tokenExpireSeconds, TimeUnit.SECONDS);

        // 加载用户权限并缓存
        List<String> permissions = userMapper.selectPermissionsByUserId(user.getId());
        String permissionKey = RedisKeyConstant.USER_PERMISSIONS_PREFIX + user.getId();
        redisTemplate.delete(permissionKey);
        if (permissions != null && !permissions.isEmpty()) {
            redisTemplate.opsForSet().add(permissionKey, permissions.toArray(new String[0]));
            redisTemplate.expire(permissionKey, RedisKeyConstant.PERMISSION_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS);
        }

        // 更新最后登录时间
        user.setLastLoginTime(new Date());
        userMapper.updateById(user);

        // 构建响应
        UserLoginResponse response = new UserLoginResponse();
        response.setToken(token);
        response.setExpireIn(tokenExpireSeconds);
        response.setPermissions(permissions);

        UserLoginResponse.UserInfo userInfo = new UserLoginResponse.UserInfo();
        userInfo.setUserId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setRealName(user.getRealName());
        userInfo.setEmail(user.getEmail());
        userInfo.setPhone(user.getPhone());
        userInfo.setRoles(userMapper.selectRoleCodesByUserId(user.getId()));
        response.setUserInfo(userInfo);

        log.info("用户[{}]登录成功", user.getUsername());
        return response;
    }

    @Override
    public void logout() {
        LoginUser loginUser = SecurityContextHolder.getLoginUser();
        if (loginUser != null && loginUser.getUserId() != null) {
            // 删除Token
            String tokenKey = RedisKeyConstant.TOKEN_PREFIX + loginUser.getUserId();
            redisTemplate.delete(tokenKey);

            // 删除权限缓存
            String permissionKey = RedisKeyConstant.USER_PERMISSIONS_PREFIX + loginUser.getUserId();
            redisTemplate.delete(permissionKey);

            log.info("用户[{}]登出成功", loginUser.getUsername());
        }
    }

    @Override
    public UserLoginResponse.UserInfo getUserInfo() {
        LoginUser loginUser = SecurityContextHolder.getLoginUser();
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new AuthException(ResponseCode.UNAUTHORIZED);
        }

        SysUser user = userMapper.selectById(loginUser.getUserId());
        if (user == null) {
            throw new AuthException(ResponseCode.USER_NOT_FOUND);
        }

        UserLoginResponse.UserInfo userInfo = new UserLoginResponse.UserInfo();
        userInfo.setUserId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setRealName(user.getRealName());
        userInfo.setEmail(user.getEmail());
        userInfo.setPhone(user.getPhone());
        userInfo.setRoles(userMapper.selectRoleCodesByUserId(user.getId()));

        return userInfo;
    }

    @Override
    public ExternalTokenIssueResult getExternalToken(String appKey, ExternalTokenIssueCommand command) {
        // 查询接入系统
        SysAccessSystem system = accessSystemMapper.selectByAppKey(appKey);
        if (system == null) {
            throw new BusinessException(ResponseCode.APP_KEY_NOT_FOUND);
        }

        // 检查系统状态
        if (system.getStatus() != 1) {
            throw new BusinessException(ResponseCode.SYSTEM_DISABLED);
        }

        // 检查有效期
        if (system.getExpireTime() != null && system.getExpireTime().before(new Date())) {
            throw new BusinessException(ResponseCode.SYSTEM_EXPIRED);
        }

        // 生成外部Token
        long tokenExpireSeconds = resolveExternalTokenExpireSeconds();
        String token = JwtUtil.generateExternalToken(
                appKey,
                command.getUserId(),
                command.getUserName(),
                command.getEnterpriseId(),
                command.getEnterpriseName(),
                command.getEnterpriseCode(),
                tokenExpireSeconds * 1000
        );

        // 存储Token到Redis
        Claims claims = JwtUtil.parseToken(token);
        String jti = claims.getId();
        String redisKey = RedisKeyConstant.EXTERNAL_TOKEN_PREFIX + appKey + ":" + command.getUserId();
        if (StringUtils.isNotBlank(jti)) {
            redisKey = redisKey + ":" + jti;
        }
        redisTemplate.opsForValue().set(redisKey, token, 
                tokenExpireSeconds, TimeUnit.SECONDS);

        log.info("外部系统[{}]用户[{}]获取Token成功", appKey, command.getUserId());
        return new ExternalTokenIssueResult(token, tokenExpireSeconds);
    }

    @Override
    public ExternalUserInfoView getExternalUserInfo() {
        LoginUser loginUser = SecurityContextHolder.getLoginUser();
        if (loginUser == null || !CommonConstant.TOKEN_TYPE_EXTERNAL.equals(loginUser.getTokenType())) {
            throw new AuthException(ResponseCode.UNAUTHORIZED);
        }

        ExternalUserInfoView view = new ExternalUserInfoView();
        view.setAppKey(loginUser.getAppKey());
        view.setUserId(loginUser.getExternalUserId());
        view.setUserName(loginUser.getExternalUserName());
        view.setEnterpriseId(loginUser.getEnterpriseId());
        view.setEnterpriseName(loginUser.getEnterpriseName());
        view.setEnterpriseCode(loginUser.getEnterpriseCode());
        return view;
    }

    @Override
    public boolean verifyExternalSignature(String appKey, long timestamp, String signature) {
        // 查询接入系统
        SysAccessSystem system = accessSystemMapper.selectByAppKey(appKey);
        if (system == null) {
            log.warn("接入系统不存在: {}", appKey);
            return false;
        }

        // 检查系统状态
        if (system.getStatus() != 1) {
            log.warn("接入系统已禁用: {}", appKey);
            return false;
        }

        // 检查有效期
        if (system.getExpireTime() != null && system.getExpireTime().before(new Date())) {
            log.warn("接入系统已过期: {}", appKey);
            return false;
        }

        long signatureWindowSeconds = resolveSignatureExpireSeconds();
        return SignatureUtil.verifySignature(appKey, timestamp, system.getAppSecret(), signature, signatureWindowSeconds);
    }
}
