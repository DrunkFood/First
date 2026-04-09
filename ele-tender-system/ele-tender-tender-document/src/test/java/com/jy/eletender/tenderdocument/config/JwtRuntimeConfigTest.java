package com.jy.eletender.tenderdocument.config;

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
    void shouldConfigureJwtSecretAndExpirationFromProperties() {
        String secret = "TenderDocumentJwtSecretForTest_1234567890";
        String token = JwtUtil.generateExternalToken(
                "demo-app",
                "u001",
                "tester",
                "ent-001",
                "Demo Enterprise",
                "91420100177666879T",
                secret,
                600_000L
        );

        assertThat(JwtUtil.validateToken(token)).isFalse();

        new JwtRuntimeConfig(secret, 120, 600);

        assertThat(JwtUtil.validateToken(token)).isTrue();
    }
}
