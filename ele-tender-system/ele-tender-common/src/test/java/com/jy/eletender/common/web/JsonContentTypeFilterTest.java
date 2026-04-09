package com.jy.eletender.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class JsonContentTypeFilterTest {

    @Test
    void shouldAllowJsonRequestBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/external/token");
        request.setContentType("application/json;charset=UTF-8");
        request.setContent("{\"userId\":\"U1\"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        FilterChain chain = (servletRequest, servletResponse) -> chainInvoked.set(true);

        JsonContentTypeFilter filter = new JsonContentTypeFilter(new ObjectMapper());
        filter.doFilter(request, response, chain);

        assertThat(chainInvoked.get()).isTrue();
    }

    @Test
    void shouldRejectXmlRequestBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/external/token");
        request.setContentType("application/xml;charset=UTF-8");
        request.setContent("<xml></xml>".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        FilterChain chain = (servletRequest, servletResponse) -> chainInvoked.set(true);

        JsonContentTypeFilter filter = new JsonContentTypeFilter(new ObjectMapper());
        filter.doFilter(request, response, chain);

        assertThat(chainInvoked.get()).isFalse();
        assertThat(response.getStatus()).isEqualTo(415);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString()).contains("\"code\":400");
    }

    @Test
    void shouldAllowMultipartUploadRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/file/upload");
        request.setContentType("multipart/form-data; boundary=----WebKitFormBoundary");
        request.setContent("------WebKitFormBoundary".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        FilterChain chain = (servletRequest, servletResponse) -> chainInvoked.set(true);

        JsonContentTypeFilter filter = new JsonContentTypeFilter(new ObjectMapper());
        filter.doFilter(request, response, chain);

        assertThat(chainInvoked.get()).isTrue();
    }
}
