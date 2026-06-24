package com.jy.eleaitender.common.interaction.exception;

import com.jy.eleaitender.common.interaction.enums.InteractionResponseCode;

/**
 * 交互层运行时异常
 */
public class InteractionException extends RuntimeException {

    private final int code;

    public InteractionException(String message) {
        this(InteractionResponseCode.FAIL, message);
    }

    public InteractionException(InteractionResponseCode responseCode) {
        this(responseCode, responseCode.getMessage());
    }

    public InteractionException(InteractionResponseCode responseCode, String message) {
        super(message);
        this.code = responseCode.getCode();
    }

    public InteractionException(InteractionResponseCode responseCode, String message, Throwable cause) {
        super(message, cause);
        this.code = responseCode.getCode();
    }

    public InteractionException(int code, String message) {
        super(message);
        this.code = code;
    }

    public InteractionException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
