package com.jy.eletender.common.enums;

/**
 * 解密请求状态，对应 `bdc_decrypt_request.status`。
 * 请求状态是面向“单次提交”的结果视角，与工件状态分离。
 */
public enum DecryptRequestStatus {
    /**
     * 请求已创建，等待工件执行或复用既有工件结果。
     */
    PENDING,

    /**
     * 请求处理中。
     */
    PROCESSING,

    /**
     * 请求成功完成。
     */
    SUCCESS,

    /**
     * 请求失败完成。
     */
    FAILED
}
