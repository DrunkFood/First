package com.jy.eletender.support.handler;

import com.jy.eletender.common.response.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void shouldExposeErrorMessageForUnhandledException() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        Result<?> result = handler.handleException(new IllegalArgumentException("参数非法"));

        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).contains("参数非法");
    }
}
