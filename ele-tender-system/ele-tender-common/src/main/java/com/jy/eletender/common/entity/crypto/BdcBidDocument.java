package com.jy.eletender.common.entity.crypto;

import com.baomidou.mybatisplus.annotation.TableName;
import com.jy.eletender.common.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 投标文件预存记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bdc_bid_document")
public class BdcBidDocument extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 业务系统 appKey。
     */
    private String appKey;

    /**
     * 发起预存的业务系统用户 ID。
     */
    private String userId;

    /**
     * 文件服务中的 fileId。
     */
    private Long fileId;

    /**
     * 原始文件名。
     */
    private String fileName;

    /**
     * 加密投标文件 SHA-256。
     */
    private String fileSha256;

    /**
     * crypto 服务本地落盘路径。
     */
    private String fileStoragePath;

    /**
     * 文件大小，单位字节。
     */
    private Long fileSize;

    /**
     * 预存状态：PENDING/PROCESSING/SUCCESS/FAILED。
     */
    private String uploadStatus;

    /**
     * 回调状态：PENDING/SUCCESS/FAILED。
     */
    private String callbackStatus;

    /**
     * 最近一次回调时间。
     */
    private Date callbackTime;

    /**
     * 最近一次回调失败信息。
     */
    private String callbackErrorMessage;
}
