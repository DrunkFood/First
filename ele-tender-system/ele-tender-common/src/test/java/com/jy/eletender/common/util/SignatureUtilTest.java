package com.jy.eletender.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SignatureUtilTest {

    @Test
    void shouldGenerateAndVerifyHmacSha256Signature() {
        long timestamp = System.currentTimeMillis();

        String signature = SignatureUtil.generateSignature("demo-key", timestamp, "demo-secret");

        assertThat(signature).hasSize(64);
        assertThat(SignatureUtil.verifySignature("demo-key", timestamp, "demo-secret", signature)).isTrue();
        assertThat(SignatureUtil.verifySignature("demo-key", timestamp, "other-secret", signature)).isFalse();
    }

    @Test
    void shouldVerifySignatureWithCustomTimestampWindow() {
        long oldTimestamp = System.currentTimeMillis() - 10 * 60 * 1000L;
        String signature = SignatureUtil.generateSignature("demo-key", oldTimestamp, "demo-secret");

        assertThat(SignatureUtil.verifySignature("demo-key", oldTimestamp, "demo-secret", signature)).isFalse();
        assertThat(SignatureUtil.verifySignature("demo-key", oldTimestamp, "demo-secret", signature, 15 * 60)).isTrue();
    }
}
