package com.jy.eletender.common.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilConfigurableSecretTest {

    private static final String SECRET_A = "01234567890123456789012345678901";
    private static final String SECRET_B = "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456";

    @AfterEach
    void tearDown() {
        JwtUtil.resetConfigForTest();
    }

    @Test
    void shouldUseConfiguredSecretForDefaultGenerateAndValidate() {
        JwtUtil.configure(SECRET_A, null, null);
        String token = JwtUtil.generateToken(1001L, "admin");

        assertThat(JwtUtil.validateToken(token)).isTrue();

        JwtUtil.configure(SECRET_B, null, null);
        assertThat(JwtUtil.validateToken(token)).isFalse();
    }
}
