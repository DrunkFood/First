package com.jy.eletender.support.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eletender.common.constant.CommonConstant;
import com.jy.eletender.common.entity.crypto.BdcDecryptArtifact;
import com.jy.eletender.common.entity.crypto.BdcDecryptRequest;
import com.jy.eletender.common.enums.CallbackStatus;
import com.jy.eletender.common.enums.DecryptArtifactStatus;
import com.jy.eletender.common.enums.DecryptRequestStatus;
import com.jy.eletender.common.enums.ResponseCode;
import com.jy.eletender.common.exception.BusinessException;
import com.jy.eletender.support.config.CryptoAdminProperties;
import com.jy.eletender.support.mapper.BdcDecryptArtifactMapper;
import com.jy.eletender.support.mapper.BdcDecryptRequestMapper;
import com.jy.eletender.support.service.ICryptoManageService;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
public class CryptoManageServiceImpl implements ICryptoManageService {

    private final BdcDecryptRequestMapper requestMapper;
    private final BdcDecryptArtifactMapper artifactMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final CryptoAdminProperties cryptoAdminProperties;

    public CryptoManageServiceImpl(BdcDecryptRequestMapper requestMapper,
                                   BdcDecryptArtifactMapper artifactMapper,
                                   StringRedisTemplate stringRedisTemplate,
                                   CryptoAdminProperties cryptoAdminProperties) {
        this.requestMapper = requestMapper;
        this.artifactMapper = artifactMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.cryptoAdminProperties = cryptoAdminProperties;
    }

    @Override
    public Page<BdcDecryptRequest> getRequestPage(Integer pageNum,
                                                  Integer pageSize,
                                                  String appKey,
                                                  String projectId,
                                                  String tenderId,
                                                  String bidRecordId,
                                                  String status,
                                                  String callbackStatus) {
        long current = pageNum == null || pageNum < 1 ? 1L : pageNum.longValue();
        long size = resolvePageSize(pageSize);

        LambdaQueryWrapper<BdcDecryptRequest> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(appKey)) {
            wrapper.eq(BdcDecryptRequest::getAppKey, appKey);
        }
        if (StringUtils.hasText(projectId)) {
            wrapper.eq(BdcDecryptRequest::getProjectId, projectId);
        }
        if (StringUtils.hasText(tenderId)) {
            wrapper.eq(BdcDecryptRequest::getTenderId, tenderId);
        }
        if (StringUtils.hasText(bidRecordId)) {
            wrapper.eq(BdcDecryptRequest::getBidRecordId, bidRecordId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(BdcDecryptRequest::getStatus, status);
        }
        if (StringUtils.hasText(callbackStatus)) {
            wrapper.eq(BdcDecryptRequest::getCallbackStatus, callbackStatus);
        }
        wrapper.orderByDesc(BdcDecryptRequest::getCreateTime);

        return requestMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public BdcDecryptArtifact getArtifactDetail(Long artifactId) {
        BdcDecryptArtifact artifact = artifactMapper.selectById(artifactId);
        if (artifact == null) {
            throw new BusinessException(ResponseCode.BID_DECRYPT_ARTIFACT_NOT_FOUND);
        }
        return artifact;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryRequest(Long requestId) {
        BdcDecryptRequest request = requestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException(ResponseCode.BID_DECRYPT_REQUEST_NOT_FOUND);
        }

        BdcDecryptArtifact artifact = artifactMapper.selectById(request.getArtifactId());
        if (artifact == null) {
            throw new BusinessException(ResponseCode.BID_DECRYPT_ARTIFACT_NOT_FOUND);
        }

        request.setStatus(DecryptRequestStatus.PENDING.name());
        request.setErrorMessage(null);
        request.setCompletedAt(null);
        request.setCallbackStatus(CallbackStatus.NOT_CALLED.name());
        request.setCallbackResponseCode(null);
        request.setCallbackResponseMessage(null);
        request.setCallbackTime(null);
        requestMapper.updateById(request);

        artifact.setStatus(DecryptArtifactStatus.PENDING.name());
        artifact.setErrorMessage(null);
        artifact.setCompletedAt(null);
        artifact.setProcessingStartedAt(null);
        artifact.setRetryCount((artifact.getRetryCount() == null ? 0 : artifact.getRetryCount()) + 1);
        artifactMapper.updateById(artifact);

        stringRedisTemplate.opsForStream().add(StreamRecords.newRecord()
                .in(cryptoAdminProperties.getStreamKey())
                .ofMap(Map.of("artifactId", String.valueOf(artifact.getId()))));
    }

    private long resolvePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return CommonConstant.DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize.intValue(), CommonConstant.MAX_PAGE_SIZE);
    }
}
