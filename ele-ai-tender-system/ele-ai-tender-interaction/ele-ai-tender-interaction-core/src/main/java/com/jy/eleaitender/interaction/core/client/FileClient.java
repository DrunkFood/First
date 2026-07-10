package com.jy.eleaitender.interaction.core.client;

import com.jy.eleaitender.common.interaction.dto.InteractionFileDownloadResponse;
import com.jy.eleaitender.common.interaction.dto.InteractionFileInfoResponse;
import com.jy.eleaitender.common.interaction.dto.InteractionFileUploadResponse;
import com.jy.eleaitender.common.interaction.dto.InteractionResult;
import com.jy.eleaitender.common.interaction.enums.InteractionResponseCode;
import com.jy.eleaitender.common.interaction.exception.InteractionException;
import com.jy.eleaitender.common.interaction.util.InteractionResultExtractor;
import com.jy.eleaitender.common.interaction.util.InteractionValidationUtils;
import com.jy.eleaitender.interaction.core.properties.EleAiTenderInteractionProperties;
import com.jy.eleaitender.interaction.core.support.ContentDispositionUtil;
import com.jy.eleaitender.interaction.core.support.InteractionTraceSupport;
import com.jy.eleaitender.interaction.core.support.NamedByteArrayResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Path;
import java.util.Collections;

/**
 * 文件客户端 — 封装文件信息查询、下载和上传能力。
 * <p>外部系统需先通过 {@link ExternalAuthClient#getExternalToken} 获取JWT令牌，
 * 再将令牌传入本类各方法的 {@code authorization} 参数。</p>
 */
@Slf4j
public class FileClient {

    private final RestTemplate restTemplate;
    private final EleAiTenderInteractionProperties properties;

    public FileClient(RestTemplate restTemplate,
                      EleAiTenderInteractionProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * 查询文件信息
     *
     * @param authorization Bearer JWT令牌（通过ExternalAuthClient获取）
     */
    public InteractionFileInfoResponse getFileInfo(String authorization, Long fileId) {
        InteractionValidationUtils.validateFileId(fileId);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<InteractionResult<InteractionFileInfoResponse>> response = restTemplate.exchange(
                resolveFileUrl(properties.getFileInfoPath(), fileId),
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<InteractionResult<InteractionFileInfoResponse>>() {
                });
        InteractionFileInfoResponse data = InteractionResultExtractor.extractData(response.getBody(), "获取文件信息失败");
        log.info("INTERACTION LOCAL traceId={} api=file/info success=true fileId={} fileName={} fileSize={} bizType={}",
                InteractionTraceSupport.getTraceId(),
                data.getFileId(),
                data.getFileName(),
                data.getFileSize(),
                data.getBizType());
        return data;
    }

    /**
     * 下载文件
     *
     * @param authorization Bearer JWT令牌（通过ExternalAuthClient获取）
     */
    public InteractionFileDownloadResponse downloadFile(String authorization, Long fileId) {
        InteractionValidationUtils.validateFileId(fileId);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<byte[]> response = restTemplate.exchange(
                resolveFileUrl(properties.getFileDownloadPath(), fileId),
                HttpMethod.GET,
                entity,
                byte[].class);
        InteractionFileDownloadResponse result = extractDownloadResponse(fileId, response);
        log.info("INTERACTION LOCAL traceId={} api=file/download success=true fileId={} fileName={} fileSize={} contentType={}",
                InteractionTraceSupport.getTraceId(),
                result.getFileId(),
                result.getFileName(),
                result.getFileSize(),
                result.getContentType());
        return result;
    }

    /**
     * 通过 byte[] 上传文件。
     *
     * @param authorization Bearer JWT令牌（通过ExternalAuthClient获取）
     * @param content        文件内容
     * @param fileName       文件名
     * @param bizType         业务类型（由业务系统指定）
     */
    public InteractionFileUploadResponse uploadFile(String authorization, byte[] content, String fileName, String bizType) {
        InteractionValidationUtils.validateFileUploadParams(content, fileName, bizType);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new NamedByteArrayResource(content, fileName));
        body.add("bizType", bizType);

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<InteractionResult<InteractionFileUploadResponse>> response = restTemplate.exchange(
                properties.resolveFileBaseUrl() + properties.getFileUploadPath(),
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<InteractionResult<InteractionFileUploadResponse>>() {
                });
        InteractionFileUploadResponse data = InteractionResultExtractor.extractData(response.getBody(), "上传文件失败");
        log.info("INTERACTION LOCAL traceId={} api=file/upload success=true fileId={} fileName={} fileSize={} fileSha256={}",
                InteractionTraceSupport.getTraceId(),
                data.getFileId(),
                data.getFileName(),
                data.getFileSize(),
                data.getFileSha256());
        return data;
    }

    /**
     * 通过本地文件路径上传文件。
     * <p>注意：由于 interactionRestTemplate 配置了日志拦截器，
     * multipart body 仍会被完整序列化到内存，大文件上传需注意 JVM 堆配置。</p>
     *
     * @param authorization Bearer JWT令牌（通过ExternalAuthClient获取）
     * @param filePath       本地文件路径
     * @param bizType        业务类型
     */
    public InteractionFileUploadResponse uploadFile(String authorization, Path filePath, String bizType) {
        if (filePath == null) {
            throw new InteractionException(InteractionResponseCode.PARAM_ERROR, "文件路径不能为空");
        }
        if (bizType == null || bizType.trim().isEmpty()) {
            throw new InteractionException(InteractionResponseCode.PARAM_ERROR, "业务类型不能为空");
        }
        if (!filePath.toFile().isFile()) {
            throw new InteractionException(InteractionResponseCode.PARAM_ERROR, "文件不存在或不可读: " + filePath);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(filePath));
        body.add("bizType", bizType);

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<InteractionResult<InteractionFileUploadResponse>> response = restTemplate.exchange(
                properties.resolveFileBaseUrl() + properties.getFileUploadPath(),
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<InteractionResult<InteractionFileUploadResponse>>() {
                });
        InteractionFileUploadResponse data = InteractionResultExtractor.extractData(response.getBody(), "上传文件失败");
        log.info("INTERACTION LOCAL traceId={} api=file/upload success=true fileId={} fileName={} fileSize={} fileSha256={}",
                InteractionTraceSupport.getTraceId(),
                data.getFileId(),
                data.getFileName(),
                data.getFileSize(),
                data.getFileSha256());
        return data;
    }

    private InteractionFileDownloadResponse extractDownloadResponse(Long fileId, ResponseEntity<byte[]> response) {
        if (response == null || response.getBody() == null) {
            throw new InteractionException(InteractionResponseCode.FAIL, "下载文件失败");
        }

        InteractionFileDownloadResponse result = new InteractionFileDownloadResponse();
        result.setFileId(fileId);
        result.setContent(response.getBody());
        result.setFileSize((long) response.getBody().length);
        MediaType contentType = response.getHeaders().getContentType();
        if (contentType != null) {
            result.setContentType(contentType.toString());
        }
        result.setFileName(ContentDispositionUtil.resolveFileName(response.getHeaders()));
        return result;
    }

    private String resolveFileUrl(String pathTemplate, Long fileId) {
        return properties.resolveFileBaseUrl() + pathTemplate.replace("{fileId}", String.valueOf(fileId));
    }
}
