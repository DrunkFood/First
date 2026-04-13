package com.jy.eleaitender.common.entity.crypto;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eleaitender.common.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 解密工件
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bdc_decrypt_artifact")
public class BdcDecryptArtifact extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 业务系统 appKey。
     */
    private String appKey;

    /**
     * 加密投标文件 SHA-256。
     */
    private String fileSha256;

    /**
     * 投标口令指纹，用于解密结果去重。
     */
    private String bidderPwdFingerprint;

    /**
     * 工件状态：PENDING/PROCESSING/SUCCESS/FAILED。
     */
    private String status;

    /**
     * 工件累计重试次数。
     */
    private Integer retryCount;

    /**
     * 本轮处理开始时间。
     */
    private Date processingStartedAt;

    /**
     * 工件完成时间。
     */
    private Date completedAt;

    /**
     * 工件失败原因。
     */
    private String errorMessage;

    /**
     * 原始解密结果 JSON。
     */
    private String decryptResultJson;

    /**
     * 解密后文件落盘路径。
     */
    private String decryptedFilePath;

    /**
     * 解密后文件 SHA-256。
     */
    private String decryptedFileSha256;

    /**
     * 提取后的标录 JSON。
     */
    private String bidRecordDataJson;
}
