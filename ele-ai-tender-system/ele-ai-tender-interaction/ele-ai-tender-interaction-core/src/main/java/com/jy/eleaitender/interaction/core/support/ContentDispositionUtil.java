package com.jy.eleaitender.interaction.core.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;

/**
 * Content-Disposition 头解析工具。
 */
@Slf4j
public final class ContentDispositionUtil {

    private ContentDispositionUtil() {
    }

    /**
     * 从 HTTP 响应头中解析文件名，优先使用标准 ContentDisposition 解析，
     * 失败时兜底手工解析原始头。
     */
    public static String resolveFileName(HttpHeaders headers) {
        try {
            ContentDisposition disposition = headers.getContentDisposition();
            if (disposition.getFilename() != null) {
                return disposition.getFilename();
            }
        } catch (RuntimeException e) {
            log.warn("标准Content-Disposition解析失败，使用原始头兜底解析: {}", e.getMessage(), e);
        }

        String contentDisposition = headers.getFirst(HttpHeaders.CONTENT_DISPOSITION);
        if (contentDisposition == null) {
            return null;
        }

        String marker = "filename=\"";
        int start = contentDisposition.indexOf(marker);
        if (start < 0) {
            return null;
        }
        int end = contentDisposition.indexOf('"', start + marker.length());
        return end < 0 ? null : contentDisposition.substring(start + marker.length(), end);
    }
}
