package com.jy.eletender.common.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilExternalTokenExpirationTest {

    @Test
    void shouldSetExternalTokenExpirationToSevenDays() {
        String token = JwtUtil.generateExternalToken(
                "app-a",
                "user-1",
                "测试用户",
                "ent-1",
                "测试企业",
                "913301"
        );

        Claims claims = JwtUtil.parseToken(token);
        long expirationMs = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();

        assertThat(expirationMs).isEqualTo(7L * 24 * 60 * 60 * 1000);
        assertThat(claims.getId()).isNotBlank();
    }

    @Test
    void shouldUseCustomExternalTokenExpirationWhenProvided() {
        long customExpirationMs = 10L * 60 * 1000;
        String token = JwtUtil.generateExternalToken(
                "app-a",
                "user-1",
                "测试用户",
                "ent-1",
                "测试企业",
                "913301",
                "EleTenderSystemSecretKey2024JWT!",
                customExpirationMs
        );

        Claims claims = JwtUtil.parseToken(token);
        long expirationMs = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();

        assertThat(expirationMs).isEqualTo(customExpirationMs);
        assertThat(claims.getId()).isNotBlank();
    }
}
