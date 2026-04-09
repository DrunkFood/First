package com.jy.eletender.common.logging;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * Trace 上下文
 */
public final class TraceContext {

    private TraceContext() {
    }

    public static String initTraceId(String traceId) {
        String resolved = StringUtils.isNotBlank(traceId) ? traceId : generateTraceId();
        MDC.put(TraceConstants.TRACE_ID_MDC_KEY, resolved);
        return resolved;
    }

    public static String getTraceId() {
        return MDC.get(TraceConstants.TRACE_ID_MDC_KEY);
    }

    public static void clear() {
        MDC.remove(TraceConstants.TRACE_ID_MDC_KEY);
    }

    private static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
