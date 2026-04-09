package com.jy.eletender.tenderdocument.support.generation;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinalPackageCryptoUtilTest {

    @Test
    void shouldEncryptAndDecryptBytesByAesGcm() {
        byte[] plain = "{\"k\":\"v\",\"n\":1}".getBytes(StandardCharsets.UTF_8);
        byte[] key = Base64.getDecoder().decode("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");

        byte[] encrypted = FinalPackageCryptoUtil.encrypt(plain, "AES/GCM/NoPadding", key);
        byte[] decrypted = FinalPackageCryptoUtil.decrypt(encrypted, "AES/GCM/NoPadding", key);

        assertThat(encrypted).isNotEmpty();
        assertThat(encrypted).isNotEqualTo(plain);
        assertThat(decrypted).isEqualTo(plain);
    }

    @Test
    void shouldRejectDecryptWhenMagicInvalid() {
        byte[] key = Base64.getDecoder().decode("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        byte[] invalid = new byte[4 + 1 + 12 + 16];
        invalid[0] = 'N';
        invalid[1] = 'O';
        invalid[2] = 'P';
        invalid[3] = 'E';

        assertThatThrownBy(() -> FinalPackageCryptoUtil.decrypt(invalid, "AES/GCM/NoPadding", key))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("magic");
    }
}
