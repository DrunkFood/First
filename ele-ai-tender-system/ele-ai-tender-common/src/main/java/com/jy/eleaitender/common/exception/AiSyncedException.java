package com.jy.eleaitender.common.exception;

import com.jy.eleaitender.common.enums.ResponseCode;
import lombok.Getter;

/**
 * AI任务结果同步异常
 * 当所有模型路由均不可用时抛出
 */
@Getter
public class AiSyncedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public AiSyncedException(String message) {
        super(message);
        this.code = ResponseCode.FAIL.getCode();
    }

    public AiSyncedException(int code, String message) {
        super(message);
        this.code = code;
    }

    public AiSyncedException(String message, Throwable cause) {
        super(message, cause);
        this.code = ResponseCode.FAIL.getCode();
    }
}
