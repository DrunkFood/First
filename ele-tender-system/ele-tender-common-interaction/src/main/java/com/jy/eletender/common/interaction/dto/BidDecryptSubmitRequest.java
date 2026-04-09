package com.jy.eletender.common.interaction.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 投标文件解密提交请求
 */
@Data
public class BidDecryptSubmitRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String projectId;

    private String tenderId;

    private String bidRecordId;

    private String bidderPwdStr;

    private String fileSha256;
}
