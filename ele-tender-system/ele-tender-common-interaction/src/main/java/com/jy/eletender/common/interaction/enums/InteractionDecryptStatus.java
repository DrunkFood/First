package com.jy.eletender.common.interaction.enums;

/**
 * 投标文件解密状态（交互层）。
 * <p>用于 {@code BidDecryptResultCallbackRequest#status}
 * 和 {@code BidDecryptStatusResponse#status} 字段的取值说明。</p>
 * <p>DTO 字段类型为 {@code String}，值为本枚举的 {@link #name()}。</p>
 */
public enum InteractionDecryptStatus {

    /** 请求已创建，等待执行 */
    PENDING,

    /** 正在解密处理中 */
    PROCESSING,

    /** 解密成功 */
    SUCCESS,

    /** 解密失败 */
    FAILED
}
