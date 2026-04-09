package com.jy.eletender.tenderdocument.support.interaction;

import com.jy.eletender.common.interaction.constant.InteractionApiPaths;
import com.jy.eletender.common.interaction.constant.InteractionHeaderConstants;
import com.jy.eletender.common.interaction.dto.BidRecordSchemeQueryRequest;
import com.jy.eletender.common.interaction.dto.BidRecordSchemeResponse;
import com.jy.eletender.common.interaction.dto.InteractionResult;
import com.jy.eletender.common.interaction.dto.ProjectBasicInfoQueryRequest;
import com.jy.eletender.common.interaction.dto.ProjectBasicInfoResponse;
import com.jy.eletender.common.interaction.dto.CaKeysInfoQueryRequest;
import com.jy.eletender.common.interaction.dto.CaKeysInfoResponse;
import com.jy.eletender.common.interaction.dto.TenderPackageCallbackRequest;
import com.jy.eletender.common.interaction.dto.TenderPdfCallbackRequest;
import com.jy.eletender.common.entity.support.SysAccessLog;
import com.jy.eletender.common.logging.AccessLogPersistenceService;
import com.jy.eletender.common.logging.SensitiveLogMasker;
import com.jy.eletender.common.logging.TraceConstants;
import com.jy.eletender.common.util.SignatureUtil;
import com.jy.eletender.tenderdocument.enums.TenderDocumentErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 业务系统远程调用客户端。
 * 统一处理签名头构造、调用异常转换与交互日志记录。
 */
@Slf4j
@Component
public class BusinessSystemRemoteClient {

    private static final String LOG_TYPE_INTERACTION_OUT = "INTERACTION_OUT";
    private final RestTemplate restTemplate;
    private final ExternalSystemAccessResolver externalSystemAccessResolver;
    private final AccessLogPersistenceService accessLogPersistenceService;
    private final String serviceName;

    @Autowired
    public BusinessSystemRemoteClient(RestTemplate tenderDocumentInteractionRestTemplate,
                                      ExternalSystemAccessResolver externalSystemAccessResolver,
                                      ObjectProvider<AccessLogPersistenceService> accessLogPersistenceServiceProvider,
                                      Environment environment) {
        this(tenderDocumentInteractionRestTemplate,
                externalSystemAccessResolver,
                accessLogPersistenceServiceProvider == null ? null : accessLogPersistenceServiceProvider.getIfAvailable(),
                environment == null ? "unknown-service" : environment.getProperty("spring.application.name", "unknown-service"));
    }

    private BusinessSystemRemoteClient(RestTemplate tenderDocumentInteractionRestTemplate,
                                       ExternalSystemAccessResolver externalSystemAccessResolver,
                                       AccessLogPersistenceService accessLogPersistenceService,
                                       String serviceName) {
        this.restTemplate = tenderDocumentInteractionRestTemplate;
        this.externalSystemAccessResolver = externalSystemAccessResolver;
        this.accessLogPersistenceService = accessLogPersistenceService;
        this.serviceName = StringUtils.defaultIfBlank(serviceName, "unknown-service");
    }

    /**
     * 查询项目基础信息。
     */
    public ProjectBasicInfoResponse queryProjectBasicInfo(String appKey, String traceId, String authorization, ProjectBasicInfoQueryRequest request) {
        return postForData(appKey, traceId, authorization, InteractionApiPaths.PROJECT_BASIC_INFO, request,
                new ParameterizedTypeReference<InteractionResult<ProjectBasicInfoResponse>>() {
                }, "projects/basic-info");
    }

    /**
     * 查询开标标录方案。
     */
    public BidRecordSchemeResponse queryBidRecordScheme(String appKey, String traceId, String authorization, BidRecordSchemeQueryRequest request) {
        return postForData(appKey, traceId, authorization, InteractionApiPaths.BID_RECORD_SCHEME, request,
                new ParameterizedTypeReference<InteractionResult<BidRecordSchemeResponse>>() {
                }, "bid-record-schemes/query");
    }

    /**
     * 查询开标时有效的 CA 锁信息。
     */
    public CaKeysInfoResponse queryCaKeysInfo(String appKey, String traceId, String authorization, CaKeysInfoQueryRequest request) {
        return postForData(appKey, traceId, authorization, InteractionApiPaths.CA_KEYS_INFO, request,
                new ParameterizedTypeReference<InteractionResult<CaKeysInfoResponse>>() {
                }, "ca-keys/query");
    }

    public void callbackTenderPdf(String appKey, String traceId, TenderPdfCallbackRequest request) {
        postWithoutData(appKey, traceId, InteractionApiPaths.CALLBACK_TENDER_PDF, request, "callbacks/tender-pdf");
    }

    public void callbackTenderPackage(String appKey, String traceId, TenderPackageCallbackRequest request) {
        postWithoutData(appKey, traceId, InteractionApiPaths.CALLBACK_TENDER_PACKAGE, request, "callbacks/tender-package");
    }

    private <T> T postForData(String appKey,
                              String traceId,
                              String authorization,
                              String path,
                              Object request,
                              ParameterizedTypeReference<InteractionResult<T>> responseType,
                              String apiName) {
        ResolvedExternalSystem system = externalSystemAccessResolver.resolve(appKey);
        long startTime = System.currentTimeMillis();
        String requestUri = system.getBaseUrl() + path;
        HttpHeaders headers = buildSignedHeaders(system, traceId, authorization);
        try {
            // 统一在客户端层封装签名头与鉴权头，业务调用方只关心请求对象。
            HttpEntity<Object> entity = new HttpEntity<Object>(request, headers);
            ResponseEntity<InteractionResult<T>> response = restTemplate.exchange(
                    requestUri,
                    HttpMethod.POST,
                    entity,
                    responseType
            );
            InteractionResult<T> body = response.getBody();
            // 交互协议同时要求 success=true 且 data 非空，任一不满足都按失败处理。
            if (body == null || !body.isSuccess() || body.getData() == null) {
                persistInteractionOutLog(traceId, appKey, request, headers, requestUri, response.getStatusCodeValue(),
                        false, body == null ? null : body.getMessage(), null, body == null ? "空响应体" : body.getMessage(),
                        System.currentTimeMillis() - startTime);
                throw integrationFailure(apiName, body == null ? null : body.getMessage());
            }
            log.info("TENDER_DOCUMENT OUT traceId={} api={} success=true appKey={} projectId={} tenderId={}",
                    traceId, apiName, appKey, extractField(request, "projectId"), extractField(request, "tenderId"));
            persistInteractionOutLog(traceId, appKey, request, headers, requestUri, response.getStatusCodeValue(),
                    true, body == null ? null : body.getMessage(), null, null, System.currentTimeMillis() - startTime);
            return body.getData();
        } catch (RestClientException ex) {
            persistInteractionOutLog(traceId, appKey, request, headers, requestUri, null,
                    false, null, ex.getClass().getSimpleName(), ex.getMessage(), System.currentTimeMillis() - startTime);
            throw integrationFailure(apiName, ex.getMessage());
        }
    }

    private void postWithoutData(String appKey, String traceId, String path, Object request, String apiName) {
        ResolvedExternalSystem system = externalSystemAccessResolver.resolve(appKey);
        long startTime = System.currentTimeMillis();
        String requestUri = system.getBaseUrl() + path;
        HttpHeaders headers = buildSignedHeaders(system, traceId, null);
        try {
            HttpEntity<Object> entity = new HttpEntity<Object>(request, headers);
            ResponseEntity<InteractionResult<Void>> response = restTemplate.exchange(
                    requestUri,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<InteractionResult<Void>>() {
                    }
            );
            InteractionResult<Void> body = response.getBody();
            // 回传接口只校验 success，不要求 data 字段。
            if (body == null || !body.isSuccess()) {
                persistInteractionOutLog(traceId, appKey, request, headers, requestUri, response.getStatusCodeValue(),
                        false, body == null ? null : body.getMessage(), null, body == null ? "空响应体" : body.getMessage(),
                        System.currentTimeMillis() - startTime);
                throw integrationFailure(apiName, body == null ? null : body.getMessage());
            }
            log.info("TENDER_DOCUMENT OUT traceId={} api={} success=true appKey={} projectId={} tenderId={} fileId={}",
                    traceId, apiName, appKey, extractField(request, "projectId"), extractField(request, "tenderId"), extractField(request, "fileId"));
            persistInteractionOutLog(traceId, appKey, request, headers, requestUri, response.getStatusCodeValue(),
                    true, body == null ? null : body.getMessage(), null, null, System.currentTimeMillis() - startTime);
        } catch (RestClientException ex) {
            persistInteractionOutLog(traceId, appKey, request, headers, requestUri, null,
                    false, null, ex.getClass().getSimpleName(), ex.getMessage(), System.currentTimeMillis() - startTime);
            throw integrationFailure(apiName, ex.getMessage());
        }
    }

    private HttpHeaders buildSignedHeaders(ResolvedExternalSystem system, String traceId, String authorization) {
        long timestamp = System.currentTimeMillis();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(InteractionHeaderConstants.APP_KEY, system.getAppKey());
        headers.set(InteractionHeaderConstants.TIMESTAMP, String.valueOf(timestamp));
        // 签名基于 appKey + timestamp + appSecret，确保业务系统可验签请求来源。
        headers.set(InteractionHeaderConstants.SIGNATURE,
                SignatureUtil.generateSignature(system.getAppKey(), timestamp, system.getAppSecret()));
        if (traceId != null && !traceId.trim().isEmpty()) {
            headers.set(TraceConstants.TRACE_ID_HEADER, traceId);
        }
        if (StringUtils.isNotBlank(authorization)) {
            headers.set(InteractionHeaderConstants.AUTHORIZATION, authorization);
        }
        return headers;
    }

    private RuntimeException integrationFailure(String apiName, String detail) {
        String message = "调用业务系统接口失败[" + apiName + "]" + (detail == null || detail.trim().isEmpty() ? "" : ": " + detail);
        return new com.jy.eletender.common.exception.BusinessException(
                TenderDocumentErrorCode.BUSINESS_SYSTEM_INTERACTION_FAILED.getCode(),
                message
        );
    }

    private Object extractField(Object request, String fieldName) {
        try {
            java.beans.PropertyDescriptor descriptor = new java.beans.PropertyDescriptor(fieldName, request.getClass());
            return descriptor.getReadMethod().invoke(request);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void persistInteractionOutLog(String traceId,
                                          String appKey,
                                          Object request,
                                          HttpHeaders headers,
                                          String requestUri,
                                          Integer statusCode,
                                          boolean success,
                                          String responseMessage,
                                          String errorType,
                                          String errorMessage,
                                          long elapsedMs) {
        if (accessLogPersistenceService == null) {
            return;
        }
        SysAccessLog accessLog = new SysAccessLog();
        accessLog.setTraceId(traceId);
        accessLog.setServiceName(serviceName);
        accessLog.setLogType(LOG_TYPE_INTERACTION_OUT);
        accessLog.setHttpMethod(HttpMethod.POST.name());
        accessLog.setRequestUri(requestUri);
        accessLog.setStatusCode(statusCode);
        accessLog.setSuccessFlag(success ? 1 : 0);
        accessLog.setElapsedMs(elapsedMs);
        accessLog.setAppKey(appKey);
        accessLog.setBizType(toNullableString(extractField(request, "bizType")));
        accessLog.setBizId(toNullableString(extractField(request, "bizId")));
        accessLog.setProjectId(toNullableString(extractField(request, "projectId")));
        accessLog.setTenderId(toNullableString(extractField(request, "tenderId")));
        accessLog.setFileId(toNullableString(extractField(request, "fileId")));
        accessLog.setFileName(toNullableString(extractField(request, "fileName")));
        accessLog.setRequestHeaders(buildRequestHeaderSummary(headers));
        accessLog.setRequestBody(request == null ? null : request.getClass().getSimpleName());
        accessLog.setResponseBody(limit(responseMessage, 2000));
        accessLog.setErrorType(errorType);
        accessLog.setErrorMessage(limit(errorMessage, 1000));
        accessLogPersistenceService.persist(accessLog);
    }

    private String buildRequestHeaderSummary(HttpHeaders headers) {
        if (headers == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        appendHeader(builder, InteractionHeaderConstants.APP_KEY, headers.getFirst(InteractionHeaderConstants.APP_KEY));
        appendHeader(builder, InteractionHeaderConstants.TIMESTAMP, headers.getFirst(InteractionHeaderConstants.TIMESTAMP));
        appendHeader(builder, InteractionHeaderConstants.SIGNATURE, SensitiveLogMasker.maskSecret(headers.getFirst(InteractionHeaderConstants.SIGNATURE)));
        appendHeader(builder, InteractionHeaderConstants.TRACE_ID, headers.getFirst(InteractionHeaderConstants.TRACE_ID));
        appendHeader(builder, InteractionHeaderConstants.AUTHORIZATION, SensitiveLogMasker.maskToken(headers.getFirst(InteractionHeaderConstants.AUTHORIZATION)));
        return builder.toString();
    }

    private void appendHeader(StringBuilder builder, String name, String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(", ");
        }
        builder.append(name).append('=').append(value);
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String toNullableString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
