package com.jy.eletender.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eletender.common.enums.ResponseCode;
import com.jy.eletender.common.response.Result;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 统一约束请求体媒体类型：仅允许 JSON（文件上传场景允许 multipart/form-data）。
 */
public class JsonContentTypeFilter extends OncePerRequestFilter {

    private static final Set<String> CONTENT_TYPE_CHECK_METHODS = Set.of(
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.PATCH.name()
    );

    private final ObjectMapper objectMapper;

    public JsonContentTypeFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!shouldCheck(request) || isSupportedContentType(request.getContentType())) {
            filterChain.doFilter(request, response);
            return;
        }
        writeUnsupportedMediaTypeResponse(response);
    }

    private boolean shouldCheck(HttpServletRequest request) {
        if (!CONTENT_TYPE_CHECK_METHODS.contains(request.getMethod())) {
            return false;
        }
        long contentLength = request.getContentLengthLong();
        if (contentLength > 0) {
            return true;
        }
        String transferEncoding = request.getHeader("Transfer-Encoding");
        return StringUtils.hasText(transferEncoding) && !"identity".equalsIgnoreCase(transferEncoding);
    }

    private boolean isSupportedContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return false;
        }
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        if (MediaType.MULTIPART_FORM_DATA.includes(mediaType)) {
            return true;
        }
        if (!MediaType.APPLICATION_JSON.isCompatibleWith(mediaType)) {
            String subtype = mediaType.getSubtype();
            return subtype != null && subtype.toLowerCase().endsWith("+json");
        }
        return true;
    }

    private void writeUnsupportedMediaTypeResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=" + StandardCharsets.UTF_8.name());
        Result<Void> result = Result.fail(ResponseCode.PARAM_ERROR.getCode(), "不支持的Content-Type，请使用application/json");
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
