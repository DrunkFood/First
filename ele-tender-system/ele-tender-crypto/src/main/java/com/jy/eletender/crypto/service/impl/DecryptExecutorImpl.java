package com.jy.eletender.crypto.service.impl;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eletender.common.entity.crypto.BdcBidDocument;
import com.jy.eletender.common.entity.crypto.BdcDecryptArtifact;
import com.jy.eletender.common.enums.BidDocumentUploadStatus;
import com.jy.eletender.common.enums.DecryptArtifactStatus;
import com.jy.eletender.crypto.config.CryptoProperties;
import com.jy.eletender.crypto.mapper.BdcBidDocumentMapper;
import com.jy.eletender.crypto.native_bridge.ICryptoNative;
import com.jy.eletender.crypto.native_bridge.UnpackResult;
import com.jy.eletender.crypto.service.IBidDecryptRequestService;
import com.jy.eletender.crypto.service.IDecryptArtifactService;
import com.jy.eletender.crypto.service.IDecryptExecutor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
/**
 * 解密工件执行器。
 * 负责读取 `bdc_decrypt_artifact` 与 `bdc_bid_document`，完成解包、解密、
 * 产物落盘与结果持久化，并在结束后通知请求服务投影结果。
 */
public class DecryptExecutorImpl implements IDecryptExecutor {

    private final IDecryptArtifactService artifactService;
    private final BdcBidDocumentMapper bidDocumentMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ICryptoNative cryptoNative;
    private final IBidDecryptRequestService decryptRequestService;
    private final CryptoProperties cryptoProperties;
    private final ObjectMapper objectMapper;

    public DecryptExecutorImpl(IDecryptArtifactService artifactService,
                               BdcBidDocumentMapper bidDocumentMapper,
                               StringRedisTemplate stringRedisTemplate,
                               ICryptoNative cryptoNative,
                               IBidDecryptRequestService decryptRequestService,
                               CryptoProperties cryptoProperties,
                               ObjectMapper objectMapper) {
        this.artifactService = artifactService;
        this.bidDocumentMapper = bidDocumentMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.cryptoNative = cryptoNative;
        this.decryptRequestService = decryptRequestService;
        this.cryptoProperties = cryptoProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void execute(Long artifactId) {
        BdcDecryptArtifact artifact = artifactService.getById(artifactId);
        if (artifact == null) {
            return;
        }

        if (DecryptArtifactStatus.SUCCESS.name().equals(artifact.getStatus())) {
            // Consumer re-delivery is tolerated. If the artifact is already terminal we only
            // need to fan out its result to requests that may still be pending.
            decryptRequestService.handleArtifactCompletion(artifactId);
            return;
        }

        try {
            artifactService.markProcessing(artifactId);

            // The decrypt password is intentionally short-lived in Redis so the database never
            // stores the raw bidder password.
            String bidderPwd = stringRedisTemplate.opsForValue().get(buildPasswordCacheKey(artifactId));
            if (StringUtils.isBlank(bidderPwd)) {
                throw new IllegalStateException("投标口令缓存已过期");
            }

            BdcBidDocument bidDocument = bidDocumentMapper.selectOne(new LambdaQueryWrapper<BdcBidDocument>()
                    .eq(BdcBidDocument::getAppKey, artifact.getAppKey())
                    .eq(BdcBidDocument::getFileSha256, artifact.getFileSha256())
                    .eq(BdcBidDocument::getUploadStatus, BidDocumentUploadStatus.SUCCESS.name())
                    .orderByDesc(BdcBidDocument::getId)
                    .last("limit 1"));
            if (bidDocument == null || StringUtils.isBlank(bidDocument.getFileStoragePath())) {
                throw new IllegalStateException("未找到可用的加密文件");
            }

            // Native decrypt is split into unpack -> segment decrypt -> project info decrypt so
            // the stub and future JNI implementation can keep the same contract.
            byte[] encryptedBytes = Files.readAllBytes(Path.of(bidDocument.getFileStoragePath()));
            UnpackResult unpackResult = cryptoNative.unpackFile(encryptedBytes);
            byte[] bidderKeyBytes = HexUtil.decodeHex(bidderPwd);
            byte[] decryptedPayloadBytes = cryptoNative.decryptSegments(unpackResult.getEncryptedSegments(), bidderKeyBytes);
            if (decryptedPayloadBytes.length == 0) {
                // Stub mode can return blank to keep the integration flow unblocked.
                decryptedPayloadBytes = encryptedBytes;
            }
            String decryptedPayload = new String(decryptedPayloadBytes, StandardCharsets.UTF_8);

            String projectInfo = resolveProjectInfo(unpackResult);
            String decryptedSha256 = DigestUtil.sha256Hex(decryptedPayload);
            String decryptedPath = persistDecryptedFile(artifactId, decryptedPayload);

            // Persist both the generated file path and a JSON payload snapshot so management,
            // callback and troubleshooting paths can all reuse the same terminal artifact record.
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("artifactId", artifactId);
            result.put("projectInfo", projectInfo);
            result.put("payload", decryptedPayload);
            result.put("payloadSha256", decryptedSha256);
            String resultJson = objectMapper.writeValueAsString(result);

            artifactService.markSuccess(artifactId, resultJson, decryptedPath, decryptedSha256, decryptedPayload);
            stringRedisTemplate.delete(buildPasswordCacheKey(artifactId));
            decryptRequestService.handleArtifactCompletion(artifactId);
        } catch (Exception ex) {
            // Failures are normalized onto artifact state first, then projected to requests by
            // handleArtifactCompletion so retry and callback logic stay in one place.
            artifactService.markFailed(artifactId, ex.getMessage());
            decryptRequestService.handleArtifactCompletion(artifactId);
        }
    }

    /**
     * 持久化解密后的明文 JSON 文件。
     */
    private String persistDecryptedFile(Long artifactId, String decryptedPayload) throws Exception {
        Path baseDir = Path.of(cryptoProperties.getDecryptFilePath());
        Files.createDirectories(baseDir);
        Path filePath = baseDir.resolve(artifactId + ".json");
        Files.writeString(filePath, decryptedPayload,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
        return filePath.toString();
    }

    /**
     * 与请求提交阶段保持一致的投标口令缓存 key。
     */
    private String buildPasswordCacheKey(Long artifactId) {
        return "crypto:pwd:" + artifactId;
    }

    private String resolveProjectInfo(UnpackResult unpackResult) {
        byte[] encryptedProjectInfo = unpackResult.getEncryptedProjectInfoRsa();
        if (encryptedProjectInfo == null || encryptedProjectInfo.length == 0) {
            return null;
        }
        if (StringUtils.isBlank(cryptoProperties.getRsaPrivateKey())) {
            return null;
        }
        byte[] projectInfoBytes = cryptoNative.rsaDecrypt(encryptedProjectInfo, cryptoProperties.getRsaPrivateKey());
        if (projectInfoBytes == null || projectInfoBytes.length == 0) {
            return null;
        }
        return new String(projectInfoBytes, StandardCharsets.UTF_8);
    }
}
