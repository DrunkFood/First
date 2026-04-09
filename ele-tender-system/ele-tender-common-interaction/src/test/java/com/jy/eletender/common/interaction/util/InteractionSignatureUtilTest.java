package com.jy.eletender.common.interaction.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InteractionSignatureUtilTest {

    @Test
    void shouldUseTheSameHmacSha256AlgorithmAsSupportModule() {
        long timestamp = System.currentTimeMillis();

        String signature = InteractionSignatureUtil.generateSignature("demo-key", timestamp, "demo-secret");

        assertEquals(64, signature.length());
        assertTrue(InteractionSignatureUtil.verifySignature("demo-key", timestamp, "demo-secret", signature));
        assertFalse(InteractionSignatureUtil.verifySignature("demo-key", timestamp, "bad-secret", signature));
    }
}
