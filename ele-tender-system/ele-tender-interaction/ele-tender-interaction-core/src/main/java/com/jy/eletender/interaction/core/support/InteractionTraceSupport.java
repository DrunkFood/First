package com.jy.eletender.interaction.core.support;

import org.slf4j.MDC;

/**
 * 交互层 trace 工具。
 */
public final class InteractionTraceSupport {

    public static final String TRACE_ID_MDC_KEY = "traceId";

    private InteractionTraceSupport() {
    }

    public static String getTraceId() {
        return MDC.get(TRACE_ID_MDC_KEY);
    }
}
