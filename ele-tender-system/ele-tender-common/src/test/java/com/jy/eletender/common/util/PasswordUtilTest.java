package com.jy.eletender.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordUtilTest {

    @Test
    void shouldEncodePasswordWithBcryptAndVerifySuccessfully() {
        String encoded = PasswordUtil.encode("123456");

        assertThat(encoded).startsWith("$2");
        assertThat(PasswordUtil.matches("123456", encoded)).isTrue();
        assertThat(PasswordUtil.matches("654321", encoded)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenEncodedPasswordIsNotBcryptFormat() {
        assertThat(PasswordUtil.matches("123456", "plain-text")).isFalse();
    }
}
