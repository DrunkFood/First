package com.jy.eleaitender.common.exception;

import com.jy.eleaitender.common.enums.ResponseCode;
import lombok.Getter;

/**
 * AI服务不可用异常
 * 当所有模型路由均不可用时抛出
 */
@Getter
public class AiUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public AiUnavailableException(String message) {
        super(message);
        this.code = ResponseCode.FAIL.getCode();
    }

    public AiUnavailableException(int code, String message) {
        super(message);
        this.code = code;
    }

    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
        this.code = ResponseCode.FAIL.getCode();
    }
}
