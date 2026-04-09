package com.jy.eletender.common.interaction.enums;

/**
 * 投标文件预存结果（交互层）。
 * <p>用于 {@code BidDocumentPushResponse#uploadResult}
 * 和 {@code BidDocumentResultCallbackRequest#uploadResult} 字段的取值说明。</p>
 * <p>DTO 字段类型为 {@code String}，值为本枚举的 {@link #name()}。</p>
 */
public enum InteractionUploadResult {

    /** 预存成功，文件已落盘 */
    SUCCESS,

    /** 预存失败 */
    FAILED,

    /** 预存处理中 */
    PROCESSING
}
