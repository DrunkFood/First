package com.jy.eleaitender.common.security;

import com.jy.eleaitender.common.constant.CommonConstant;
import com.jy.eleaitender.common.constant.RedisKeyConstant;
import com.jy.eleaitender.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final List<String> excludePathPrefixes;
    private final Set<String> acceptedTokenTypes;

    public JwtAuthenticationFilter(StringRedisTemplate redisTemplate,
                                   List<String> excludePathPrefixes,
                                   Set<String> acceptedTokenTypes) {
        this.redisTemplate = redisTemplate;
        this.excludePathPrefixes = excludePathPrefixes;
        this.acceptedTokenTypes = acceptedTokenTypes;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isExcludedPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = getTokenFromRequest(request);
            if (StringUtils.isNotBlank(token) && JwtUtil.validateToken(token)) {
                Claims claims = JwtUtil.parseToken(token);
                String tokenType = claims.get("type", String.class);
                if (acceptedTokenTypes.contains(tokenType) && isTokenValid(token, claims, tokenType)) {
                    SecurityContextHolder.setLoginUser(buildLoginUser(claims, tokenType));
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            // 请求处理完成后清空上下文
            SecurityContextHolder.clear();
        }
    }

    private boolean isExcludedPath(String requestUri) {
        return excludePathPrefixes.stream().anyMatch(requestUri::startsWith);
    }

    protected String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.isNotBlank(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean isTokenValid(String token, Claims claims, String tokenType) {
        if (CommonConstant.TOKEN_TYPE_INTERNAL.equals(tokenType)) {
            String redisKey = RedisKeyConstant.TOKEN_PREFIX + getInternalUserId(claims);
            String storedToken = redisTemplate.opsForValue().get(redisKey);
            return token.equals(storedToken);
        }

        if (CommonConstant.TOKEN_TYPE_EXTERNAL.equals(tokenType)) {
            String appKey = claims.get("appKey", String.class);
            String userId = claims.get("userId", String.class);
            String jti = claims.getId();
            if (StringUtils.isNotBlank(jti)) {
                String sessionRedisKey = RedisKeyConstant.EXTERNAL_TOKEN_PREFIX + appKey + ":" + userId + ":" + jti;
                String sessionToken = redisTemplate.opsForValue().get(sessionRedisKey);
                if (token.equals(sessionToken)) {
                    return true;
                }
            }
            String legacyRedisKey = RedisKeyConstant.EXTERNAL_TOKEN_PREFIX + appKey + ":" + userId;
            String legacyToken = redisTemplate.opsForValue().get(legacyRedisKey);
            return token.equals(legacyToken);
        }

        return false;
    }

    private LoginUser buildLoginUser(Claims claims, String tokenType) {
        LoginUser loginUser = new LoginUser();
        loginUser.setTokenType(tokenType);

        if (CommonConstant.TOKEN_TYPE_INTERNAL.equals(tokenType)) {
            loginUser.setUserId(getInternalUserId(claims));
            loginUser.setUsername(claims.get("username", String.class));
            // 从 Redis 加载角色缓存（数据隔离需要判断管理员）
            loadRolesFromRedis(loginUser);
            return loginUser;
        }

        loginUser.setAppKey(claims.get("appKey", String.class));
        loginUser.setExternalUserId(claims.get("userId", String.class));
        loginUser.setExternalUserName(claims.get("userName", String.class));
        loginUser.setExternalJti(claims.getId());
        loginUser.setEnterpriseId(claims.get("enterpriseId", String.class));
        loginUser.setEnterpriseName(claims.get("enterpriseName", String.class));
        loginUser.setEnterpriseCode(claims.get("enterpriseCode", String.class));
        return loginUser;
    }

    private Long getInternalUserId(Claims claims) {
        Object userIdObj = claims.get("userId");
        if (userIdObj instanceof Integer integerUserId) {
            return integerUserId.longValue();
        }
        if (userIdObj instanceof Long longUserId) {
            return longUserId;
        }
        return Long.valueOf(String.valueOf(userIdObj));
    }

    /**
     * 从 Redis 加载用户角色缓存
     * 角色在登录时由 AuthServiceImpl 写入 Redis
     */
    private void loadRolesFromRedis(LoginUser loginUser) {
        if (loginUser.getUserId() == null) {
            return;
        }
        try {
            String roleKey = RedisKeyConstant.USER_ROLES_PREFIX + loginUser.getUserId();
            Set<String> roles = redisTemplate.opsForSet().members(roleKey);
            if (roles != null && !roles.isEmpty()) {
                loginUser.setRoles(new ArrayList<>(roles));
            }
        } catch (Exception e) {
            // Redis 读取失败不影响主流程
        }
    }
}
