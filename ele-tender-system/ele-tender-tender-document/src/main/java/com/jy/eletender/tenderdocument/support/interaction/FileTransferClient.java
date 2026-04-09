package com.jy.eletender.tenderdocument.support.interaction;

import com.jy.eletender.common.dto.response.FileUploadResponse;
import com.jy.eletender.common.exception.BusinessException;
import com.jy.eletender.common.logging.TraceConstants;
import com.jy.eletender.common.response.Result;
import com.jy.eletender.tenderdocument.enums.TenderDocumentErrorCode;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 文件服务传输客户端。
 * 提供文件上传与下载的统一封装，并将异常转换为编制模块业务异常。
 */
@Component
public class FileTransferClient {

    private final RestTemplate restTemplate;
    private final TenderDocumentInteractionProperties properties;

    public FileTransferClient(RestTemplate tenderDocumentInteractionRestTemplate,
                              TenderDocumentInteractionProperties properties) {
        this.restTemplate = tenderDocumentInteractionRestTemplate;
        this.properties = properties;
    }

    /**
     * 上传文件到文件服务并返回标准化文件元信息。
     */
    public UploadedFileInfo upload(String fileName, String bizType, String contentType, byte[] content) {
        try {
            HttpHeaders fileHeaders = new HttpHeaders();
            if (contentType != null && !contentType.trim().isEmpty()) {
                fileHeaders.setContentType(MediaType.parseMediaType(contentType));
            }
            HttpEntity<ByteArrayResource> filePart = new HttpEntity<ByteArrayResource>(new NamedByteArrayResource(fileName, content), fileHeaders);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<String, Object>();
            body.add("file", filePart);
            body.add("bizType", bizType);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            ResponseEntity<Result<FileUploadResponse>> response = restTemplate.exchange(
                    resolveUrl(properties.getFileUploadPath(), null),
                    HttpMethod.POST,
                    new HttpEntity<MultiValueMap<String, Object>>(body, headers),
                    new ParameterizedTypeReference<Result<FileUploadResponse>>() {
                    }
            );
            Result<FileUploadResponse> result = response.getBody();
            // 文件服务约定 code=200 且 data 非空才表示上传成功。
            if (result == null || result.getData() == null || result.getCode() != 200) {
                throw fileFailure(buildUploadFailureMessage(result));
            }
            FileUploadResponse data = result.getData();
            return new UploadedFileInfo(data.getFileId(), data.getFileName(), data.getFileSize(), data.getFileSha256());
        } catch (RestClientException ex) {
            throw fileFailure("上传文件失败: " + ex.getMessage());
        }
    }

    /**
     * 从文件服务下载文件内容与基础元信息。
     */
    public DownloadedFileInfo download(Long fileId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(TraceConstants.TRACE_ID_HEADER, "");
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    resolveUrl(properties.getFileDownloadPath(), fileId),
                    HttpMethod.GET,
                    new HttpEntity<Void>(headers),
                    byte[].class
            );
            byte[] content = response.getBody();
            if (content == null) {
                throw fileFailure("下载文件失败: 文件内容为空");
            }
            MediaType contentType = response.getHeaders().getContentType();
            return new DownloadedFileInfo(fileId, resolveFileName(response.getHeaders()), contentType == null ? null : contentType.toString(), content);
        } catch (RestClientException ex) {
            throw fileFailure("下载文件失败: " + ex.getMessage());
        }
    }

    private String resolveUrl(String path, Long fileId) {
        String baseUrl = properties.getFileServiceBaseUrl();
        // 统一做 baseUrl 标准化，避免配置为带尾斜杠时拼接错误。
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (fileId == null) {
            return normalizedBase + path;
        }
        return normalizedBase + path.replace("{fileId}", String.valueOf(fileId));
    }

    private String resolveFileName(HttpHeaders headers) {
        try {
            ContentDisposition disposition = headers.getContentDisposition();
            if (disposition != null && disposition.getFilename() != null) {
                return disposition.getFilename();
            }
        } catch (RuntimeException ignored) {
            // fallback below
        }
        // 部分文件服务不会返回标准化 ContentDisposition，这里兜底解析原始响应头。
        String value = headers.getFirst(HttpHeaders.CONTENT_DISPOSITION);
        if (value == null) {
            return null;
        }
        String marker = "filename=\"";
        int start = value.indexOf(marker);
        if (start < 0) {
            return null;
        }
        int end = value.indexOf('"', start + marker.length());
        return end < 0 ? null : value.substring(start + marker.length(), end);
    }

    private BusinessException fileFailure(String message) {
        return new BusinessException(TenderDocumentErrorCode.FILE_SERVICE_ACCESS_FAILED.getCode(), message);
    }

    private String buildUploadFailureMessage(Result<FileUploadResponse> result) {
        if (result == null) {
            return "上传文件失败: 下游响应为空";
        }
        String message = result.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = "未知错误";
        }
        return "上传文件失败: code=" + result.getCode() + ", message=" + message;
    }

    private static final class NamedByteArrayResource extends ByteArrayResource {

        private final String fileName;

        private NamedByteArrayResource(String fileName, byte[] byteArray) {
            super(byteArray);
            this.fileName = fileName;
        }

        @Override
        public String getFilename() {
            return fileName;
        }
    }
}
