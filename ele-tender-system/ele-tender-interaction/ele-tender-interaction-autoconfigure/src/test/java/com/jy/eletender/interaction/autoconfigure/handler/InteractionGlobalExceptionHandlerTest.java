package com.jy.eletender.interaction.autoconfigure.handler;

import com.jy.eletender.common.interaction.dto.InteractionResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InteractionGlobalExceptionHandlerTest {

    @Test
    void shouldExposeErrorMessageForUnhandledException() {
        InteractionGlobalExceptionHandler handler = new InteractionGlobalExceptionHandler();

        InteractionResult<?> result = handler.handleException(new IllegalArgumentException("交互参数异常"));

        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).contains("交互参数异常");
    }
}
