package com.jy.eletender.common.entity.crypto;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eletender.common.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 解密请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bdc_decrypt_request")
public class BdcDecryptRequest extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 业务系统 appKey。
     */
    private String appKey;

    /**
     * 项目 ID。
     */
    private String projectId;

    /**
     * 标段 ID。
     */
    private String tenderId;

    /**
     * 开标标录 ID。
     */
    private String bidRecordId;

    /**
     * 加密投标文件 SHA-256。
     */
    private String fileSha256;

    /**
     * 关联解密工件 ID。
     */
    private Long artifactId;

    /**
     * 请求状态：PENDING/PROCESSING/SUCCESS/FAILED。
     */
    private String status;

    /**
     * 请求完成时间。
     */
    private Date completedAt;

    /**
     * 请求失败原因。
     */
    private String errorMessage;

    /**
     * 回调状态：PENDING/SUCCESS/FAILED。
     */
    private String callbackStatus;

    /**
     * 回调累计重试次数。
     */
    private Integer callbackRetryCount;

    /**
     * 业务系统回调响应码。
     */
    private String callbackResponseCode;

    /**
     * 业务系统回调响应信息。
     */
    private String callbackResponseMessage;

    /**
     * 最近一次回调时间。
     */
    private Date callbackTime;

    /**
     * 提交解密请求的业务系统用户 ID。
     */
    private String requestedByUserId;

    /**
     * 提交解密请求的业务系统用户名。
     */
    private String requestedByUserName;

    /**
     * 提交解密请求的企业 ID。
     */
    private String requestedByEnterpriseId;

    /**
     * 按项目/标段组织的加密文件副本路径。
     */
    private String organizedEncryptedFilePath;

    /**
     * 按项目/标段组织的解密文件副本路径。
     */
    private String organizedDecryptedFilePath;
}
