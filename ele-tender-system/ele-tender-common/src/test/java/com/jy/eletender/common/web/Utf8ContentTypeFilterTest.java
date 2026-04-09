package com.jy.eletender.common.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class Utf8ContentTypeFilterTest {

    @Test
    void shouldAppendUtf8CharsetForJsonResponseWithoutCharset() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/external/token");
        request.setCharacterEncoding("ISO8859-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (servletRequest, servletResponse) -> {
            MockHttpServletResponse httpServletResponse = (MockHttpServletResponse) servletResponse;
            httpServletResponse.setContentType("application/json");
            httpServletResponse.setCharacterEncoding("ISO8859-1");
            httpServletResponse.getOutputStream().write("{\"message\":\"签名验证失败\"}".getBytes(StandardCharsets.UTF_8));
        };

        Utf8ContentTypeFilter filter = new Utf8ContentTypeFilter();
        filter.doFilter(request, response, chain);

        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
    }

    @Test
    void shouldNotAppendCharsetForOctetStreamResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/file/download/1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (servletRequest, servletResponse) -> {
            MockHttpServletResponse httpServletResponse = (MockHttpServletResponse) servletResponse;
            httpServletResponse.setContentType("application/octet-stream");
            httpServletResponse.getOutputStream().write(new byte[] {1, 2, 3});
        };

        Utf8ContentTypeFilter filter = new Utf8ContentTypeFilter();
        filter.doFilter(request, response, chain);

        assertThat(response.getContentType()).isEqualTo("application/octet-stream");
    }
}
