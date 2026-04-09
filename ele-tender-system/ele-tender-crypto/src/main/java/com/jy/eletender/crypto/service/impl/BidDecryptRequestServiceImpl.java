package com.jy.eletender.crypto.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jy.eletender.common.entity.crypto.BdcBidDocument;
import com.jy.eletender.common.entity.crypto.BdcDecryptArtifact;
import com.jy.eletender.common.entity.crypto.BdcDecryptRequest;
import com.jy.eletender.common.enums.BidDocumentUploadStatus;
import com.jy.eletender.common.enums.CallbackStatus;
import com.jy.eletender.common.enums.DecryptArtifactStatus;
import com.jy.eletender.common.enums.DecryptRequestStatus;
import com.jy.eletender.common.enums.ResponseCode;
import com.jy.eletender.common.exception.BusinessException;
import com.jy.eletender.common.interaction.dto.BidDecryptStatusResponse;
import com.jy.eletender.common.interaction.dto.BidDecryptSubmitRequest;
import com.jy.eletender.common.interaction.dto.BidDecryptSubmitResponse;
import com.jy.eletender.common.logging.TraceConstants;
import com.jy.eletender.crypto.config.CryptoProperties;
import com.jy.eletender.crypto.mapper.BdcBidDocumentMapper;
import com.jy.eletender.crypto.mapper.BdcDecryptArtifactMapper;
import com.jy.eletender.crypto.mapper.BdcDecryptRequestMapper;
import com.jy.eletender.crypto.model.CallbackInvokeResult;
import com.jy.eletender.crypto.service.IBidDecryptRequestService;
import com.jy.eletender.crypto.service.ICallbackService;
import com.jy.eletender.crypto.support.CryptoFingerprintUtil;
import com.jy.eletender.crypto.support.CryptoUserContext;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
/**
 * 解密请求实现。
 * 负责建立“请求记录 + 工件缓存”的双层模型：
 * `bdc_decrypt_request` 记录每次提交与回调结果，
 * `bdc_decrypt_artifact` 负责按文件和口令指纹复用实际解密结果。
 */
public class BidDecryptRequestServiceImpl implements IBidDecryptRequestService {

    private final BdcDecryptRequestMapper requestMapper;
    private final BdcDecryptArtifactMapper artifactMapper;
    private final BdcBidDocumentMapper bidDocumentMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final CryptoProperties cryptoProperties;
    private final ICallbackService callbackService;
    private final OrganizedFileCopyService organizedFileCopyService;

    public BidDecryptRequestServiceImpl(BdcDecryptRequestMapper requestMapper,
                                        BdcDecryptArtifactMapper artifactMapper,
                                        BdcBidDocumentMapper bidDocumentMapper,
                                        StringRedisTemplate stringRedisTemplate,
                                        CryptoProperties cryptoProperties) {
        this(requestMapper, artifactMapper, bidDocumentMapper, stringRedisTemplate, cryptoProperties, null, null);
    }

    @Autowired
    public BidDecryptRequestServiceImpl(BdcDecryptRequestMapper requestMapper,
                                        BdcDecryptArtifactMapper artifactMapper,
                                        BdcBidDocumentMapper bidDocumentMapper,
                                        StringRedisTemplate stringRedisTemplate,
                                        CryptoProperties cryptoProperties,
                                        @Autowired(required = false) ICallbackService callbackService,
                                        @Autowired(required = false) OrganizedFileCopyService organizedFileCopyService) {
        this.requestMapper = requestMapper;
        this.artifactMapper = artifactMapper;
        this.bidDocumentMapper = bidDocumentMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.cryptoProperties = cryptoProperties;
        this.callbackService = callbackService;
        this.organizedFileCopyService = organizedFileCopyService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BidDecryptSubmitResponse submit(CryptoUserContext userContext, BidDecryptSubmitRequest request) {
        return submit(
                userContext.getAppKey(),
                userContext.getUserId(),
                userContext.getUserName(),
                userContext.getEnterpriseId(),
                request
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public BidDecryptSubmitResponse submit(String appKey,
                                           String userId,
                                           String userName,
                                           String enterpriseId,
                                           BidDecryptSubmitRequest request) {
        // 解密请求必须绑定已预存成功的加密文件，取相同 appKey + fileSha256 下最新一条成功记录作为解密源。
        BdcBidDocument bidDocument = bidDocumentMapper.selectOne(new LambdaQueryWrapper<BdcBidDocument>()
                .eq(BdcBidDocument::getAppKey, appKey)
                .eq(BdcBidDocument::getFileSha256, request.getFileSha256())
                .eq(BdcBidDocument::getUploadStatus, BidDocumentUploadStatus.SUCCESS.name())
                .orderByDesc(BdcBidDocument::getId)
                .last("limit 1"));
        if (bidDocument == null) {
            throw new BusinessException(ResponseCode.BID_DOCUMENT_NOT_FOUND);
        }

        String fingerprint = CryptoFingerprintUtil.fingerprint(request.getBidderPwdStr());
        // 工件按"加密文件 + 口令指纹"去重，相同组合的重复提交直接复用已有解密结果，不重跑 native 解密。
        BdcDecryptArtifact artifact = artifactMapper.selectOne(new LambdaQueryWrapper<BdcDecryptArtifact>()
                .eq(BdcDecryptArtifact::getAppKey, appKey)
                .eq(BdcDecryptArtifact::getFileSha256, request.getFileSha256())
                .eq(BdcDecryptArtifact::getBidderPwdFingerprint, fingerprint)
                .last("limit 1"));

        if (artifact == null) {
            artifact = new BdcDecryptArtifact();
            artifact.setAppKey(appKey);
            artifact.setFileSha256(request.getFileSha256());
            artifact.setBidderPwdFingerprint(fingerprint);
            artifact.setStatus(DecryptArtifactStatus.PENDING.name());
            artifact.setRetryCount(0);
            artifactMapper.insert(artifact);
        }

        BdcDecryptRequest decryptRequest = new BdcDecryptRequest();
        decryptRequest.setAppKey(appKey);
        decryptRequest.setProjectId(request.getProjectId());
        decryptRequest.setTenderId(request.getTenderId());
        decryptRequest.setBidRecordId(request.getBidRecordId());
        decryptRequest.setFileSha256(request.getFileSha256());
        decryptRequest.setArtifactId(artifact.getId());
        decryptRequest.setStatus(DecryptRequestStatus.PENDING.name());
        decryptRequest.setCallbackStatus(CallbackStatus.NOT_CALLED.name());
        decryptRequest.setCallbackRetryCount(0);
        decryptRequest.setRequestedByUserId(userId);
        decryptRequest.setRequestedByUserName(userName);
        decryptRequest.setRequestedByEnterpriseId(enterpriseId);
        requestMapper.insert(decryptRequest);

        // 原始口令只在异步解密窗口内存入 Redis，不落库。
        cachePassword(artifact.getId(), request.getBidderPwdStr());

        if (DecryptArtifactStatus.SUCCESS.name().equals(artifact.getStatus())
                || DecryptArtifactStatus.FAILED.name().equals(artifact.getStatus())) {
            // 复用已终态的工件：直接将解密结果投影到新建的请求并触发回调。
            handleArtifactCompletion(artifact.getId());
        } else {
            pushArtifactTask(artifact.getId());
        }

        BidDecryptSubmitResponse response = new BidDecryptSubmitResponse();
        response.setRecordId(String.valueOf(decryptRequest.getId()));
        return response;
    }

    @Override
    public BidDecryptStatusResponse queryStatus(String appKey, String recordId) {
        long id;
        try {
            id = Long.parseLong(recordId);
        } catch (NumberFormatException ex) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "recordId格式错误");
        }

        BdcDecryptRequest request = requestMapper.selectById(id);
        if (request == null || !appKey.equals(request.getAppKey())) {
            throw new BusinessException(ResponseCode.BID_DECRYPT_REQUEST_NOT_FOUND);
        }

        BidDecryptStatusResponse response = new BidDecryptStatusResponse();
        response.setRecordId(String.valueOf(request.getId()));
        response.setStatus(request.getStatus());
        response.setErrorMessage(request.getErrorMessage());
        response.setCompletedAt(request.getCompletedAt());
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleArtifactCompletion(Long artifactId) {
        BdcDecryptArtifact artifact = artifactMapper.selectById(artifactId);
        if (artifact == null) {
            return;
        }

        if (!DecryptArtifactStatus.SUCCESS.name().equals(artifact.getStatus())
                && !DecryptArtifactStatus.FAILED.name().equals(artifact.getStatus())) {
            return;
        }

        List<BdcDecryptRequest> requests = requestMapper.selectList(new LambdaQueryWrapper<BdcDecryptRequest>()
                .eq(BdcDecryptRequest::getArtifactId, artifactId)
                .in(BdcDecryptRequest::getStatus, DecryptRequestStatus.PENDING.name(), DecryptRequestStatus.PROCESSING.name()));

        // 一个工件可对应多条提交记录（相同文件+口令复用），终态结果投影到所有进行中的请求。
        for (BdcDecryptRequest request : requests) {
            request.setStatus(DecryptArtifactStatus.SUCCESS.name().equals(artifact.getStatus())
                    ? DecryptRequestStatus.SUCCESS.name()
                    : DecryptRequestStatus.FAILED.name());
            request.setCompletedAt(artifact.getCompletedAt() == null ? new Date() : artifact.getCompletedAt());
            request.setErrorMessage(artifact.getErrorMessage());
            request.setCallbackStatus(CallbackStatus.NOT_CALLED.name());

            // 将加密文件和解密文件复制到项目/标段组织目录，复制失败不阻塞主流程（组织视图为辅助）。
            if (DecryptRequestStatus.SUCCESS.name().equals(request.getStatus())
                    && organizedFileCopyService != null) {
                BdcBidDocument bidDoc = bidDocumentMapper.selectOne(
                        new LambdaQueryWrapper<BdcBidDocument>()
                                .eq(BdcBidDocument::getAppKey, artifact.getAppKey())
                                .eq(BdcBidDocument::getFileSha256, artifact.getFileSha256())
                                .eq(BdcBidDocument::getUploadStatus, BidDocumentUploadStatus.SUCCESS.name())
                                .orderByDesc(BdcBidDocument::getId)
                                .last("limit 1"));
                if (bidDoc != null) {
                    request.setOrganizedEncryptedFilePath(
                            organizedFileCopyService.copyEncryptedFile(request, bidDoc.getFileStoragePath()));
                }
                if (artifact.getDecryptedFilePath() != null) {
                    request.setOrganizedDecryptedFilePath(
                            organizedFileCopyService.copyDecryptedFile(request, artifact.getDecryptedFilePath()));
                }
            }

            if (callbackService != null) {
                // 回调执行结果仅用于重试/审计字段，不作为对调用方的 API 响应。
                CallbackInvokeResult callbackResult = callbackService.callbackDecryptResult(
                        request,
                        artifact,
                        MDC.get(TraceConstants.TRACE_ID_MDC_KEY)
                );
                request.setCallbackResponseCode(callbackResult.getResponseCode());
                request.setCallbackResponseMessage(callbackResult.getResponseMessage());
                request.setCallbackTime(new Date());
                if (callbackResult.isSuccess()) {
                    request.setCallbackStatus(CallbackStatus.SUCCESS.name());
                } else {
                    request.setCallbackStatus(CallbackStatus.FAILED.name());
                    request.setCallbackRetryCount((request.getCallbackRetryCount() == null ? 0 : request.getCallbackRetryCount()) + 1);
                }
            }
            requestMapper.updateById(request);
        }
    }

    /**
     * 仅在异步解密窗口内缓存原始投标口令，避免落库明文密码。
     */
    private void cachePassword(Long artifactId, String bidderPwdStr) {
        String key = "crypto:pwd:" + artifactId;
        stringRedisTemplate.opsForValue().set(key, bidderPwdStr, Duration.ofSeconds(cryptoProperties.getPasswordCacheSeconds()));
    }

    /**
     * 向 Redis Stream 投递工件任务，由异步消费者执行真正解密。
     */
    private void pushArtifactTask(Long artifactId) {
        stringRedisTemplate.opsForStream().add(StreamRecords.newRecord()
                .in(cryptoProperties.getStream().getKey())
                .ofMap(Map.of("artifactId", String.valueOf(artifactId))));
    }
}
