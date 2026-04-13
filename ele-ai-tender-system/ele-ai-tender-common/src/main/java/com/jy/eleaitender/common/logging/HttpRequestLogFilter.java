package com.jy.eleaitender.common.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eleaitender.common.entity.support.SysAccessLog;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * 通用请求日志过滤器
 */
@Slf4j
public class HttpRequestLogFilter extends OncePerRequestFilter {

    private final String serviceName;
    private final ObjectMapper objectMapper;
    private final AccessLogProperties properties;
    private final AccessLogPersistenceService persistenceService;

    public HttpRequestLogFilter() {
        this("unknown-service", new ObjectMapper(), new AccessLogProperties(), null);
    }

    public HttpRequestLogFilter(String serviceName,
                                ObjectMapper objectMapper,
                                AccessLogProperties properties,
                                AccessLogPersistenceService persistenceService) {
        this.serviceName = serviceName;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.persistenceService = persistenceService;
    }

    @Override
    /**
     * 统一拦截 HTTP 请求，补 traceId、生成访问日志并在响应返回前持久化。
     */
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        long start = System.currentTimeMillis();
        Throwable error = null;
        String traceId = TraceContext.initTraceId(request.getHeader(TraceConstants.TRACE_ID_HEADER));
        ContentCachingRequestWrapper requestWrapper = wrapRequest(request);
        ContentCachingResponseWrapper responseWrapper = wrapResponse(response);
        responseWrapper.setHeader(TraceConstants.TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } catch (Throwable ex) {
            error = ex;
            throw ex;
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            // 使用缓存包装器读取请求/响应体，避免日志采集把流消费掉。
            SysAccessLog accessLog = HttpAccessLogSupport.buildAccessLog(
                    request,
                    requestWrapper,
                    responseWrapper,
                    serviceName,
                    traceId,
                    elapsed,
                    error,
                    objectMapper,
                    properties);
            log.info(HttpAccessLogSupport.buildLogMessage(accessLog));
            if (persistenceService != null) {
                persistenceService.persist(accessLog);
            }
            responseWrapper.copyBodyToResponse();
            TraceContext.clear();
        }
    }

    /**
     * 只在必要时包装请求，避免重复嵌套包装器。
     */
    private ContentCachingRequestWrapper wrapRequest(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper) {
            return (ContentCachingRequestWrapper) request;
        }
        return new ContentCachingRequestWrapper(request);
    }

    /**
     * 只在必要时包装响应，确保最终还能把缓存体写回客户端。
     */
    private ContentCachingResponseWrapper wrapResponse(HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper) {
            return (ContentCachingResponseWrapper) response;
        }
        return new ContentCachingResponseWrapper(response);
    }
}
