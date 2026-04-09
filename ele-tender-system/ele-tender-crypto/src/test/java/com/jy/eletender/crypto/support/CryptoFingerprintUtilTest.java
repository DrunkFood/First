package com.jy.eletender.crypto.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CryptoFingerprintUtilTest {

    @Test
    void shouldGenerateLowercaseHmacSha256FingerprintWithConfiguredSecret() {
        CryptoFingerprintUtil.configure("fingerprint-secret");

        assertThat(CryptoFingerprintUtil.fingerprint("Abc123!"))
                .isEqualTo("7e2dc799cbd30123ea0eae95198f730ce84f182fb06c6cd22583f91f9413896b");
    }

    @Test
    void shouldThrowWhenSecretNotConfigured() {
        CryptoFingerprintUtil.configure(null);

        assertThatThrownBy(() -> CryptoFingerprintUtil.fingerprint("Abc123!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bidderPwdFingerprintSecret");
    }
}
