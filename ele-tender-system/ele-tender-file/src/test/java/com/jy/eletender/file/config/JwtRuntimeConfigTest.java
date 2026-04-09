package com.jy.eletender.file.config;

import com.jy.eletender.common.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtRuntimeConfigTest {

    @AfterEach
    void tearDown() {
        JwtUtil.configure(null, null, null);
    }

    @Test
    void shouldConfigureJwtSecretFromProperties() {
        String secret = "FileServiceJwtSecretForTest_1234567890";
        String token = JwtUtil.generateExternalToken(
                "demo-app",
                "u002",
                "file-user",
                "ent-002",
                "Demo Enterprise 2",
                "91420100177666879T",
                secret,
                600_000L
        );

        assertThat(JwtUtil.validateToken(token)).isFalse();

        new JwtRuntimeConfig(secret, 120, 600);

        assertThat(JwtUtil.validateToken(token)).isTrue();
    }
}
