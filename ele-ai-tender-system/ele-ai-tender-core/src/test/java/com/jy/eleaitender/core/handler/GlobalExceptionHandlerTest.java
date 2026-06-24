package com.jy.eleaitender.core.handler;

import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.response.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void handleExceptionDoesNotExposeInternalSqlDetails() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        Result<?> result = handler.handleException(new RuntimeException(
                "### Error updating database. SQL: INSERT INTO tb_project VALUES (...)"));

        assertThat(result.getCode()).isEqualTo(ResponseCode.FAIL.getCode());
        assertThat(result.getMessage()).isEqualTo("系统异常，请联系管理员");
        assertThat(result.getMessage()).doesNotContain("SQL", "INSERT", "tb_project");
    }
}
