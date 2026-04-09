package com.jy.eletender.common.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseCodeCryptoTest {

    @Test
    void shouldReserveCryptoResponseCodesFrom7001To7013() {
        assertThat(ResponseCode.BID_DOCUMENT_PUSH_PARAM_ERROR.getCode()).isEqualTo(7001);
        assertThat(ResponseCode.BID_DOCUMENT_NOT_FOUND.getCode()).isEqualTo(7002);
        assertThat(ResponseCode.BID_DOCUMENT_SHA256_MISMATCH.getCode()).isEqualTo(7003);
        assertThat(ResponseCode.BID_DECRYPT_REQUEST_NOT_FOUND.getCode()).isEqualTo(7004);
        assertThat(ResponseCode.BID_DECRYPT_REQUEST_STATUS_INVALID.getCode()).isEqualTo(7005);
        assertThat(ResponseCode.BID_DECRYPT_ARTIFACT_NOT_FOUND.getCode()).isEqualTo(7006);
        assertThat(ResponseCode.BID_DECRYPT_ARTIFACT_EXECUTE_FAILED.getCode()).isEqualTo(7007);
        assertThat(ResponseCode.BID_DECRYPT_ARTIFACT_TIMEOUT.getCode()).isEqualTo(7008);
        assertThat(ResponseCode.BID_DECRYPT_CALLBACK_FAILED.getCode()).isEqualTo(7009);
        assertThat(ResponseCode.BID_DECRYPT_NATIVE_ERROR.getCode()).isEqualTo(7010);
        assertThat(ResponseCode.BID_DECRYPT_PARSE_ERROR.getCode()).isEqualTo(7011);
        assertThat(ResponseCode.BID_DECRYPT_DUPLICATE_REQUEST.getCode()).isEqualTo(7012);
        assertThat(ResponseCode.BID_DECRYPT_INTERNAL_ERROR.getCode()).isEqualTo(7013);
    }
}
