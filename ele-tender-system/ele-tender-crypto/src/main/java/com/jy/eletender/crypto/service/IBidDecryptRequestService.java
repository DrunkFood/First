package com.jy.eletender.crypto.service;

import com.jy.eletender.common.interaction.dto.BidDecryptStatusResponse;
import com.jy.eletender.common.interaction.dto.BidDecryptSubmitRequest;
import com.jy.eletender.common.interaction.dto.BidDecryptSubmitResponse;
import com.jy.eletender.crypto.support.CryptoUserContext;

/**
 * 解密请求服务，负责写入 `bdc_decrypt_request`、复用或创建 `bdc_decrypt_artifact`，
 * 并在工件完成后把结果投影到请求记录。
 */
public interface IBidDecryptRequestService {

    /**
     * 提交解密请求。
     */
    BidDecryptSubmitResponse submit(CryptoUserContext userContext, BidDecryptSubmitRequest request);

    /**
     * 按 appKey + recordId 查询解密请求状态。
     */
    BidDecryptStatusResponse queryStatus(String appKey, String recordId);

    /**
     * 在工件进入终态后，同步更新所有关联请求并触发回调留痕。
     */
    void handleArtifactCompletion(Long artifactId);
}
