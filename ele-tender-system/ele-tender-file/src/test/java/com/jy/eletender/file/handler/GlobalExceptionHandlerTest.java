package com.jy.eletender.file.handler;

import com.jy.eletender.common.exception.AuthException;
import com.jy.eletender.common.response.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void shouldExposeErrorMessageForUnhandledException() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        Result<?> result = handler.handleException(new IllegalArgumentException("文件参数错误"));

        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).contains("文件参数错误");
    }

    @Test
    void shouldReturnUnauthorizedCodeForAuthException() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        Result<?> result = handler.handleAuthException(new AuthException("未授权"));

        assertThat(result.getCode()).isEqualTo(401);
        assertThat(result.getMessage()).isEqualTo("未授权");
    }
}
