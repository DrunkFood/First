package com.jy.eletender.common.interaction.dto;

import com.jy.eletender.common.interaction.enums.InteractionResponseCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 交互层统一响应
 */
@Data
public class InteractionResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;
    private String message;
    private T data;
    private long timestamp;

    public InteractionResult() {
        this.timestamp = System.currentTimeMillis();
    }

    public InteractionResult(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> InteractionResult<T> success() {
        return new InteractionResult<T>(InteractionResponseCode.SUCCESS.getCode(),
                InteractionResponseCode.SUCCESS.getMessage(), null);
    }

    public static <T> InteractionResult<T> success(T data) {
        return new InteractionResult<T>(InteractionResponseCode.SUCCESS.getCode(),
                InteractionResponseCode.SUCCESS.getMessage(), data);
    }

    public static <T> InteractionResult<T> success(String message, T data) {
        return new InteractionResult<T>(InteractionResponseCode.SUCCESS.getCode(), message, data);
    }

    public static <T> InteractionResult<T> fail(String message) {
        return new InteractionResult<T>(InteractionResponseCode.FAIL.getCode(), message, null);
    }

    public static <T> InteractionResult<T> fail(int code, String message) {
        return new InteractionResult<T>(code, message, null);
    }

    public static <T> InteractionResult<T> fail(InteractionResponseCode responseCode) {
        return new InteractionResult<T>(responseCode.getCode(), responseCode.getMessage(), null);
    }

    public static <T> InteractionResult<T> fail(InteractionResponseCode responseCode, String message) {
        return new InteractionResult<T>(responseCode.getCode(), message, null);
    }

    public boolean isSuccess() {
        return this.code == InteractionResponseCode.SUCCESS.getCode();
    }
}
