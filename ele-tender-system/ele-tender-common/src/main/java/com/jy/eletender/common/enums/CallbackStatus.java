package com.jy.eletender.common.enums;

/**
 * 回调状态，对应 `bdc_bid_document.callback_status`
 * 和 `bdc_decrypt_request.callback_status`。
 */
public enum CallbackStatus {
    /**
     * 尚未调用回调。
     */
    NOT_CALLED,

    /**
     * 回调成功。
     */
    SUCCESS,

    /**
     * 回调失败。
     */
    FAILED
}
