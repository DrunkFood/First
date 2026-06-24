package com.jy.eleaitender.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 统一 UTF-8 编码过滤器：
 * 1. 请求/响应编码统一兜底 UTF-8
 * 2. 文本类响应未携带 charset 时自动追加 charset=UTF-8
 */
@Slf4j
public class Utf8ContentTypeFilter extends OncePerRequestFilter {

    private static final String UTF_8 = StandardCharsets.UTF_8.name();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        forceRequestEncoding(request);
        filterChain.doFilter(request, response);
        enforceUtf8CharsetIfNeeded(response);
    }

    private void forceRequestEncoding(HttpServletRequest request) throws java.io.UnsupportedEncodingException {
        String requestEncoding = request.getCharacterEncoding();
        if (!StringUtils.hasText(requestEncoding) || isIso88591(requestEncoding)) {
            request.setCharacterEncoding(UTF_8);
        }
    }

    private void enforceUtf8CharsetIfNeeded(HttpServletResponse response) {
        if (response.isCommitted()) {
            return;
        }
        String contentType = response.getContentType();
        if (!StringUtils.hasText(contentType)) {
            return;
        }
        String loweredContentType = contentType.toLowerCase(Locale.ROOT);
        if (!isTextualContentType(loweredContentType)) {
            return;
        }
        String responseEncoding = response.getCharacterEncoding();
        boolean hasCharset = loweredContentType.contains("charset=");
        if (hasCharset && !isIso88591(responseEncoding)) {
            return;
        }
        response.setCharacterEncoding(UTF_8);
        if (hasCharset) {
            response.setContentType(replaceCharset(contentType));
            return;
        }
        response.setContentType(contentType + ";charset=" + UTF_8);
    }

    private String replaceCharset(String contentType) {
        return contentType.replaceAll("(?i)charset\\s*=\\s*[^;]+", "charset=" + UTF_8);
    }

    private boolean isTextualContentType(String loweredContentType) {
        String mediaType = loweredContentType;
        int semicolonIndex = loweredContentType.indexOf(';');
        if (semicolonIndex >= 0) {
            mediaType = loweredContentType.substring(0, semicolonIndex).trim();
        }
        return mediaType.startsWith("text/")
                || mediaType.equals("application/json")
                || mediaType.endsWith("+json")
                || mediaType.equals("application/xml")
                || mediaType.endsWith("+xml")
                || mediaType.equals("application/javascript")
                || mediaType.equals("application/xhtml+xml");
    }

    private boolean isIso88591(String encoding) {
        try {
            return StandardCharsets.ISO_8859_1.name().equalsIgnoreCase(Charset.forName(encoding).name());
        } catch (Exception ex) {
            log.warn("判断请求编码是否ISO-8859-1失败: encoding={}", encoding, ex);
            return false;
        }
    }
}
