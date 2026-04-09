package com.jy.eletender.crypto.service.impl;

import com.jy.eletender.common.entity.crypto.BdcDecryptArtifact;
import com.jy.eletender.common.enums.DecryptArtifactStatus;
import com.jy.eletender.crypto.mapper.BdcDecryptArtifactMapper;
import com.jy.eletender.crypto.service.IDecryptArtifactService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
/**
 * `bdc_decrypt_artifact` 状态维护实现。
 */
public class DecryptArtifactServiceImpl implements IDecryptArtifactService {

    private final BdcDecryptArtifactMapper artifactMapper;

    public DecryptArtifactServiceImpl(BdcDecryptArtifactMapper artifactMapper) {
        this.artifactMapper = artifactMapper;
    }

    @Override
    public BdcDecryptArtifact getById(Long artifactId) {
        return artifactMapper.selectById(artifactId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markProcessing(Long artifactId) {
        BdcDecryptArtifact artifact = artifactMapper.selectById(artifactId);
        if (artifact == null) {
            return;
        }
        artifact.setStatus(DecryptArtifactStatus.PROCESSING.name());
        artifact.setProcessingStartedAt(new Date());
        artifact.setErrorMessage(null);
        artifactMapper.updateById(artifact);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markSuccess(Long artifactId,
                            String resultJson,
                            String decryptedPath,
                            String decryptedSha256,
                            String bidRecordDataJson) {
        BdcDecryptArtifact artifact = artifactMapper.selectById(artifactId);
        if (artifact == null) {
            return;
        }
        artifact.setStatus(DecryptArtifactStatus.SUCCESS.name());
        artifact.setDecryptResultJson(resultJson);
        artifact.setDecryptedFilePath(decryptedPath);
        artifact.setDecryptedFileSha256(decryptedSha256);
        artifact.setBidRecordDataJson(bidRecordDataJson);
        artifact.setCompletedAt(new Date());
        artifact.setErrorMessage(null);
        artifactMapper.updateById(artifact);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markFailed(Long artifactId, String errorMessage) {
        BdcDecryptArtifact artifact = artifactMapper.selectById(artifactId);
        if (artifact == null) {
            return;
        }
        artifact.setStatus(DecryptArtifactStatus.FAILED.name());
        artifact.setErrorMessage(errorMessage);
        artifact.setCompletedAt(new Date());
        artifactMapper.updateById(artifact);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetForRetry(Long artifactId) {
        BdcDecryptArtifact artifact = artifactMapper.selectById(artifactId);
        if (artifact == null) {
            return;
        }
        artifact.setStatus(DecryptArtifactStatus.PENDING.name());
        artifact.setErrorMessage(null);
        artifact.setProcessingStartedAt(null);
        artifact.setCompletedAt(null);
        artifact.setRetryCount((artifact.getRetryCount() == null ? 0 : artifact.getRetryCount()) + 1);
        artifactMapper.updateById(artifact);
    }
}
