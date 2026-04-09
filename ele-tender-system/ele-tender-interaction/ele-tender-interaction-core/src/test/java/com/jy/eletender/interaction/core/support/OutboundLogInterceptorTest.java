package com.jy.eletender.interaction.core.support;

import com.jy.eletender.common.interaction.constant.InteractionHeaderConstants;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class OutboundLogInterceptorTest {

    @Test
    void shouldGenerateTraceIdAndCleanUpMdcWhenAbsent() throws IOException {
        MDC.remove(InteractionTraceSupport.TRACE_ID_MDC_KEY);
        OutboundLogInterceptor interceptor = new OutboundLogInterceptor(null);
        TestHttpRequest request = new TestHttpRequest(HttpMethod.POST, URI.create("http://localhost:8080/api/external/token"));

        interceptor.intercept(request, new byte[0], (httpRequest, bytes) ->
                new MockClientHttpResponse(new byte[0], HttpStatus.OK));

        String traceId = request.getHeaders().getFirst(InteractionHeaderConstants.TRACE_ID);
        assertThat(traceId).isNotBlank();
        assertThat(MDC.get(InteractionTraceSupport.TRACE_ID_MDC_KEY)).isNull();
    }

    @Test
    void shouldReuseExistingTraceIdWithoutClearingMdc() throws IOException {
        MDC.put(InteractionTraceSupport.TRACE_ID_MDC_KEY, "trace-001");
        try {
            OutboundLogInterceptor interceptor = new OutboundLogInterceptor(null);
            TestHttpRequest request = new TestHttpRequest(HttpMethod.POST, URI.create("http://localhost:8080/api/external/token"));

            interceptor.intercept(request, new byte[0], (httpRequest, bytes) ->
                    new MockClientHttpResponse(new byte[0], HttpStatus.OK));

            assertThat(request.getHeaders().getFirst(InteractionHeaderConstants.TRACE_ID)).isEqualTo("trace-001");
            assertThat(MDC.get(InteractionTraceSupport.TRACE_ID_MDC_KEY)).isEqualTo("trace-001");
        } finally {
            MDC.remove(InteractionTraceSupport.TRACE_ID_MDC_KEY);
        }
    }

    private static final class TestHttpRequest implements HttpRequest {

        private final HttpMethod method;
        private final URI uri;
        private final HttpHeaders headers = new HttpHeaders();

        private TestHttpRequest(HttpMethod method, URI uri) {
            this.method = method;
            this.uri = uri;
        }

        @Override
        public HttpMethod getMethod() {
            return method;
        }

        @Override
        public String getMethodValue() {
            return method.name();
        }

        @Override
        public URI getURI() {
            return uri;
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }
    }
}
