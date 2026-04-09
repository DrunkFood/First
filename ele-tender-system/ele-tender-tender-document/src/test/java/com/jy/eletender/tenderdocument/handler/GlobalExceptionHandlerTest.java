package com.jy.eletender.tenderdocument.handler;

import com.jy.eletender.common.response.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void shouldExposeErrorMessageForUnhandledException() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        Result<?> result = handler.handleException(new IllegalArgumentException("编制参数错误"));

        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).contains("编制参数错误");
    }
}
