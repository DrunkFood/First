package com.jy.eletender.common.interaction.enums;

/**
 * 交互层响应码。
 * <p>用于 {@code InteractionResult} 的统一响应状态。</p>
 */
public enum InteractionResponseCode {

    /** 操作成功 */
    SUCCESS(200, "操作成功"),

    /** 服务端内部错误 */
    FAIL(500, "操作失败"),

    /** 请求参数校验失败 */
    PARAM_ERROR(400, "参数错误"),

    /** 签名验证不通过 */
    SIGNATURE_ERROR(3004, "签名验证失败");

    private final int code;
    private final String message;

    InteractionResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
