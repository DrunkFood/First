package com.jy.eleaitender.common.util;

import com.jy.eleaitender.common.constant.CommonConstant;
import com.jy.eleaitender.common.exception.AuthException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT工具类 (JJWT 0.12.x API)
 */
@Slf4j
public class JwtUtil {

    private JwtUtil() {}

    /**
     * 默认密钥（实际使用时应从配置文件读取，至少32字节）
     */
    private static final String DEFAULT_SECRET = "eleAiTenderSystemSecretKey2024JWT!";

    /**
     * 默认过期时间（毫秒）- 12小时
     */
    private static final long DEFAULT_EXPIRATION = 12 * 60 * 60 * 1000L;

    /**
     * 外部Token默认过期时间（毫秒）- 7天
     */
    private static final long EXTERNAL_EXPIRATION = 7L * 24 * 60 * 60 * 1000;

    /**
     * 运行时配置（默认回退到内置值）
     */
    private static volatile String runtimeSecret = DEFAULT_SECRET;
    private static volatile long runtimeExpiration = DEFAULT_EXPIRATION;
    private static volatile long runtimeExternalExpiration = EXTERNAL_EXPIRATION;

    public static synchronized void configure(String secret, Long expirationMillis, Long externalExpirationMillis) {
        runtimeSecret = StringUtils.isNotBlank(secret) ? secret : DEFAULT_SECRET;
        runtimeExpiration = resolvePositive(expirationMillis, DEFAULT_EXPIRATION);
        runtimeExternalExpiration = resolvePositive(externalExpirationMillis, EXTERNAL_EXPIRATION);
    }

    static synchronized void resetConfigForTest() {
        runtimeSecret = DEFAULT_SECRET;
        runtimeExpiration = DEFAULT_EXPIRATION;
        runtimeExternalExpiration = EXTERNAL_EXPIRATION;
    }

    private static long resolvePositive(Long value, long defaultValue) {
        return value != null && value > 0 ? value : defaultValue;
    }

    /**
     * 获取签名密钥
     */
    private static SecretKey getSigningKey(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成内部用户Token
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param secret   密钥
     * @return Token字符串
     */
    public static String generateToken(Long userId, String username, String secret) {
        return generateToken(userId, username, secret, runtimeExpiration);
    }

    /**
     * 生成内部用户Token（自定义过期时间）
     */
    public static String generateToken(Long userId, String username, String secret, long expirationMillis) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("type", CommonConstant.TOKEN_TYPE_INTERNAL);

        return createToken(claims, secret, expirationMillis, null);
    }

    /**
     * 生成内部用户Token（使用默认密钥 + 自定义过期时间）
     */
    public static String generateToken(Long userId, String username, long expirationMillis) {
        return generateToken(userId, username, runtimeSecret, expirationMillis);
    }

    /**
     * 生成内部用户Token（使用默认密钥）
     */
    public static String generateToken(Long userId, String username) {
        return generateToken(userId, username, runtimeSecret, runtimeExpiration);
    }

    /**
     * 生成服务间调用Token（SERVICE类型，不依赖Redis校验）
     *
     * @param serviceName 服务名称
     * @param secret      密钥
     * @param expirationMillis 过期时间（毫秒）
     * @return Token字符串
     */
    public static String generateServiceToken(String serviceName, String secret, long expirationMillis) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", CommonConstant.TOKEN_TYPE_SERVICE);
        claims.put("serviceName", serviceName);
        claims.put("username", serviceName);
        return createToken(claims, secret, expirationMillis, null);
    }

    /**
     * 生成服务间调用Token（使用默认密钥）
     */
    public static String generateServiceToken(String serviceName, long expirationMillis) {
        return generateServiceToken(serviceName, runtimeSecret, expirationMillis);
    }

    /**
     * 生成外部系统用户Token
     *
     * @param appKey         应用Key
     * @param userId         外部用户ID
     * @param userName       外部用户名称
     * @param enterpriseId   企业ID
     * @param enterpriseName 企业名称
     * @param enterpriseCode 企业社会统一信用代码
     * @param secret         密钥
     * @return Token字符串
     */
    public static String generateExternalToken(String appKey, String userId, String userName,
                                                String enterpriseId, String enterpriseName,
                                                String enterpriseCode,
                                                String secret) {
        return generateExternalToken(appKey, userId, userName, enterpriseId, enterpriseName, enterpriseCode, secret, runtimeExternalExpiration);
    }

    /**
     * 生成外部系统用户Token（自定义过期时间）
     */
    public static String generateExternalToken(String appKey, String userId, String userName,
                                               String enterpriseId, String enterpriseName,
                                               String enterpriseCode,
                                               String secret,
                                               long expirationMillis) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", CommonConstant.TOKEN_TYPE_EXTERNAL);
        claims.put("appKey", appKey);
        claims.put("userId", userId);
        claims.put("userName", userName);
        claims.put("enterpriseId", enterpriseId);
        claims.put("enterpriseName", enterpriseName);
        claims.put("enterpriseCode", enterpriseCode);
        String sessionJti = UUID.randomUUID().toString();

        return createToken(claims, secret, expirationMillis, sessionJti);
    }

    /**
     * 生成外部系统用户Token（使用默认密钥 + 自定义过期时间）
     */
    public static String generateExternalToken(String appKey, String userId, String userName,
                                               String enterpriseId, String enterpriseName,
                                               String enterpriseCode,
                                               long expirationMillis) {
        return generateExternalToken(appKey, userId, userName, enterpriseId, enterpriseName, enterpriseCode, runtimeSecret, expirationMillis);
    }

    /**
     * 生成外部系统用户Token（使用默认密钥）
     */
    public static String generateExternalToken(String appKey, String userId, String userName,
                                                String enterpriseId, String enterpriseName,
                                                String enterpriseCode) {
        return generateExternalToken(appKey, userId, userName, enterpriseId, enterpriseName, enterpriseCode, runtimeSecret);
    }

    /**
     * 创建Token
     */
    private static String createToken(Map<String, Object> claims, String secret, long expiration, String jti) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        JwtBuilder builder = Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate);

        if (StringUtils.isNotBlank(jti)) {
            builder.id(jti);
        }

        return builder.signWith(getSigningKey(secret)).compact();
    }

    /**
     * 解析Token
     *
     * @param token  Token字符串
     * @param secret 密钥
     * @return Claims
     */
    public static Claims parseToken(String token, String secret) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey(secret))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new AuthException("Token已过期", e);
        } catch (Exception e) {
            throw new AuthException("Token无效", e);
        }
    }

    /**
     * 解析Token（使用默认密钥）
     */
    public static Claims parseToken(String token) {
        return parseToken(token, runtimeSecret);
    }

    /**
     * 从Token中获取用户ID
     */
    public static Long getUserId(String token) {
        Claims claims = parseToken(token);
        Object userId = claims.get("userId");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        } else if (userId instanceof Long) {
            return (Long) userId;
        } else if (userId instanceof String) {
            return Long.parseLong((String) userId);
        }
        return null;
    }

    /**
     * 从Token中获取用户名
     */
    public static String getUsername(String token) {
        Claims claims = parseToken(token);
        return (String) claims.get("username");
    }

    /**
     * 从Token中获取Token类型
     */
    public static String getTokenType(String token) {
        Claims claims = parseToken(token);
        return (String) claims.get("type");
    }

    /**
     * 验证Token是否有效
     */
    public static boolean validateToken(String token, String secret) {
        if (StringUtils.isBlank(token)) {
            return false;
        }
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey(secret))
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.warn("Token校验失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 验证Token是否有效（使用默认密钥）
     */
    public static boolean validateToken(String token) {
        return validateToken(token, runtimeSecret);
    }

    /**
     * 判断Token是否即将过期（5分钟内）
     */
    public static boolean isTokenAboutToExpire(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            long diff = expiration.getTime() - System.currentTimeMillis();
            return diff > 0 && diff < 5 * 60 * 1000;
        } catch (Exception e) {
            log.warn("判断Token是否即将过期失败: {}", e.getMessage(), e);
            return false;
        }
    }
}
