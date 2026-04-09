package com.jy.eletender.common.logging;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eletender.common.entity.support.SysAccessLog;
import com.jy.eletender.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTTP访问日志辅助工具。
 */
public final class HttpAccessLogSupport {

    private static final String LOG_TYPE_HTTP_IN = "HTTP_IN";
    private static final String OMITTED_MULTIPART = "[multipart omitted]";
    private static final String OMITTED_BINARY = "[binary omitted]";

    private HttpAccessLogSupport() {
    }

    /**
     * 从 HTTP 请求/响应中抽取日志实体，统一补齐认证、业务键和文件上下文。
     */
    public static SysAccessLog buildAccessLog(HttpServletRequest request,
                                              ContentCachingRequestWrapper requestWrapper,
                                              ContentCachingResponseWrapper responseWrapper,
                                              String serviceName,
                                              String traceId,
                                              long elapsed,
                                              Throwable error,
                                              ObjectMapper objectMapper,
                                              AccessLogProperties properties) {
        SysAccessLog accessLog = new SysAccessLog();
        accessLog.setTraceId(traceId);
        accessLog.setServiceName(serviceName);
        accessLog.setLogType(LOG_TYPE_HTTP_IN);
        accessLog.setHttpMethod(request.getMethod());
        accessLog.setRequestUri(buildRequestUri(request));
        accessLog.setClientIp(resolveClientIp(request));
        accessLog.setStatusCode(Integer.valueOf(responseWrapper.getStatus()));
        accessLog.setSuccessFlag(resolveSuccessFlag(responseWrapper.getStatus(), error));
        accessLog.setElapsedMs(Long.valueOf(elapsed));
        accessLog.setRequestHeaders(buildRequestHeaders(request));
        accessLog.setRequestBody(extractRequestBody(requestWrapper, properties));
        accessLog.setResponseBody(extractResponseBody(responseWrapper, properties));

        if (error != null) {
            accessLog.setErrorType(error.getClass().getSimpleName());
            accessLog.setErrorMessage(limit(error.getMessage(), properties.getErrorMessageMaxLength()));
        }

        fillAuthContext(accessLog, request.getHeader(HttpHeaders.AUTHORIZATION), request.getHeader("X-App-Key"));
        fillBizContextFromRequest(accessLog, request, requestWrapper, objectMapper);
        fillFileContextFromPath(accessLog, request);
        return accessLog;
    }

    /**
     * 控制台打印的摘要日志，字段顺序固定，便于 grep 和日志平台检索。
     */
    public static String buildLogMessage(SysAccessLog accessLog) {
        return "HTTP IN traceId=" + safe(accessLog.getTraceId())
                + " service=" + safe(accessLog.getServiceName())
                + " method=" + safe(accessLog.getHttpMethod())
                + " uri=" + safe(accessLog.getRequestUri())
                + " status=" + safe(accessLog.getStatusCode())
                + " elapsedMs=" + safe(accessLog.getElapsedMs())
                + " userId=" + safe(accessLog.getUserId())
                + " userName=" + safe(accessLog.getUserName())
                + " appKey=" + safe(accessLog.getAppKey())
                + " bizType=" + safe(accessLog.getBizType())
                + " bizId=" + safe(accessLog.getBizId())
                + " projectId=" + safe(accessLog.getProjectId())
                + " tenderId=" + safe(accessLog.getTenderId())
                + " fileId=" + safe(accessLog.getFileId())
                + " fileName=" + safe(accessLog.getFileName())
                + " auth=" + SensitiveLogMasker.maskToken(extractHeader(accessLog.getRequestHeaders(), HttpHeaders.AUTHORIZATION))
                + " signature=" + SensitiveLogMasker.maskSecret(extractHeader(accessLog.getRequestHeaders(), "X-Signature"));
    }

    private static Integer resolveSuccessFlag(int status, Throwable error) {
        return error == null && status < 400 ? Integer.valueOf(1) : Integer.valueOf(0);
    }

    private static String buildRequestUri(HttpServletRequest request) {
        String queryString = request.getQueryString();
        if (StringUtils.isBlank(queryString)) {
            return request.getRequestURI();
        }
        return request.getRequestURI() + "?" + queryString;
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(forwarded)) {
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.isNotBlank(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private static String buildRequestHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        putHeader(headers, HttpHeaders.AUTHORIZATION, SensitiveLogMasker.maskToken(request.getHeader(HttpHeaders.AUTHORIZATION)));
        putHeader(headers, "X-App-Key", request.getHeader("X-App-Key"));
        putHeader(headers, "X-Signature", SensitiveLogMasker.maskSecret(request.getHeader("X-Signature")));
        putHeader(headers, TraceConstants.TRACE_ID_HEADER, request.getHeader(TraceConstants.TRACE_ID_HEADER));
        putHeader(headers, HttpHeaders.CONTENT_TYPE, request.getContentType());
        return headers.toString();
    }

    private static void putHeader(Map<String, String> headers, String name, String value) {
        if (StringUtils.isNotBlank(value)) {
            headers.put(name, value);
        }
    }

    /**
     * 解析 token 类型并补齐认证上下文。外部 token 和内部 token 的 claim 结构不同。
     */
    private static void fillAuthContext(SysAccessLog accessLog, String authorization, String appKeyHeader) {
        if (StringUtils.isNotBlank(appKeyHeader)) {
            accessLog.setAppKey(appKeyHeader);
        }
        if (StringUtils.isBlank(authorization) || !authorization.startsWith("Bearer ")) {
            return;
        }
        String token = authorization.substring(7);
        try {
            Claims claims = JwtUtil.parseToken(token);
            String tokenType = stringValue(claims.get("type"));
            accessLog.setTokenType(tokenType);
            accessLog.setAppKey(firstNonBlank(accessLog.getAppKey(), stringValue(claims.get("appKey"))));
            if ("EXTERNAL".equalsIgnoreCase(tokenType)) {
                accessLog.setUserId(stringValue(claims.get("userId")));
                accessLog.setUserName(stringValue(claims.get("userName")));
                accessLog.setEnterpriseId(stringValue(claims.get("enterpriseId")));
                accessLog.setEnterpriseName(stringValue(claims.get("enterpriseName")));
                return;
            }
            accessLog.setUserId(stringValue(claims.get("userId")));
            accessLog.setUserName(firstNonBlank(stringValue(claims.get("realName")), stringValue(claims.get("username"))));
        } catch (RuntimeException ignored) {
        }
    }

    /**
     * 业务键优先从 query 中取，再从 JSON body 中兜底提取，兼容 GET/POST 两类接口。
     */
    private static void fillBizContextFromRequest(SysAccessLog accessLog,
                                                  HttpServletRequest request,
                                                  ContentCachingRequestWrapper requestWrapper,
                                                  ObjectMapper objectMapper) {
        accessLog.setBizType(firstNonBlank(accessLog.getBizType(), request.getParameter("bizType")));
        accessLog.setBizId(firstNonBlank(accessLog.getBizId(), request.getParameter("bizId")));
        accessLog.setProjectId(firstNonBlank(accessLog.getProjectId(), request.getParameter("projectId")));
        accessLog.setTenderId(firstNonBlank(accessLog.getTenderId(), request.getParameter("tenderId")));
        accessLog.setFileId(firstNonBlank(accessLog.getFileId(), request.getParameter("fileId")));
        accessLog.setFileName(firstNonBlank(accessLog.getFileName(), request.getParameter("fileName")));

        Map<String, Object> bodyMap = parseJsonBody(requestWrapper, objectMapper);
        if (bodyMap.isEmpty()) {
            return;
        }
        accessLog.setBizType(firstNonBlank(accessLog.getBizType(), stringValue(bodyMap.get("bizType"))));
        accessLog.setBizId(firstNonBlank(accessLog.getBizId(), stringValue(bodyMap.get("bizId"))));
        accessLog.setProjectId(firstNonBlank(accessLog.getProjectId(), stringValue(bodyMap.get("projectId"))));
        accessLog.setTenderId(firstNonBlank(accessLog.getTenderId(), stringValue(bodyMap.get("tenderId"))));
        accessLog.setFileId(firstNonBlank(accessLog.getFileId(), stringValue(bodyMap.get("fileId"))));
        accessLog.setFileName(firstNonBlank(accessLog.getFileName(), stringValue(bodyMap.get("fileName"))));
    }

    /**
     * 文件下载等接口的 fileId 经常出现在 URI 尾段，这里补提取一次。
     */
    private static void fillFileContextFromPath(SysAccessLog accessLog, HttpServletRequest request) {
        if (StringUtils.isNotBlank(accessLog.getFileId())) {
            return;
        }
        String requestUri = request.getRequestURI();
        if (StringUtils.isBlank(requestUri)) {
            return;
        }
        int lastSlash = requestUri.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == requestUri.length() - 1) {
            return;
        }
        String value = requestUri.substring(lastSlash + 1);
        if (value.chars().allMatch(Character::isDigit)) {
            accessLog.setFileId(value);
        }
    }

    /**
     * multipart 和二进制请求体不直接记录原文，防止日志爆量和敏感内容泄露。
     */
    private static String extractRequestBody(ContentCachingRequestWrapper requestWrapper, AccessLogProperties properties) {
        if (isMultipart(requestWrapper.getContentType())) {
            return OMITTED_MULTIPART;
        }
        byte[] content = requestWrapper.getContentAsByteArray();
        if (content == null || content.length == 0) {
            return null;
        }
        if (!isTextualContent(requestWrapper.getContentType())) {
            return OMITTED_BINARY;
        }
        return limit(decode(content, requestWrapper.getCharacterEncoding(), requestWrapper.getContentType()), properties.getBodyMaxLength());
    }

    /**
     * 仅对文本型响应做正文采集，二进制响应统一打占位符。
     */
    private static String extractResponseBody(ContentCachingResponseWrapper responseWrapper, AccessLogProperties properties) {
        byte[] content = responseWrapper.getContentAsByteArray();
        if (content == null || content.length == 0) {
            return null;
        }
        if (!isTextualContent(responseWrapper.getContentType())) {
            return OMITTED_BINARY;
        }
        return limit(decode(content, responseWrapper.getCharacterEncoding(), responseWrapper.getContentType()), properties.getBodyMaxLength());
    }

    /**
     * 只有 JSON 请求才尝试解析正文，解析失败时静默降级为空 map。
     */
    private static Map<String, Object> parseJsonBody(ContentCachingRequestWrapper requestWrapper, ObjectMapper objectMapper) {
        if (!isJsonContent(requestWrapper.getContentType())) {
            return java.util.Collections.emptyMap();
        }
        byte[] content = requestWrapper.getContentAsByteArray();
        if (content == null || content.length == 0) {
            return java.util.Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return java.util.Collections.emptyMap();
        }
    }

    private static boolean isMultipart(String contentType) {
        return StringUtils.isNotBlank(contentType) && contentType.toLowerCase().startsWith(MediaType.MULTIPART_FORM_DATA_VALUE);
    }

    private static boolean isJsonContent(String contentType) {
        return StringUtils.isNotBlank(contentType) && contentType.toLowerCase().contains(MediaType.APPLICATION_JSON_VALUE);
    }

    private static boolean isTextualContent(String contentType) {
        if (StringUtils.isBlank(contentType)) {
            return true;
        }
        String normalized = contentType.toLowerCase();
        return normalized.contains("json")
                || normalized.contains("xml")
                || normalized.contains("text")
                || normalized.contains("form-urlencoded");
    }

    private static String decode(byte[] content, String encoding, String contentType) {
        Charset charset = resolveCharset(content, encoding, contentType);
        return new String(content, charset);
    }

    private static Charset resolveCharset(byte[] content, String encoding, String contentType) {
        if (isJsonContent(contentType) && (StringUtils.isBlank(encoding) || isIso88591(encoding))) {
            return StandardCharsets.UTF_8;
        }
        if ((StringUtils.isBlank(encoding) || isIso88591(encoding)) && looksLikeJsonPayload(content)) {
            return StandardCharsets.UTF_8;
        }
        if (StringUtils.isBlank(encoding)) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(encoding);
        } catch (RuntimeException e) {
            return StandardCharsets.UTF_8;
        }
    }

    private static boolean isIso88591(String encoding) {
        if (StringUtils.isBlank(encoding)) {
            return false;
        }
        try {
            return StandardCharsets.ISO_8859_1.name().equalsIgnoreCase(Charset.forName(encoding.trim()).name());
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean looksLikeJsonPayload(byte[] content) {
        if (content == null || content.length == 0) {
            return false;
        }
        int start = 0;
        int end = content.length - 1;
        while (start <= end && isWhitespace(content[start])) {
            start++;
        }
        while (end >= start && isWhitespace(content[end])) {
            end--;
        }
        if (start > end) {
            return false;
        }
        byte first = content[start];
        byte last = content[end];
        return (first == '{' && last == '}') || (first == '[' && last == ']');
    }

    private static boolean isWhitespace(byte value) {
        return value == ' ' || value == '\n' || value == '\r' || value == '\t';
    }

    private static String limit(String value, int maxLength) {
        if (StringUtils.isBlank(value) || maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...(truncated)";
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String firstNonBlank(String first, String second) {
        return StringUtils.isNotBlank(first) ? first : second;
    }

    private static String safe(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private static String extractHeader(String headers, String name) {
        if (StringUtils.isBlank(headers) || StringUtils.isBlank(name)) {
            return null;
        }
        String token = name + "=";
        int start = headers.indexOf(token);
        if (start < 0) {
            return null;
        }
        int end = headers.indexOf(", ", start);
        if (end < 0) {
            end = headers.length() - 1;
        }
        return headers.substring(start + token.length(), end).replace("}", "");
    }
}
