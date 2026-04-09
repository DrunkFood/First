package com.jy.eletender.interaction.core.support;

import com.jy.eletender.common.interaction.constant.InteractionHeaderConstants;
import com.jy.eletender.common.interaction.spi.InteractionEventLogger;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.UUID;

/**
 * 出站请求日志拦截器
 */
@Slf4j
public class OutboundLogInterceptor implements ClientHttpRequestInterceptor {

    private final InteractionEventLogger eventLogger;

    public OutboundLogInterceptor(InteractionEventLogger eventLogger) {
        this.eventLogger = eventLogger;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        long start = System.currentTimeMillis();
        Throwable error = null;
        ClientHttpResponse response = null;

        String traceId = InteractionTraceSupport.getTraceId();
        boolean generatedTraceId = false;
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
            MDC.put(InteractionTraceSupport.TRACE_ID_MDC_KEY, traceId);
            generatedTraceId = true;
        }
        request.getHeaders().set(InteractionHeaderConstants.TRACE_ID, traceId);

        try {
            response = execution.execute(request, body);
            return response;
        } catch (IOException e) {
            error = e;
            throw e;
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            Integer status = null;
            try {
                if (response != null) {
                    status = response.getStatusCode().value();
                }
            } catch (IOException ignored) {
                // ignore response status parsing issues for logging only
            }
            log.info("HTTP OUT traceId={} method={} url={} status={} elapsedMs={} auth={} appKey={} signature={}",
                    traceId,
                    request.getMethod(),
                    request.getURI(),
                    status,
                    elapsed,
                    InteractionLogMasker.maskToken(request.getHeaders().getFirst(InteractionHeaderConstants.AUTHORIZATION)),
                    request.getHeaders().getFirst(InteractionHeaderConstants.APP_KEY),
                    InteractionLogMasker.maskSecret(request.getHeaders().getFirst(InteractionHeaderConstants.SIGNATURE)));
            if (eventLogger != null) {
                eventLogger.logOutbound(request.getMethod() + " " + request.getURI(), request, status, error);
            }
            if (generatedTraceId) {
                MDC.remove(InteractionTraceSupport.TRACE_ID_MDC_KEY);
            }
        }
    }
}
