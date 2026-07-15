package com.jy.eleaitender.support.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jy.eleaitender.common.constant.CommonConstant;
import com.jy.eleaitender.common.constant.RedisKeyConstant;
import com.jy.eleaitender.common.dto.request.PhoneLoginRequest;
import com.jy.eleaitender.common.dto.request.ResetPasswordRequest;
import com.jy.eleaitender.common.dto.request.UserLoginRequest;
import com.jy.eleaitender.common.dto.response.UserLoginResponse;
import com.jy.eleaitender.common.entity.support.SysAccessSystem;
import com.jy.eleaitender.common.entity.support.SysRole;
import com.jy.eleaitender.common.entity.support.SysUser;
import com.jy.eleaitender.common.entity.support.SysUserRole;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.AuthException;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.common.security.LoginUser;
import com.jy.eleaitender.common.security.SecurityContextHolder;
import com.jy.eleaitender.common.util.JwtUtil;
import com.jy.eleaitender.common.util.PasswordUtil;
import com.jy.eleaitender.common.util.RsaKeyUtil;
import com.jy.eleaitender.common.util.SignatureUtil;
import com.jy.eleaitender.support.mapper.SysAccessSystemMapper;
import com.jy.eleaitender.support.mapper.SysRoleMapper;
import com.jy.eleaitender.support.mapper.SysUserMapper;
import com.jy.eleaitender.support.mapper.SysUserRoleMapper;
import com.jy.eleaitender.support.model.external.ExternalTokenIssueCommand;
import com.jy.eleaitender.support.model.external.ExternalTokenIssueResult;
import com.jy.eleaitender.support.model.external.ExternalUserInfoView;
import com.jy.eleaitender.support.service.IAuthService;
import com.jy.eleaitender.support.service.ISmsService;
import com.jy.eleaitender.support.service.IUserService;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
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

    private static final String DEFAULT_ROLE_CODE = "BID_USER";

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysAccessSystemMapper accessSystemMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ISmsService smsService;

    @Autowired
    private IUserService userService;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Value("${jwt.expiration:43200}")
    private long tokenExpireSeconds;

    @Value("${jwt.external-expiration:604800}")
    private long externalTokenExpireSeconds;

    @Value("${signature.expire-seconds:300}")
    private long signatureExpireSeconds;

    private long resolveTokenExpireSeconds() {
        return tokenExpireSeconds > 0 ? tokenExpireSeconds : 12 * 60 * 60;
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

        // RSA解密密码
        String rawPassword = RsaKeyUtil.decryptPassword(request.getKeyId(), request.getPassword());

        // 验证密码
        if (!PasswordUtil.matches(rawPassword, user.getPassword())) {
            throw new AuthException(ResponseCode.USER_PASSWORD_ERROR);
        }

        // 检查用户状态
        if (user.getStatus() != 1) {
            throw new AuthException(ResponseCode.USER_DISABLED);
        }

        // 更新最后登录时间
        user.setLastLoginTime(new Date());
        userMapper.updateById(user);

        // 构建登录响应
        UserLoginResponse response = buildLoginResponse(user);
        log.info("用户[{}]登录成功", user.getUsername());
        return response;
    }

    @Override
    public UserLoginResponse phoneLogin(PhoneLoginRequest request) {
        // 验证短信验证码
        boolean valid = smsService.verifyCode(request.getPhone(), request.getCode(), "LOGIN");
        if (!valid) {
            throw new BusinessException("验证码错误或已过期");
        }

        // 查询用户（通过手机号），未注册则自动注册
        SysUser user = userMapper.selectByPhone(request.getPhone());
        if (user == null) {
            user = autoRegisterByPhone(request.getPhone());
        }

        // 检查用户状态
        if (user.getStatus() != 1) {
            throw new AuthException(ResponseCode.USER_DISABLED);
        }

        // 更新最后登录时间
        user.setLastLoginTime(new Date());
        userMapper.updateById(user);

        // 构建登录响应
        UserLoginResponse response = buildLoginResponse(user);
        log.info("用户[{}]通过手机验证码登录成功", user.getUsername());
        return response;
    }

    @Override
    public void resetPasswordByPhone(ResetPasswordRequest request) {
        // 1. 验证短信验证码
        boolean valid = smsService.verifyCode(request.getPhone(), request.getCode(), "RESET_PWD");
        if (!valid) {
            throw new BusinessException("验证码错误或已过期");
        }

        // 2. 查询用户
        SysUser user = userMapper.selectByPhone(request.getPhone());
        if (user == null) {
            throw new BusinessException("该手机号未注册");
        }

        // 3. RSA解密新密码
        String rawPassword = RsaKeyUtil.decryptPassword(request.getKeyId(), request.getNewPassword());

        // 4. 密码长度校验（RSA解密后的明文）
        if (rawPassword.length() < 6 || rawPassword.length() > 20) {
            throw new BusinessException("密码长度需为6-20位");
        }

        // 5. 更新密码
        userService.resetPassword(user.getId(), rawPassword);

        // 6. 清除该用户的登录缓存，强制重新登录
        String tokenKey = RedisKeyConstant.TOKEN_PREFIX + user.getId();
        redisTemplate.delete(tokenKey);
        String permissionKey = RedisKeyConstant.USER_PERMISSIONS_PREFIX + user.getId();
        redisTemplate.delete(permissionKey);
        String roleKey = RedisKeyConstant.USER_ROLES_PREFIX + user.getId();
        redisTemplate.delete(roleKey);

        log.info("用户[{}]通过短信验证码重置密码成功", user.getUsername());
    }

    /**
     * 手机号自动注册：创建用户并分配默认角色
     */
    private SysUser autoRegisterByPhone(String phone) {
        // 检查用户名是否已存在（以手机号作为用户名）
        SysUser existing = userMapper.selectByUsername(phone);
        if (existing != null) {
            throw new BusinessException("该手机号对应的用户名已存在");
        }

        String password = PasswordUtil.generateRandomPassword();
        String realName = "用户" + phone.substring(phone.length() - 4);
        SysUser user = registerSysUser(phone, password, realName, phone);

        log.info("手机号[{}]自动注册成功，分配角色: {}", phone, DEFAULT_ROLE_CODE);
        return user;
    }

    @Override
    public SysUser registerSysUser(String username, String password, String realName, String phone) {
        // 创建用户
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(PasswordUtil.encode(password));
        user.setRealName(realName);
        user.setPhone(phone);
        user.setStatus(1);
        userMapper.insert(user);

        // 查找默认角色 BID_USER 并分配
        LambdaQueryWrapper<SysRole> roleQuery = new LambdaQueryWrapper<>();
        roleQuery.eq(SysRole::getRoleCode, DEFAULT_ROLE_CODE);
        roleQuery.eq(SysRole::getStatus, 1);
        SysRole defaultRole = roleMapper.selectOne(roleQuery);
        if (defaultRole != null) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(defaultRole.getId());
            userRoleMapper.insert(userRole);
        } else {
            log.warn("默认角色[BID_USER]不存在，自动注册用户[{}]未分配角色", user.getId());
        }
        return user;
    }

    /**
     * 构建登录响应（Token生成 + 权限加载 + 响应构建）
     */
    private UserLoginResponse buildLoginResponse(SysUser user) {
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
            redisTemplate.expire(permissionKey, tokenExpireSeconds, TimeUnit.SECONDS);
        }

        // 加载用户角色并缓存（数据隔离需要角色信息判断管理员）
        List<String> roles = userMapper.selectRoleCodesByUserId(user.getId());
        String roleKey = RedisKeyConstant.USER_ROLES_PREFIX + user.getId();
        redisTemplate.delete(roleKey);
        if (roles != null && !roles.isEmpty()) {
            redisTemplate.opsForSet().add(roleKey, roles.toArray(new String[0]));
            redisTemplate.expire(roleKey, tokenExpireSeconds, TimeUnit.SECONDS);
        }

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

            // 删除角色缓存
            String roleKey = RedisKeyConstant.USER_ROLES_PREFIX + loginUser.getUserId();
            redisTemplate.delete(roleKey);

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

        // 查询接入系统用户
        SysUser sysUser = userMapper.selectByUsername(appKey);
        if (sysUser == null) {
            throw new BusinessException(ResponseCode.APP_KEY_NOT_FOUND);
        }

        // 解析有效期
        long tokenExpireSeconds = resolveExternalTokenExpireSeconds();

        // 生成外部Token
        String token = JwtUtil.generateExternalToken(
                system.getId(),
                system.getAppKey(),
                sysUser.getId(),
                sysUser.getUsername(),
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
        redisTemplate.opsForValue().set(redisKey, token, tokenExpireSeconds, TimeUnit.SECONDS);

        // 缓存角色（与 buildLoginResponse 保持一致）
        List<String> roles = userMapper.selectRoleCodesByUserId(sysUser.getId());
        String roleKey = RedisKeyConstant.USER_ROLES_PREFIX + sysUser.getId();
        redisTemplate.delete(roleKey);
        if (roles != null && !roles.isEmpty()) {
            redisTemplate.opsForSet().add(roleKey, roles.toArray(new String[]{}));
            redisTemplate.expire(roleKey, tokenExpireSeconds, TimeUnit.SECONDS);
        }

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
        view.setEnterpriseCode(loginUser.getEnterpriseCode());
        view.setEnterpriseName(loginUser.getEnterpriseName());
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
