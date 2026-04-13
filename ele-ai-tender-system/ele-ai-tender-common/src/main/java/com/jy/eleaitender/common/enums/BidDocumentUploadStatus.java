package com.jy.eleaitender.common.enums;

/**
 * 投标文件预存状态，对应 `bdc_bid_document.upload_status`。
 */
public enum BidDocumentUploadStatus {
    /**
     * 预存成功，文件已落盘且可作为后续解密输入。
     */
    SUCCESS,

    /**
     * 预存失败。
     */
    FAILED,

    /**
     * 预存处理中。
     */
    PROCESSING
}
