package com.jy.eletender.common.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eletender.common.entity.support.SysAccessLog;
import com.jy.eletender.common.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HttpRequestLogFilterTest {

    @Test
    void shouldCreateTraceIdMaskSensitiveHeadersAndPersistAccessLog() throws Exception {
        String token = JwtUtil.generateExternalToken("demo-key", "U-100", "张三", "E-1", "企业A", "QY001");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/eleTender/interaction/callbacks/tender-pdf");
        request.setContentType("application/json");
        request.setCharacterEncoding("UTF-8");
        request.setContent(("{\"bizType\":1,\"bizId\":\"BIZ-1\",\"projectId\":\"P-1\",\"tenderId\":\"T-1\",\"fileId\":101,\"fileName\":\"招标文件.pdf\"}")
                .getBytes("UTF-8"));
        request.addHeader("Authorization", "Bearer " + token);
        request.addHeader("X-App-Key", "demo-key");
        request.addHeader("X-Signature", "SECRET-SIGNATURE");

        MockHttpServletResponse response = new MockHttpServletResponse();
        List<SysAccessLog> persistedLogs = new ArrayList<SysAccessLog>();
        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest servletRequest,
                                 jakarta.servlet.ServletResponse servletResponse) throws IOException, ServletException {
                servletRequest.getInputStream().readAllBytes();
                jakarta.servlet.http.HttpServletResponse httpServletResponse =
                        (jakarta.servlet.http.HttpServletResponse) servletResponse;
                httpServletResponse.setStatus(200);
                httpServletResponse.setContentType("application/json");
                httpServletResponse.getWriter().write("{\"code\":200}");
            }
        };

        HttpRequestLogFilter filter = new HttpRequestLogFilter(
                "ele-tender-support",
                new ObjectMapper(),
                new AccessLogProperties(),
                new AccessLogPersistenceService() {
                    @Override
                    public void persist(SysAccessLog accessLog) {
                        persistedLogs.add(accessLog);
                    }
                });
        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(TraceConstants.TRACE_ID_HEADER)).isNotBlank();
        assertThat(SensitiveLogMasker.maskToken("Bearer abcdefghijklmn")).contains("***");
        assertThat(SensitiveLogMasker.maskSecret("SECRET-SIGNATURE")).contains("***");
        assertThat(TraceContext.getTraceId()).isNull();
        assertThat(persistedLogs).hasSize(1);
        SysAccessLog accessLog = persistedLogs.get(0);
        assertThat(accessLog.getServiceName()).isEqualTo("ele-tender-support");
        assertThat(accessLog.getBizType()).isEqualTo("1");
        assertThat(accessLog.getBizId()).isEqualTo("BIZ-1");
        assertThat(accessLog.getProjectId()).isEqualTo("P-1");
        assertThat(accessLog.getTenderId()).isEqualTo("T-1");
        assertThat(accessLog.getFileId()).isEqualTo("101");
        assertThat(accessLog.getFileName()).isEqualTo("招标文件.pdf");
        assertThat(accessLog.getUserId()).isEqualTo("U-100");
        assertThat(accessLog.getUserName()).isEqualTo("张三");
        assertThat(accessLog.getRequestHeaders()).contains("X-App-Key=demo-key");
        assertThat(accessLog.getRequestBody()).contains("\"bizId\":\"BIZ-1\"");
        assertThat(accessLog.getResponseBody()).contains("\"code\":200");
    }

    @Test
    void shouldDecodeJsonRequestBodyAsUtf8WhenRequestEncodingIsIso88591() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/external/token");
        request.setContentType("application/json");
        request.setCharacterEncoding("ISO-8859-1");
        request.setContent("{\"enterpriseName\":\"浙江今隆建设有限公司(测试)\"}".getBytes("UTF-8"));

        MockHttpServletResponse response = new MockHttpServletResponse();
        List<SysAccessLog> persistedLogs = new ArrayList<SysAccessLog>();
        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest servletRequest,
                                 jakarta.servlet.ServletResponse servletResponse) throws IOException, ServletException {
                servletRequest.getInputStream().readAllBytes();
                jakarta.servlet.http.HttpServletResponse httpServletResponse =
                        (jakarta.servlet.http.HttpServletResponse) servletResponse;
                httpServletResponse.setStatus(200);
            }
        };

        HttpRequestLogFilter filter = new HttpRequestLogFilter(
                "ele-tender-support",
                new ObjectMapper(),
                new AccessLogProperties(),
                new AccessLogPersistenceService() {
                    @Override
                    public void persist(SysAccessLog accessLog) {
                        persistedLogs.add(accessLog);
                    }
                });
        filter.doFilter(request, response, chain);

        assertThat(persistedLogs).hasSize(1);
        assertThat(persistedLogs.get(0).getRequestBody()).contains("浙江今隆建设有限公司(测试)");
    }

    @Test
    void shouldDecodeJsonResponseBodyAsUtf8WhenResponseEncodingIsIso88591AndContentTypeMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/external/token");
        request.setContentType("application/json");
        request.setCharacterEncoding("UTF-8");
        request.setContent("{\"userId\":\"1140\"}".getBytes("UTF-8"));

        MockHttpServletResponse response = new MockHttpServletResponse();
        List<SysAccessLog> persistedLogs = new ArrayList<SysAccessLog>();
        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest servletRequest,
                                 jakarta.servlet.ServletResponse servletResponse) throws IOException, ServletException {
                servletRequest.getInputStream().readAllBytes();
                jakarta.servlet.http.HttpServletResponse httpServletResponse =
                        (jakarta.servlet.http.HttpServletResponse) servletResponse;
                httpServletResponse.setStatus(200);
                httpServletResponse.setCharacterEncoding("ISO-8859-1");
                httpServletResponse.getOutputStream().write("{\"message\":\"签名验证失败\"}".getBytes("UTF-8"));
            }
        };

        HttpRequestLogFilter filter = new HttpRequestLogFilter(
                "ele-tender-support",
                new ObjectMapper(),
                new AccessLogProperties(),
                new AccessLogPersistenceService() {
                    @Override
                    public void persist(SysAccessLog accessLog) {
                        persistedLogs.add(accessLog);
                    }
                });
        filter.doFilter(request, response, chain);

        assertThat(persistedLogs).hasSize(1);
        assertThat(persistedLogs.get(0).getResponseBody()).contains("签名验证失败");
    }

    @Test
    void shouldDecodeJsonResponseBodyAsUtf8WhenResponseEncodingUsesIso88591Alias() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/external/token");
        request.setContentType("application/json");
        request.setCharacterEncoding("UTF-8");
        request.setContent("{\"userId\":\"1140\"}".getBytes("UTF-8"));

        MockHttpServletResponse response = new MockHttpServletResponse();
        List<SysAccessLog> persistedLogs = new ArrayList<SysAccessLog>();
        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest servletRequest,
                                 jakarta.servlet.ServletResponse servletResponse) throws IOException, ServletException {
                servletRequest.getInputStream().readAllBytes();
                jakarta.servlet.http.HttpServletResponse httpServletResponse =
                        (jakarta.servlet.http.HttpServletResponse) servletResponse;
                httpServletResponse.setStatus(200);
                httpServletResponse.setCharacterEncoding("ISO8859-1");
                httpServletResponse.getOutputStream().write("{\"message\":\"签名验证失败\"}".getBytes("UTF-8"));
            }
        };

        HttpRequestLogFilter filter = new HttpRequestLogFilter(
                "ele-tender-support",
                new ObjectMapper(),
                new AccessLogProperties(),
                new AccessLogPersistenceService() {
                    @Override
                    public void persist(SysAccessLog accessLog) {
                        persistedLogs.add(accessLog);
                    }
                });
        filter.doFilter(request, response, chain);

        assertThat(persistedLogs).hasSize(1);
        assertThat(persistedLogs.get(0).getResponseBody()).contains("签名验证失败");
    }
}
