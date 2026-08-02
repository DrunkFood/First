package com.jy.eleaitender.common.exception;

import com.jy.eleaitender.common.enums.ResponseCode;
import lombok.Getter;

import java.io.Serial;

/**
 * AI输出格式异常
 */
@Getter
public class AiErrorContentException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int code;

    private final String content;

    public AiErrorContentException(String message, String content) {
        super(message);
        this.code = ResponseCode.FAIL.getCode();
        this.content = content;
    }

    public AiErrorContentException(int code, String message, String content) {
        super(message);
        this.code = code;
        this.content = content;
    }

    public AiErrorContentException(String message, String content, Throwable cause) {
        super(message, cause);
        this.code = ResponseCode.FAIL.getCode();
        this.content = content;
    }
}
