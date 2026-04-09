package com.jy.eletender.common.exception;

import com.jy.eletender.common.enums.ResponseCode;
import lombok.Getter;

/**
 * 认证异常
 */
@Getter
public class AuthException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public AuthException(String message) {
        super(message);
        this.code = ResponseCode.UNAUTHORIZED.getCode();
    }

    public AuthException(int code, String message) {
        super(message);
        this.code = code;
    }

    public AuthException(ResponseCode responseCode) {
        super(responseCode.getMessage());
        this.code = responseCode.getCode();
    }

    public AuthException(ResponseCode responseCode, String message) {
        super(message);
        this.code = responseCode.getCode();
    }
}
