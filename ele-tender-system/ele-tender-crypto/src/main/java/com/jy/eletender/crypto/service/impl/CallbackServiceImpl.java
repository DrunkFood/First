package com.jy.eletender.crypto.service.impl;

import com.jy.eletender.common.entity.crypto.BdcDecryptArtifact;
import com.jy.eletender.common.entity.crypto.BdcDecryptRequest;
import com.jy.eletender.common.entity.support.SysAccessSystem;
import com.jy.eletender.common.interaction.constant.InteractionApiPaths;
import com.jy.eletender.common.interaction.constant.InteractionHeaderConstants;
import com.jy.eletender.common.interaction.dto.BidDecryptResultCallbackRequest;
import com.jy.eletender.common.interaction.dto.BidDocumentResultCallbackRequest;
import com.jy.eletender.common.interaction.dto.InteractionResult;
import com.jy.eletender.common.logging.TraceConstants;
import com.jy.eletender.common.util.SignatureUtil;
import com.jy.eletender.crypto.mapper.SysAccessSystemReadMapper;
import com.jy.eletender.crypto.model.CallbackInvokeResult;
import com.jy.eletender.crypto.service.ICallbackService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Date;

@Service
/**
 * crypto 回调服务。
 * 负责把 `bdc_bid_document` / `bdc_decrypt_request` / `bdc_decrypt_artifact` 的终态结果
 * 转换为业务系统可消费的回调协议，并完成签名与 HTTP 调用。
 */
public class CallbackServiceImpl implements ICallbackService {

    private final SysAccessSystemReadMapper accessSystemReadMapper;
    private final RestTemplate cryptoRestTemplate;

    public CallbackServiceImpl(SysAccessSystemReadMapper accessSystemReadMapper,
                               RestTemplate cryptoRestTemplate) {
        this.accessSystemReadMapper = accessSystemReadMapper;
        this.cryptoRestTemplate = cryptoRestTemplate;
    }

    @Override
    public CallbackInvokeResult callbackBidDocumentResult(String appKey, Long fileId, String uploadResult, String traceId) {
        SysAccessSystem system = resolveSystem(appKey);

        BidDocumentResultCallbackRequest request = new BidDocumentResultCallbackRequest();
        request.setFileId(fileId);
        request.setUploadResult(uploadResult);

        return invokeCallback(system, InteractionApiPaths.CALLBACK_BID_DOCUMENT_RESULT, request, traceId);
    }

    @Override
    public CallbackInvokeResult callbackDecryptResult(BdcDecryptRequest request,
                                                      BdcDecryptArtifact artifact,
                                                      String traceId) {
        SysAccessSystem system = resolveSystem(request.getAppKey());

        // request 负责“这次提交”的业务上下文，artifact 负责“这次解密产物”的结果载荷。
        // 两者合并后，业务系统才能拿到完整终态视图。
        BidDecryptResultCallbackRequest callbackRequest = new BidDecryptResultCallbackRequest();
        callbackRequest.setProjectId(request.getProjectId());
        callbackRequest.setTenderId(request.getTenderId());
        callbackRequest.setBidRecordId(request.getBidRecordId());
        callbackRequest.setStatus(request.getStatus());
        callbackRequest.setErrorMessage(request.getErrorMessage());
        callbackRequest.setBidRecordData(artifact.getBidRecordDataJson());

        return invokeCallback(system, InteractionApiPaths.CALLBACK_BID_DECRYPT_RESULT, callbackRequest, traceId);
    }

    private CallbackInvokeResult invokeCallback(SysAccessSystem system, String path, Object request, String traceId) {
        long timestamp = System.currentTimeMillis();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(InteractionHeaderConstants.APP_KEY, system.getAppKey());
        headers.set(InteractionHeaderConstants.TIMESTAMP, String.valueOf(timestamp));
        headers.set(InteractionHeaderConstants.SIGNATURE,
                SignatureUtil.generateSignature(system.getAppKey(), timestamp, system.getAppSecret()));
        if (StringUtils.isNotBlank(traceId)) {
            headers.set(TraceConstants.TRACE_ID_HEADER, traceId);
        }

        String requestUrl = trimTrailingSlash(system.getSystemUrl()) + path;
        try {
            // 远端回调协议仍是 InteractionResult；
            // 本地持久化和重试逻辑则统一折叠为 CallbackInvokeResult。
            ResponseEntity<InteractionResult<Void>> response = cryptoRestTemplate.exchange(
                    requestUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    new ParameterizedTypeReference<InteractionResult<Void>>() {
                    }
            );
            InteractionResult<Void> body = response.getBody();
            if (body != null && body.isSuccess()) {
                return CallbackInvokeResult.success(String.valueOf(response.getStatusCode().value()), body.getMessage());
            }
            String message = body == null ? "空响应体" : body.getMessage();
            return CallbackInvokeResult.fail(String.valueOf(response.getStatusCode().value()), message);
        } catch (RestClientException ex) {
            return CallbackInvokeResult.fail("HTTP_EXCEPTION", ex.getMessage());
        }
    }

    private SysAccessSystem resolveSystem(String appKey) {
        // 回调依赖 support 中登记的业务系统配置，这里统一做可用性与有效期校验。
        if (StringUtils.isBlank(appKey)) {
            throw new IllegalStateException("缺少appKey，无法回调业务系统");
        }
        SysAccessSystem system = accessSystemReadMapper.selectByAppKey(appKey);
        if (system == null) {
            throw new IllegalStateException("未找到业务系统配置: " + appKey);
        }
        if (system.getStatus() == null || system.getStatus().intValue() != 1) {
            throw new IllegalStateException("业务系统已禁用: " + appKey);
        }
        if (system.getExpireTime() != null && system.getExpireTime().before(new Date())) {
            throw new IllegalStateException("业务系统已过期: " + appKey);
        }
        if (StringUtils.isAnyBlank(system.getSystemUrl(), system.getAppSecret())) {
            throw new IllegalStateException("业务系统配置不完整: " + appKey);
        }
        return system;
    }

    private String trimTrailingSlash(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
