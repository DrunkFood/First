package com.jy.eletender.common.enums;

/**
 * 解密工件状态，对应 `bdc_decrypt_artifact.status`。
 */
public enum DecryptArtifactStatus {
    /**
     * 已建工件但尚未开始执行。
     */
    PENDING,

    /**
     * 正在执行实际解密。
     */
    PROCESSING,

    /**
     * 工件已成功产出，可被多个请求复用。
     */
    SUCCESS,

    /**
     * 工件执行失败。
     */
    FAILED
}
