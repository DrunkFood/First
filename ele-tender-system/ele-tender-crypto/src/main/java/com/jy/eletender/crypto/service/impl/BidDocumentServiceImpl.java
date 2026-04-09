package com.jy.eletender.crypto.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jy.eletender.common.constant.FileConstants;
import com.jy.eletender.common.entity.crypto.BdcBidDocument;
import com.jy.eletender.common.entity.file.FileInfo;
import com.jy.eletender.common.entity.support.SysAccessSystem;
import com.jy.eletender.common.enums.BidDocumentUploadStatus;
import com.jy.eletender.common.enums.CallbackStatus;
import com.jy.eletender.common.enums.ResponseCode;
import com.jy.eletender.common.exception.BusinessException;
import com.jy.eletender.common.interaction.dto.BidDocumentPushRequest;
import com.jy.eletender.common.interaction.dto.BidDocumentPushResponse;
import com.jy.eletender.common.logging.TraceConstants;
import com.jy.eletender.common.response.Result;
import com.jy.eletender.crypto.config.CryptoProperties;
import com.jy.eletender.crypto.mapper.BdcBidDocumentMapper;
import com.jy.eletender.crypto.mapper.SysAccessSystemReadMapper;
import com.jy.eletender.crypto.model.CallbackInvokeResult;
import com.jy.eletender.crypto.service.IBidDocumentService;
import com.jy.eletender.crypto.service.ICallbackService;
import com.jy.eletender.crypto.support.CryptoUserContext;
import org.slf4j.MDC;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Date;

@Service
/**
 * 投标文件预存实现。
 * 职责包括下载文件服务中的加密文件、校验 SHA-256、落盘、写入 `bdc_bid_document`
 * 以及向业务系统回调预存结果。
 */
public class BidDocumentServiceImpl implements IBidDocumentService {

    private final BdcBidDocumentMapper bidDocumentMapper;
    private final RestTemplate cryptoRestTemplate;
    private final CryptoProperties cryptoProperties;
    private final ICallbackService callbackService;
    private final SysAccessSystemReadMapper sysAccessSystemReadMapper;

    public BidDocumentServiceImpl(BdcBidDocumentMapper bidDocumentMapper,
                              RestTemplate cryptoRestTemplate,
                              CryptoProperties cryptoProperties,
                              ICallbackService callbackService,
                              SysAccessSystemReadMapper sysAccessSystemReadMapper) {
        this.bidDocumentMapper = bidDocumentMapper;
        this.cryptoRestTemplate = cryptoRestTemplate;
        this.cryptoProperties = cryptoProperties;
        this.callbackService = callbackService;
        this.sysAccessSystemReadMapper = sysAccessSystemReadMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BidDocumentPushResponse pushBidDocument(CryptoUserContext userContext, BidDocumentPushRequest request) {
        byte[] encryptedFile = downloadFile(request.getFileId());
        String actualSha256 = DigestUtil.sha256Hex(encryptedFile);
        if (!actualSha256.equalsIgnoreCase(request.getFileSha256())) {
            throw new BusinessException(ResponseCode.BID_DOCUMENT_SHA256_MISMATCH);
        }

        FileInfo fileInfo = queryFileInfo(request.getFileId());
        String bidDocSuffix = resolveBidDocumentSuffix(userContext.getAppKey());
        Path storagePath = persistEncryptedFile(userContext.getAppKey(), request.getFileSha256(), encryptedFile, bidDocSuffix);

        BdcBidDocument bidDocument = bidDocumentMapper.selectOne(new LambdaQueryWrapper<BdcBidDocument>()
                .eq(BdcBidDocument::getAppKey, userContext.getAppKey())
                .eq(BdcBidDocument::getFileId, request.getFileId())
                .last("limit 1"));
        if (bidDocument == null) {
            bidDocument = new BdcBidDocument();
        }

        bidDocument.setAppKey(userContext.getAppKey());
        bidDocument.setUserId(userContext.getUserId());
        bidDocument.setFileId(request.getFileId());
        bidDocument.setFileName(fileInfo == null ? null : fileInfo.getFileName());
        bidDocument.setFileSha256(request.getFileSha256());
        bidDocument.setFileStoragePath(storagePath.toString());
        bidDocument.setFileSize((long) encryptedFile.length);
        bidDocument.setUploadStatus(BidDocumentUploadStatus.SUCCESS.name());
        bidDocument.setCallbackStatus(CallbackStatus.NOT_CALLED.name());

        if (bidDocument.getId() == null) {
            bidDocumentMapper.insert(bidDocument);
        } else {
            bidDocumentMapper.updateById(bidDocument);
        }

        CallbackInvokeResult callbackResult = callbackService.callbackBidDocumentResult(
                userContext.getAppKey(),
                request.getFileId(),
                BidDocumentUploadStatus.SUCCESS.name(),
                MDC.get(TraceConstants.TRACE_ID_MDC_KEY)
        );
        bidDocument.setCallbackTime(new Date());
        bidDocument.setCallbackStatus(callbackResult.isSuccess() ? CallbackStatus.SUCCESS.name() : CallbackStatus.FAILED.name());
        bidDocument.setCallbackErrorMessage(callbackResult.getResponseMessage());
        bidDocumentMapper.updateById(bidDocument);

        BidDocumentPushResponse response = new BidDocumentPushResponse();
        response.setFileId(request.getFileId());
        response.setUploadResult(BidDocumentUploadStatus.SUCCESS.name());
        return response;
    }

    /**
     * 从文件服务下载原始加密投标文件内容。
     */
    private byte[] downloadFile(Long fileId) {
        String url = cryptoProperties.getFileServiceBaseUrl() + cryptoProperties.getFileDownloadPath().replace("{fileId}", String.valueOf(fileId));
        ResponseEntity<byte[]> response = cryptoRestTemplate.getForEntity(url, byte[].class);
        byte[] body = response.getBody();
        if (body == null || body.length == 0) {
            throw new BusinessException(ResponseCode.BID_DOCUMENT_NOT_FOUND);
        }
        return body;
    }

    /**
     * 查询文件服务中的文件元信息，用于补齐 `bdc_bid_document.file_name` 等字段。
     */
    private FileInfo queryFileInfo(Long fileId) {
        String url = cryptoProperties.getFileServiceBaseUrl() + cryptoProperties.getFileInfoPath().replace("{fileId}", String.valueOf(fileId));
        ResponseEntity<Result<FileInfo>> response = cryptoRestTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Result<FileInfo>>() {
                }
        );
        Result<FileInfo> result = response.getBody();
        if (result == null || !result.isSuccess()) {
            return null;
        }
        return result.getData();
    }

    /**
     * 按 appKey + sha256 分桶目录落盘加密投标文件。
     */
    private Path persistEncryptedFile(String appKey, String fileSha256, byte[] content, String suffix) {
        try {
            Path baseDir = Path.of(
                    cryptoProperties.getBidDocumentPath(),
                    appKey,
                    fileSha256.substring(0, 2),
                    fileSha256.substring(2, 4),
                    fileSha256.substring(4, 6)
            );
            Files.createDirectories(baseDir);
            Path filePath = baseDir.resolve(fileSha256 + suffix);
            Files.write(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            return filePath;
        } catch (Exception ex) {
            throw new BusinessException(ResponseCode.BID_DOCUMENT_PUSH_PARAM_ERROR.getCode(), "加密文件落盘失败: " + ex.getMessage());
        }
    }

    private String resolveBidDocumentSuffix(String appKey) {
        if (appKey == null) {
            return FileConstants.DEFAULT_BID_DOCUMENT_SUFFIX;
        }
        SysAccessSystem system = sysAccessSystemReadMapper.selectByAppKey(appKey);
        if (system != null && system.getBidDocumentSuffix() != null && !system.getBidDocumentSuffix().isBlank()) {
            return system.getBidDocumentSuffix();
        }
        return FileConstants.DEFAULT_BID_DOCUMENT_SUFFIX;
    }
}
