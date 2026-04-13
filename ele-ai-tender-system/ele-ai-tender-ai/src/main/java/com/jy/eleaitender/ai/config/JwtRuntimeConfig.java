package com.jy.eleaitender.ai.config;

import com.jy.eleaitender.common.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 初始化JWT运行时配置，确保与签发服务使用同一套密钥与过期策略。
 */
@Configuration
public class JwtRuntimeConfig {

    private static final long DEFAULT_INTERNAL_EXPIRATION_SECONDS = 2 * 60 * 60;
    private static final long DEFAULT_EXTERNAL_EXPIRATION_SECONDS = 7L * 24 * 60 * 60;

    public JwtRuntimeConfig(@Value("${jwt.secret:}") String secret,
                            @Value("${jwt.expiration:7200}") long expirationSeconds,
                            @Value("${jwt.external-expiration:604800}") long externalExpirationSeconds) {
        JwtUtil.configure(
                secret,
                resolveSeconds(expirationSeconds, DEFAULT_INTERNAL_EXPIRATION_SECONDS) * 1000,
                resolveSeconds(externalExpirationSeconds, DEFAULT_EXTERNAL_EXPIRATION_SECONDS) * 1000
        );
    }

    private long resolveSeconds(long seconds, long defaultSeconds) {
        return seconds > 0 ? seconds : defaultSeconds;
    }
}
