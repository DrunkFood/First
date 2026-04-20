package com.jy.eleaitender.common.client;

import com.jy.eleaitender.common.dto.response.FileUploadResponse;
import com.jy.eleaitender.common.entity.file.FileInfo;
import com.jy.eleaitender.common.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * 内部文件服务客户端
 * 供各内部微服务调用 file 模块的上传/下载接口
 */
@Slf4j
public class InternalFileServiceClient {

    private static final String SERVICE_NAME = "ele-ai-tender-core";

    private final RestTemplate restTemplate;
    private final InternalFileServiceProperties properties;

    /** 缓存的服务Token及过期时间 */
    private volatile String cachedToken;
    private volatile long tokenExpireAt;

    public InternalFileServiceClient(RestTemplate restTemplate, InternalFileServiceProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * 上传文件（字节数组方式）
     *
     * @param content  文件内容字节数组
     * @param fileName 文件名
     * @param bizType  业务类型
     * @return 上传响应（含fileId）
     */
    public FileUploadResponse upload(byte[] content, String fileName, String bizType) {
        String url = properties.getBaseUrl() + properties.getUploadPath();
        log.info("上传文件到文件服务: url={}, fileName={}, bizType={}, size={}bytes",
                url, fileName, bizType, content.length);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        addAuthHeader(headers);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new NamedByteArrayResource(content, fileName));
        body.add("bizType", bizType);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            log.info("文件上传完成: fileName={}", fileName);
            // 解析响应
            return parseUploadResponse(response.getBody());
        } catch (Exception e) {
            log.error("文件上传失败: fileName={}, error={}", fileName, e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 下载文件
     *
     * @param fileId 文件ID
     * @return 文件内容字节数组
     */
    public byte[] download(Long fileId) {
        String url = properties.getBaseUrl() + properties.getDownloadPath() + "/" + fileId;
        log.info("从文件服务下载文件: url={}", url);

        HttpHeaders headers = new HttpHeaders();
        addAuthHeader(headers);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, request, byte[].class);
            return response.getBody();
        } catch (Exception e) {
            log.error("文件下载失败: fileId={}, error={}", fileId, e.getMessage(), e);
            throw new RuntimeException("文件下载失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取文件元数据
     *
     * @param fileId 文件ID
     * @return 文件信息，文件不存在时返回null
     */
    public FileInfo info(Long fileId) {
        String url = properties.getBaseUrl() + properties.getInfoPath() + "/" + fileId;
        log.info("从文件服务获取文件信息: url={}", url);

        HttpHeaders headers = new HttpHeaders();
        addAuthHeader(headers);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            return parseInfoResponse(response.getBody());
        } catch (Exception e) {
            log.error("获取文件信息失败: fileId={}, error={}", fileId, e.getMessage(), e);
            throw new RuntimeException("获取文件信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成或获取缓存的服务间调用JWT Token
     */
    private void addAuthHeader(HttpHeaders headers) {
        String token = getOrCreateToken();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private synchronized String getOrCreateToken() {
        long now = System.currentTimeMillis();
        if (cachedToken != null && tokenExpireAt > now + 60_000) {
            return cachedToken;
        }
        String secret = properties.getJwtSecret();
        cachedToken = JwtUtil.generateServiceToken(SERVICE_NAME, secret, properties.getTokenExpiration());
        tokenExpireAt = now + properties.getTokenExpiration();
        return cachedToken;
    }

    @SuppressWarnings("unchecked")
    private FileUploadResponse parseUploadResponse(String responseBody) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> result = mapper.readValue(responseBody, java.util.Map.class);
            java.util.Map<String, Object> data = (java.util.Map<String, Object>) result.get("data");
            if (data == null) {
                throw new RuntimeException("文件服务返回数据为空");
            }
            FileUploadResponse response = new FileUploadResponse();
            response.setFileId(((Number) data.get("fileId")).longValue());
            response.setFileName((String) data.get("fileName"));
            response.setFileSize(data.get("fileSize") != null ? ((Number) data.get("fileSize")).longValue() : null);
            response.setFileSha256((String) data.get("fileSha256"));
            response.setFileType((String) data.get("fileType"));
            return response;
        } catch (Exception e) {
            throw new RuntimeException("解析文件上传响应失败: " + e.getMessage(), e);
        }
    }

    private FileInfo parseInfoResponse(String responseBody) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JavaType type = mapper.getTypeFactory()
                    .constructParametricType(com.jy.eleaitender.common.response.Result.class, FileInfo.class);
            com.jy.eleaitender.common.response.Result<FileInfo> result = mapper.readValue(responseBody, type);
            return result.getData();
        } catch (Exception e) {
            throw new RuntimeException("解析文件信息响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 支持设置文件名的ByteArrayResource
     */
    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
