package com.jy.eletender.crypto.service;

import com.jy.eletender.common.entity.crypto.BdcDecryptArtifact;

/**
 * 解密工件状态服务，负责维护 `bdc_decrypt_artifact` 的状态流转。
 */
public interface IDecryptArtifactService {

    /**
     * 查询工件。
     */
    BdcDecryptArtifact getById(Long artifactId);

    /**
     * 将工件置为处理中。
     */
    void markProcessing(Long artifactId);

    /**
     * 将工件置为成功并写入产物信息。
     */
    void markSuccess(Long artifactId, String resultJson, String decryptedPath, String decryptedSha256, String bidRecordDataJson);

    /**
     * 将工件置为失败。
     */
    void markFailed(Long artifactId, String errorMessage);

    /**
     * 重置工件以便重试。
     */
    void resetForRetry(Long artifactId);
}
