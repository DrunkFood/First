# Interaction FileClient 合并与上传能力实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 合并 FileInfoClient + FileDownloadClient 为 FileClient，新增文件上传能力，升级交互系统版本号，更新全部相关文档。

**Architecture:** 在 `ele-tender-interaction-core` 中用单一 `FileClient` 替代两个独立 client，新增 multipart 上传方法（byte[] 和 Path 两种入参）。`InteractionFileUploadResponse` DTO 放 `common-interaction`。自动配置和统一客户端同步改造。所有 `docs/guides/` 加版本头，相关规范文档同步更新。

**Tech Stack:** Java 8 · Spring Boot 2.7 · RestTemplate · MockRestServiceServer · Maven

---

## 文件结构

| 操作 | 文件路径 | 职责 |
|------|----------|------|
| 创建 | `ele-tender-common-interaction/.../dto/InteractionFileUploadResponse.java` | 上传响应 DTO |
| 创建 | `ele-tender-interaction-core/.../client/FileClient.java` | 合并后的文件客户端 |
| 创建 | `ele-tender-interaction-core/.../client/FileClientTest.java` | FileClient 测试 |
| 修改 | `ele-tender-common-interaction/.../util/InteractionValidationUtils.java` | 新增上传参数校验方法 |
| 修改 | `ele-tender-interaction-core/.../properties/EleTenderInteractionProperties.java` | 新增 fileUploadPath |
| 修改 | `ele-tender-interaction-core/.../client/EleTenderInteractionClient.java` | 改用 FileClient + 新增上传方法 |
| 修改 | `ele-tender-interaction-autoconfigure/.../EleTenderInteractionAutoConfiguration.java` | 替换 bean 定义 |
| 修改 | `ele-tender-system/pom.xml` | interaction.version 属性 |
| 修改 | `ele-tender-common-interaction/pom.xml` | version 升级 |
| 修改 | `ele-tender-interaction/pom.xml` | version + interaction.common.version 升级 |
| 删除 | `ele-tender-interaction-core/.../client/FileInfoClient.java` | 被 FileClient 替代 |
| 删除 | `ele-tender-interaction-core/.../client/FileDownloadClient.java` | 被 FileClient 替代 |
| 删除 | `ele-tender-interaction-core/.../client/FileInfoClientTest.java` | 被 FileClientTest 替代 |
| 删除 | `ele-tender-interaction-core/.../client/FileDownloadClientTest.java` | 被 FileClientTest 替代 |
| 修改 | `docs/guides/业务系统接入手册.md` | 版本头 + FileClient 合并 + uploadFile 说明 |
| 修改 | `docs/guides/开发联调CLI工具使用说明.md` | 版本头 |
| 修改 | `docs/guides/本地加解密工具指南（Java CLI）.md` | 版本头 |
| 修改 | `docs/guides/Electron 客户端加解密指南.md` | 版本头 |
| 修改 | `docs/guides/Electron-native-交付清单.md` | 版本头 |
| 修改 | `docs/guides/NativeVectorExportCli-使用说明.md` | 版本头 |
| 修改 | `docs/guides/2026-03-03-招标文件编制系统前端联调文档.md` | 版本头 |
| 修改 | `docs/guides/2026-03-04-招标文件编制系统接口文档-openapi3.yaml` | yaml changelog 注释 |
| 修改 | `docs/guides/2026-03-06-文件服务接口文档-openapi3.1.yaml` | yaml changelog 注释 |
| 修改 | `docs/guides/2026-03-10-支撑系统接口文档-openapi3.1.yaml` | yaml changelog 注释 |
| 修改 | `docs/rules/INTERACTION_INTEGRATION_SPEC.md` | 出站能力 + 配置项 |
| 修改 | `docs/rules/ELE_TENDER_CRYPTO_SPEC.md` | 预存流程补充 interaction 上传说明 |
| 修改 | `docs/rules/FILE_SERVICE_SPEC.md` | 标注 interaction 封装了上传能力 |

---

### Task 1: 新增 InteractionFileUploadResponse DTO

**Files:**
- Create: `ele-tender-system/ele-tender-common-interaction/src/main/java/com/jy/eletender/common/interaction/dto/InteractionFileUploadResponse.java`

- [ ] **Step 1: 创建 InteractionFileUploadResponse**

```java
package com.jy.eletender.common.interaction.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 文件上传响应
 */
@Data
public class InteractionFileUploadResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long fileId;
    private String fileName;
    private Long fileSize;
    private String fileSha256;
}
```

- [ ] **Step 2: 新增上传参数校验方法**

修改 `ele-tender-system/ele-tender-common-interaction/src/main/java/com/jy/eletender/common/interaction/util/InteractionValidationUtils.java`，在 `validateFileId` 方法后面新增：

```java
public static void validateFileUploadParams(byte[] content, String fileName, String bizType) {
    requireNotNull(content, "文件内容不能为空");
    if (content.length == 0) {
        throw new InteractionException(InteractionResponseCode.PARAM_ERROR, "文件内容不能为空");
    }
    requireText(fileName, "文件名不能为空");
    requireText(bizType, "业务类型不能为空");
}
```

- [ ] **Step 3: 编译验证**

Run: `cd ele-tender-system && mvn -pl ele-tender-common-interaction compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交**

```bash
git add ele-tender-system/ele-tender-common-interaction/src/main/java/com/jy/eletender/common/interaction/dto/InteractionFileUploadResponse.java ele-tender-system/ele-tender-common-interaction/src/main/java/com/jy/eletender/common/interaction/util/InteractionValidationUtils.java
git commit -m "feat(common-interaction): 新增 InteractionFileUploadResponse DTO 和上传参数校验"
```

---

### Task 2: 新增 fileUploadPath 配置项

**Files:**
- Modify: `ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/src/main/java/com/jy/eletender/interaction/core/properties/EleTenderInteractionProperties.java:25`

- [ ] **Step 1: 在 fileDownloadPath 后新增 fileUploadPath**

在 `EleTenderInteractionProperties.java` 的 `fileDownloadPath` 字段（line 25）后面新增：

```java
private String fileUploadPath = "/api/file/upload";
```

- [ ] **Step 2: 编译验证**

Run: `cd ele-tender-system && mvn -pl ele-tender-interaction/ele-tender-interaction-core -am compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/src/main/java/com/jy/eletender/interaction/core/properties/EleTenderInteractionProperties.java
git commit -m "feat(interaction): 新增 fileUploadPath 配置项"
```

---

### Task 3: 创建 FileClient（合并 + 上传）

**Files:**
- Create: `ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/src/main/java/com/jy/eletender/interaction/core/client/FileClient.java`
- Create: `ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/src/test/java/com/jy/eletender/interaction/core/client/FileClientTest.java`

- [ ] **Step 1: 编写 FileClientTest — getFileInfo 测试**

```java
package com.jy.eletender.interaction.core.client;

import com.jy.eletender.common.interaction.dto.InteractionFileDownloadResponse;
import com.jy.eletender.common.interaction.dto.InteractionFileInfoResponse;
import com.jy.eletender.common.interaction.dto.InteractionFileUploadResponse;
import com.jy.eletender.interaction.core.properties.EleTenderInteractionProperties;
import com.jy.eletender.interaction.core.support.InteractionRequestSigner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FileClientTest {

    private RestTemplate restTemplate;
    private FileClient client;
    private EleTenderInteractionProperties properties;

    @BeforeEach
    void setUp() {
        properties = new EleTenderInteractionProperties();
        properties.setApiBaseUrl("http://localhost:8080");
        properties.setFileBaseUrl("http://localhost:8081");
        properties.setAppKey("demo-key");
        properties.setAppSecret("demo-secret");
        restTemplate = new RestTemplate();
        client = new FileClient(restTemplate, properties, new InteractionRequestSigner(properties));
    }

    @Test
    void shouldQueryFileInfoByFileId() {
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("http://localhost:8081/api/file/info/101"))
                .andExpect(header("X-App-Key", "demo-key"))
                .andExpect(header("Accept", org.hamcrest.Matchers.containsString(MediaType.APPLICATION_JSON_VALUE)))
                .andRespond(withSuccess(
                        "{\"code\":200,\"message\":\"操作成功\",\"data\":{\"fileId\":101,\"fileName\":\"招标文件.pdf\",\"fileSize\":1024,\"fileSha256\":\"abc123\",\"bizType\":\"tender-pdf\"}}",
                        MediaType.APPLICATION_JSON));

        InteractionFileInfoResponse response = client.getFileInfo(101L);

        assertThat(response.getFileId()).isEqualTo(101L);
        assertThat(response.getFileName()).isEqualTo("招标文件.pdf");
        assertThat(response.getFileSha256()).isEqualTo("abc123");
        server.verify();
    }

    @Test
    void shouldFallbackToApiBaseUrlWhenFileBaseUrlMissing() {
        EleTenderInteractionProperties fallbackProps = new EleTenderInteractionProperties();
        fallbackProps.setApiBaseUrl("http://localhost:8080");
        fallbackProps.setAppKey("demo-key");
        fallbackProps.setAppSecret("demo-secret");
        RestTemplate fallbackTemplate = new RestTemplate();
        FileClient fallbackClient = new FileClient(fallbackTemplate, fallbackProps, new InteractionRequestSigner(fallbackProps));

        MockRestServiceServer server = MockRestServiceServer.bindTo(fallbackTemplate).build();
        server.expect(requestTo("http://localhost:8080/api/file/info/202"))
                .andRespond(withSuccess(
                        "{\"code\":200,\"message\":\"操作成功\",\"data\":{\"fileId\":202,\"fileName\":\"说明.docx\",\"fileSize\":256,\"fileSha256\":\"sha\",\"bizType\":\"tender-doc\"}}",
                        MediaType.APPLICATION_JSON));

        InteractionFileInfoResponse response = fallbackClient.getFileInfo(202L);
        assertThat(response.getFileId()).isEqualTo(202L);
        server.verify();
    }

    @Test
    void shouldDownloadFileByFileId() {
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"招标文件.pdf\"");
        server.expect(requestTo("http://localhost:8081/api/file/download/101"))
                .andExpect(header("X-App-Key", "demo-key"))
                .andExpect(header("Accept", org.hamcrest.Matchers.containsString(MediaType.APPLICATION_OCTET_STREAM_VALUE)))
                .andRespond(withSuccess("PDF-DATA".getBytes(), MediaType.APPLICATION_OCTET_STREAM)
                        .headers(responseHeaders));

        InteractionFileDownloadResponse response = client.downloadFile(101L);

        assertThat(response.getFileId()).isEqualTo(101L);
        assertThat(response.getFileName()).isEqualTo("招标文件.pdf");
        assertThat(new String(response.getContent())).isEqualTo("PDF-DATA");
        server.verify();
    }

    @Test
    void shouldUploadFileFromBytes() {
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("http://localhost:8081/api/file/upload"))
                .andExpect(header("X-App-Key", "demo-key"))
                .andRespond(withSuccess(
                        "{\"code\":200,\"message\":\"操作成功\",\"data\":{\"fileId\":301,\"fileName\":\"投标文件.HzctTbs\",\"fileSize\":2048,\"fileSha256\":\"def456\"}}",
                        MediaType.APPLICATION_JSON));

        InteractionFileUploadResponse response = client.uploadFile(
                "file-content".getBytes(StandardCharsets.UTF_8), "投标文件.HzctTbs", "BID_DOCUMENT");

        assertThat(response.getFileId()).isEqualTo(301L);
        assertThat(response.getFileName()).isEqualTo("投标文件.HzctTbs");
        assertThat(response.getFileSha256()).isEqualTo("def456");
        server.verify();
    }

    @Test
    void shouldUploadFileFromPath(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("测试文件.HzctTbs");
        Files.write(testFile, "test-data".getBytes(StandardCharsets.UTF_8));

        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("http://localhost:8081/api/file/upload"))
                .andExpect(header("X-App-Key", "demo-key"))
                .andRespond(withSuccess(
                        "{\"code\":200,\"message\":\"操作成功\",\"data\":{\"fileId\":302,\"fileName\":\"测试文件.HzctTbs\",\"fileSize\":9,\"fileSha256\":\"ghi789\"}}",
                        MediaType.APPLICATION_JSON));

        InteractionFileUploadResponse response = client.uploadFile(testFile, "BID_DOCUMENT");

        assertThat(response.getFileId()).isEqualTo(302L);
        assertThat(response.getFileName()).isEqualTo("测试文件.HzctTbs");
        server.verify();
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd ele-tender-system && mvn -pl ele-tender-interaction/ele-tender-interaction-core -am test -Dtest=FileClientTest -q`
Expected: FAIL — `FileClient` 类不存在

- [ ] **Step 3: 创建 FileClient 实现**

```java
package com.jy.eletender.interaction.core.client;

import com.jy.eletender.common.interaction.dto.InteractionFileDownloadResponse;
import com.jy.eletender.common.interaction.dto.InteractionFileInfoResponse;
import com.jy.eletender.common.interaction.dto.InteractionFileUploadResponse;
import com.jy.eletender.common.interaction.dto.InteractionResult;
import com.jy.eletender.common.interaction.enums.InteractionResponseCode;
import com.jy.eletender.common.interaction.exception.InteractionException;
import com.jy.eletender.common.interaction.util.InteractionValidationUtils;
import com.jy.eletender.interaction.core.properties.EleTenderInteractionProperties;
import com.jy.eletender.interaction.core.support.InteractionRequestSigner;
import com.jy.eletender.interaction.core.support.InteractionTraceSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

/**
 * 文件客户端 — 封装文件信息查询、下载和上传能力。
 */
@Slf4j
public class FileClient {

    private final RestTemplate restTemplate;
    private final EleTenderInteractionProperties properties;
    private final InteractionRequestSigner signer;

    public FileClient(RestTemplate restTemplate,
                      EleTenderInteractionProperties properties,
                      InteractionRequestSigner signer) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.signer = signer;
    }

    // ==================== 查询 ====================

    public InteractionFileInfoResponse getFileInfo(Long fileId) {
        InteractionValidationUtils.validateFileId(fileId);

        HttpHeaders headers = signer.sign(new HttpHeaders());
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<InteractionResult<InteractionFileInfoResponse>> response = restTemplate.exchange(
                resolveFileUrl(properties.getFileInfoPath(), fileId),
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<InteractionResult<InteractionFileInfoResponse>>() {
                });
        InteractionFileInfoResponse data = extractData(response.getBody(), "获取文件信息失败");
        log.info("INTERACTION LOCAL traceId={} api=file/info success=true fileId={} fileName={} fileSize={} bizType={}",
                InteractionTraceSupport.getTraceId(),
                data.getFileId(),
                data.getFileName(),
                data.getFileSize(),
                data.getBizType());
        return data;
    }

    // ==================== 下载 ====================

    public InteractionFileDownloadResponse downloadFile(Long fileId) {
        InteractionValidationUtils.validateFileId(fileId);

        HttpHeaders headers = signer.sign(new HttpHeaders());
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

    // ==================== 上传 ====================

    /**
     * 通过 byte[] 上传文件。
     *
     * @param content  文件内容
     * @param fileName 文件名
     * @param bizType  业务类型（由业务系统指定）
     */
    public InteractionFileUploadResponse uploadFile(byte[] content, String fileName, String bizType) {
        InteractionValidationUtils.validateFileUploadParams(content, fileName, bizType);

        HttpHeaders headers = signer.sign(new HttpHeaders());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new NamedByteArrayResource(content, fileName));
        body.add("bizType", bizType);

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<InteractionResult<InteractionFileUploadResponse>> response = restTemplate.exchange(
                resolveFileBaseUrl() + properties.getFileUploadPath(),
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<InteractionResult<InteractionFileUploadResponse>>() {
                });
        InteractionFileUploadResponse data = extractData(response.getBody(), "上传文件失败");
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
     *
     * @param filePath 本地文件路径（必须存在且可读）
     * @param bizType  业务类型
     */
    public InteractionFileUploadResponse uploadFile(Path filePath, String bizType) {
        if (filePath == null) {
            throw new InteractionException(InteractionResponseCode.PARAM_ERROR, "文件路径不能为空");
        }
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new InteractionException(InteractionResponseCode.PARAM_ERROR, "文件不存在或不可读: " + filePath);
        }
        byte[] content;
        try {
            content = Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new InteractionException(InteractionResponseCode.FAIL, "读取文件失败: " + e.getMessage());
        }
        String fileName = filePath.getFileName().toString();
        return uploadFile(content, fileName, bizType);
    }

    // ==================== 内部方法 ====================

    private <T> T extractData(InteractionResult<T> result, String defaultMessage) {
        if (result == null) {
            throw new InteractionException(InteractionResponseCode.FAIL, defaultMessage);
        }
        if (!result.isSuccess() || result.getData() == null) {
            throw new InteractionException(result.getCode(), result.getMessage());
        }
        return result.getData();
    }

    private InteractionFileDownloadResponse extractDownloadResponse(Long fileId, ResponseEntity<byte[]> response) {
        if (response == null || response.getBody() == null) {
            throw new InteractionException(InteractionResponseCode.FAIL, "下载文件失败");
        }

        InteractionFileDownloadResponse result = new InteractionFileDownloadResponse();
        result.setFileId(fileId);
        result.setContent(response.getBody());
        result.setFileSize(Long.valueOf(response.getBody().length));
        MediaType contentType = response.getHeaders().getContentType();
        if (contentType != null) {
            result.setContentType(contentType.toString());
        }
        result.setFileName(resolveFileName(response.getHeaders()));
        return result;
    }

    private String resolveFileName(HttpHeaders headers) {
        try {
            ContentDisposition disposition = headers.getContentDisposition();
            if (disposition != null && disposition.getFilename() != null) {
                return disposition.getFilename();
            }
        } catch (RuntimeException ignored) {
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
        if (end < 0) {
            return null;
        }
        return contentDisposition.substring(start + marker.length(), end);
    }

    private String resolveFileUrl(String pathTemplate, Long fileId) {
        return resolveFileBaseUrl() + pathTemplate.replace("{fileId}", String.valueOf(fileId));
    }

    private String resolveFileBaseUrl() {
        if (properties.getFileBaseUrl() == null || properties.getFileBaseUrl().trim().isEmpty()) {
            return properties.getApiBaseUrl();
        }
        return properties.getFileBaseUrl();
    }

    /**
     * 为 multipart 上传提供文件名的 ByteArrayResource 子类。
     */
    private static final class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return this.filename;
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd ele-tender-system && mvn -pl ele-tender-interaction/ele-tender-interaction-core -am test -Dtest=FileClientTest -q`
Expected: BUILD SUCCESS, 5 tests pass

- [ ] **Step 5: 提交**

```bash
git add ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/src/main/java/com/jy/eletender/interaction/core/client/FileClient.java ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/src/test/java/com/jy/eletender/interaction/core/client/FileClientTest.java
git commit -m "feat(interaction): 新增 FileClient 合并文件查询、下载、上传能力"
```

---

### Task 4: 改造 EleTenderInteractionClient

**Files:**
- Modify: `ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/src/main/java/com/jy/eletender/interaction/core/client/EleTenderInteractionClient.java`

- [ ] **Step 1: 替换 EleTenderInteractionClient 实现**

将整个文件内容替换为：

```java
package com.jy.eletender.interaction.core.client;

import com.jy.eletender.common.interaction.dto.BidDecryptStatusResponse;
import com.jy.eletender.common.interaction.dto.BidDecryptSubmitRequest;
import com.jy.eletender.common.interaction.dto.BidDecryptSubmitResponse;
import com.jy.eletender.common.interaction.dto.BidDocumentPushRequest;
import com.jy.eletender.common.interaction.dto.BidDocumentPushResponse;
import com.jy.eletender.common.interaction.dto.EnvelopeJoinRequest;
import com.jy.eletender.common.interaction.dto.EnvelopeJoinResponse;
import com.jy.eletender.common.interaction.dto.ExternalTokenRequest;
import com.jy.eletender.common.interaction.dto.ExternalTokenResponse;
import com.jy.eletender.common.interaction.dto.InteractionFileDownloadResponse;
import com.jy.eletender.common.interaction.dto.InteractionFileInfoResponse;
import com.jy.eletender.common.interaction.dto.InteractionFileUploadResponse;
import com.jy.eletender.common.interaction.dto.ExternalUserInfoResponse;
import com.jy.eletender.common.interaction.dto.TenderEntryContext;

import java.nio.file.Path;

/**
 * 电子标交互统一客户端
 */
public class EleTenderInteractionClient {

    private final ExternalAuthClient externalAuthClient;
    private final ExternalUserInfoClient externalUserInfoClient;
    private final FileClient fileClient;
    private final BidDocumentPushClient bidDocumentPushClient;
    private final BidDecryptClient bidDecryptClient;
    private final EnvelopeClient envelopeClient;
    private final TenderDocumentEntryUrlBuilder tenderDocumentEntryUrlBuilder;

    public EleTenderInteractionClient(ExternalAuthClient externalAuthClient,
                                      ExternalUserInfoClient externalUserInfoClient,
                                      FileClient fileClient,
                                      TenderDocumentEntryUrlBuilder tenderDocumentEntryUrlBuilder) {
        this(externalAuthClient,
                externalUserInfoClient,
                fileClient,
                null,
                null,
                null,
                tenderDocumentEntryUrlBuilder);
    }

    public EleTenderInteractionClient(ExternalAuthClient externalAuthClient,
                                      ExternalUserInfoClient externalUserInfoClient,
                                      FileClient fileClient,
                                      BidDocumentPushClient bidDocumentPushClient,
                                      BidDecryptClient bidDecryptClient,
                                      EnvelopeClient envelopeClient,
                                      TenderDocumentEntryUrlBuilder tenderDocumentEntryUrlBuilder) {
        this.externalAuthClient = externalAuthClient;
        this.externalUserInfoClient = externalUserInfoClient;
        this.fileClient = fileClient;
        this.bidDocumentPushClient = bidDocumentPushClient;
        this.bidDecryptClient = bidDecryptClient;
        this.envelopeClient = envelopeClient;
        this.tenderDocumentEntryUrlBuilder = tenderDocumentEntryUrlBuilder;
    }

    public ExternalTokenResponse getExternalToken(ExternalTokenRequest request) {
        return externalAuthClient.getExternalToken(request);
    }

    public ExternalUserInfoResponse getCurrentExternalUser(String authorization) {
        return externalUserInfoClient.getCurrentExternalUser(authorization);
    }

    public InteractionFileInfoResponse getFileInfo(Long fileId) {
        return fileClient.getFileInfo(fileId);
    }

    public InteractionFileDownloadResponse downloadFile(Long fileId) {
        return fileClient.downloadFile(fileId);
    }

    public InteractionFileUploadResponse uploadFile(byte[] content, String fileName, String bizType) {
        return fileClient.uploadFile(content, fileName, bizType);
    }

    public InteractionFileUploadResponse uploadFile(Path filePath, String bizType) {
        return fileClient.uploadFile(filePath, bizType);
    }

    public BidDocumentPushResponse pushBidDocument(String authorization, BidDocumentPushRequest request) {
        requireClient(bidDocumentPushClient, "BidDocumentPushClient");
        return bidDocumentPushClient.push(authorization, request);
    }

    public BidDecryptSubmitResponse submitDecrypt(String authorization, BidDecryptSubmitRequest request) {
        requireClient(bidDecryptClient, "BidDecryptClient");
        return bidDecryptClient.submit(authorization, request);
    }

    public BidDecryptStatusResponse queryDecryptStatus(String authorization, String recordId) {
        requireClient(bidDecryptClient, "BidDecryptClient");
        return bidDecryptClient.queryStatus(authorization, recordId);
    }

    public EnvelopeJoinResponse joinEnvelope(String authorization, EnvelopeJoinRequest request) {
        requireClient(envelopeClient, "EnvelopeClient");
        return envelopeClient.join(authorization, request);
    }

    public String buildTenderDocumentEntryUrl(TenderEntryContext context) {
        return tenderDocumentEntryUrlBuilder.build(context);
    }

    private void requireClient(Object client, String clientName) {
        if (client == null) {
            throw new IllegalStateException(clientName + " is not configured");
        }
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `cd ele-tender-system && mvn -pl ele-tender-interaction/ele-tender-interaction-core -am compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

```bash
git add ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/src/main/java/com/jy/eletender/interaction/core/client/EleTenderInteractionClient.java
git commit -m "refactor(interaction): EleTenderInteractionClient 改用 FileClient 替代独立 file client"
```

---

### Task 5: 改造自动配置 + 删除旧 Client

**Files:**
- Modify: `ele-tender-system/ele-tender-interaction/ele-tender-interaction-autoconfigure/src/main/java/com/jy/eletender/interaction/autoconfigure/EleTenderInteractionAutoConfiguration.java`
- Delete: `ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/src/main/java/com/jy/eletender/interaction/core/client/FileInfoClient.java`
- Delete: `ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/src/main/java/com/jy/eletender/interaction/core/client/FileDownloadClient.java`
- Delete: `ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/src/test/java/com/jy/eletender/interaction/core/client/FileInfoClientTest.java`
- Delete: `ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/src/test/java/com/jy/eletender/interaction/core/client/FileDownloadClientTest.java`

- [ ] **Step 1: 修改自动配置类**

在 `EleTenderInteractionAutoConfiguration.java` 中：

1. 替换 import：删除 `FileInfoClient` 和 `FileDownloadClient` 的 import，新增 `FileClient` 的 import：

```java
// 删除这两行：
import com.jy.eletender.interaction.core.client.FileDownloadClient;
import com.jy.eletender.interaction.core.client.FileInfoClient;
// 新增：
import com.jy.eletender.interaction.core.client.FileClient;
```

2. 删除 `fileInfoClient` bean（lines 86-92）和 `fileDownloadClient` bean（lines 94-100），替换为：

```java
    @Bean
    @ConditionalOnMissingBean
    public FileClient fileClient(@Qualifier(INTERACTION_REST_TEMPLATE_BEAN_NAME) RestTemplate interactionRestTemplate,
                                 EleTenderInteractionProperties properties,
                                 InteractionRequestSigner interactionRequestSigner) {
        return new FileClient(interactionRestTemplate, properties, interactionRequestSigner);
    }
```

3. 修改 `eleTenderInteractionClient` bean 方法（lines 132-151），将参数中的 `FileInfoClient fileInfoClient` 和 `FileDownloadClient fileDownloadClient` 替换为 `FileClient fileClient`，构造调用改为：

```java
    @Bean
    @ConditionalOnMissingBean
    public EleTenderInteractionClient eleTenderInteractionClient(ExternalAuthClient externalAuthClient,
                                                                 ExternalUserInfoClient externalUserInfoClient,
                                                                 FileClient fileClient,
                                                                 BidDocumentPushClient bidDocumentPushClient,
                                                                 BidDecryptClient bidDecryptClient,
                                                                 EnvelopeClient envelopeClient,
                                                                 TenderDocumentEntryUrlBuilder tenderDocumentEntryUrlBuilder) {
        return new EleTenderInteractionClient(
                externalAuthClient,
                externalUserInfoClient,
                fileClient,
                bidDocumentPushClient,
                bidDecryptClient,
                envelopeClient,
                tenderDocumentEntryUrlBuilder);
    }
```

- [ ] **Step 2: 删除旧文件**

```bash
rm ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/src/main/java/com/jy/eletender/interaction/core/client/FileInfoClient.java
rm ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/src/main/java/com/jy/eletender/interaction/core/client/FileDownloadClient.java
rm ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/src/test/java/com/jy/eletender/interaction/core/client/FileInfoClientTest.java
rm ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/src/test/java/com/jy/eletender/interaction/core/client/FileDownloadClientTest.java
```

- [ ] **Step 3: 运行全部 interaction 测试**

Run: `cd ele-tender-system && mvn -pl ele-tender-interaction/ele-tender-interaction-core -am test -q`
Expected: BUILD SUCCESS, all tests pass

- [ ] **Step 4: 提交**

```bash
git add -A ele-tender-system/ele-tender-interaction/ ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/src/main/java/com/jy/eletender/interaction/core/client/FileInfoClient.java ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/src/main/java/com/jy/eletender/interaction/core/client/FileDownloadClient.java ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/src/test/java/com/jy/eletender/interaction/core/client/FileInfoClientTest.java ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/src/test/java/com/jy/eletender/interaction/core/client/FileDownloadClientTest.java
git commit -m "refactor(interaction): 自动配置改用 FileClient，删除 FileInfoClient/FileDownloadClient"
```

---

### Task 6: 版本号升级

**Files:**
- Modify: `ele-tender-system/pom.xml` (line ~30 附近的 `interaction.version` 属性)
- Modify: `ele-tender-system/ele-tender-common-interaction/pom.xml` (line 14)
- Modify: `ele-tender-system/ele-tender-interaction/pom.xml` (line 14 + line 23)

- [ ] **Step 1: 查找父 POM 中的 interaction.version**

搜索父 POM 中的 `interaction.version` 属性。如果不存在独立属性，则 common-interaction 和 interaction 各自声明了自己的 `<version>1.1.2-SNAPSHOT</version>`。

- [ ] **Step 2: 升级 common-interaction 版本**

`ele-tender-system/ele-tender-common-interaction/pom.xml` line 14：

```xml
<!-- 旧 -->
<version>1.1.2-SNAPSHOT</version>
<!-- 新 -->
<version>1.1.3-SNAPSHOT</version>
```

- [ ] **Step 3: 升级 interaction 聚合模块版本**

`ele-tender-system/ele-tender-interaction/pom.xml`：
- line 14：`<version>1.1.3-SNAPSHOT</version>`
- line 23 `interaction.common.version`：`<interaction.common.version>1.1.3-SNAPSHOT</interaction.common.version>`

- [ ] **Step 4: 编译验证**

Run: `cd ele-tender-system && mvn -pl ele-tender-interaction/ele-tender-interaction-spring-boot-starter -am compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: 提交**

```bash
git add ele-tender-system/ele-tender-common-interaction/pom.xml ele-tender-system/ele-tender-interaction/pom.xml
git commit -m "chore: 交互系统版本号升级至 1.1.3-SNAPSHOT"
```

---

### Task 7: docs/guides/ 添加版本头 — Markdown 文件

**Files:**
- Modify: `docs/guides/业务系统接入手册.md`
- Modify: `docs/guides/开发联调CLI工具使用说明.md`
- Modify: `docs/guides/本地加解密工具指南（Java CLI）.md`
- Modify: `docs/guides/Electron 客户端加解密指南.md`
- Modify: `docs/guides/Electron-native-交付清单.md`
- Modify: `docs/guides/NativeVectorExportCli-使用说明.md`
- Modify: `docs/guides/2026-03-03-招标文件编制系统前端联调文档.md`

- [ ] **Step 1: 业务系统接入手册 — 添加版本头**

在 `docs/guides/业务系统接入手册.md` 标题后、`## 1. 目的` 之前插入：

```markdown
> **适用版本**: 1.1.3
> **最后更新**: 2026-04-07

## 变更记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| 1.1.3 | 2026-04-07 | 新增文件上传能力（uploadFile）；FileInfoClient/FileDownloadClient 合并为 FileClient；新增 file-upload-path 配置项 |
| 1.1.2 | — | 初始版本 |

---
```

- [ ] **Step 2: 开发联调CLI工具使用说明 — 替换元数据为版本头**

当前头部有：
```markdown
> 模块：`ele-tender-dev-tools`
> 最后更新：2026-04-01
> 适用范围：开发调试、联调验证
```

替换为：
```markdown
> **适用版本**: 1.1.2
> **最后更新**: 2026-04-01
> **模块**: `ele-tender-dev-tools`
> **适用范围**: 开发调试、联调验证

## 变更记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| 1.1.2 | 2026-04-01 | 初始版本 |
```

- [ ] **Step 3: 本地加解密工具指南 — 替换元数据为版本头**

当前头部有：
```markdown
> 适用范围：开发调试、运维测试、文件验证  
> 最后更新：2026-03-31  
> 参考规范：`docs/rules/ELE_TENDER_CRYPTO_SPEC.md`
```

替换为：
```markdown
> **适用版本**: 1.1.2
> **最后更新**: 2026-03-31
> **适用范围**: 开发调试、运维测试、文件验证
> **参考规范**: `docs/rules/ELE_TENDER_CRYPTO_SPEC.md`

## 变更记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| 1.1.2 | 2026-03-31 | 初始版本 |
```

- [ ] **Step 4: Electron 客户端加解密指南 — 替换元数据为版本头**

当前头部有：
```markdown
> 适用范围：投标方 Electron 客户端  
> 最后更新：2026-03-31  
> 参考规范：`docs/rules/ELE_TENDER_CRYPTO_SPEC.md`
```

替换为：
```markdown
> **适用版本**: 1.1.2
> **最后更新**: 2026-03-31
> **适用范围**: 投标方 Electron 客户端
> **参考规范**: `docs/rules/ELE_TENDER_CRYPTO_SPEC.md`

## 变更记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| 1.1.2 | 2026-03-31 | 初始版本 |
```

- [ ] **Step 5: Electron-native-交付清单 — 添加版本头**

当前头部有：
```markdown
> 适用对象：Electron / Node.js 加解密同事  
> 对接模块：`ele-tender-crypto`
```

替换为：
```markdown
> **适用版本**: 1.1.2
> **最后更新**: 2026-04-07
> **适用对象**: Electron / Node.js 加解密同事
> **对接模块**: `ele-tender-crypto`

## 变更记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| 1.1.2 | 2026-04-07 | 初始版本 |
```

- [ ] **Step 6: NativeVectorExportCli-使用说明 — 添加版本头**

当前头部有：
```markdown
> 适用范围：为 Electron / Node.js 同事导出固定对拍样例  
> 所属模块：`ele-tender-system/ele-tender-crypto`
```

替换为：
```markdown
> **适用版本**: 1.1.2
> **最后更新**: 2026-04-07
> **适用范围**: 为 Electron / Node.js 同事导出固定对拍样例
> **所属模块**: `ele-tender-system/ele-tender-crypto`

## 变更记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| 1.1.2 | 2026-04-07 | 初始版本 |
```

- [ ] **Step 7: 招标文件编制系统前端联调文档 — 添加版本头**

当前 `## 1. 文档说明` 下有：
```markdown
- 文档日期：2026-03-04
- 文档范围：`ele-tender-system/ele-tender-tender-document`
```

在标题 `# 招标文件编制系统前端联调文档` 后、`## 1. 文档说明` 之前插入：
```markdown
> **适用版本**: 1.1.2
> **最后更新**: 2026-03-04

## 变更记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| 1.1.2 | 2026-03-04 | 初始版本 |

---
```

- [ ] **Step 8: 提交**

```bash
git add docs/guides/业务系统接入手册.md docs/guides/开发联调CLI工具使用说明.md "docs/guides/本地加解密工具指南（Java CLI）.md" "docs/guides/Electron 客户端加解密指南.md" docs/guides/Electron-native-交付清单.md docs/guides/NativeVectorExportCli-使用说明.md docs/guides/2026-03-03-招标文件编制系统前端联调文档.md
git commit -m "docs(guides): 全部 Markdown 手册添加版本号和变更记录"
```

---

### Task 8: docs/guides/ 添加版本头 — OpenAPI YAML 文件

**Files:**
- Modify: `docs/guides/2026-03-04-招标文件编制系统接口文档-openapi3.yaml`
- Modify: `docs/guides/2026-03-06-文件服务接口文档-openapi3.1.yaml`
- Modify: `docs/guides/2026-03-10-支撑系统接口文档-openapi3.1.yaml`

- [ ] **Step 1: 招标文件编制系统接口文档 — 在文件顶部加 changelog 注释**

在文件最顶部（`openapi:` 行之前）添加：

```yaml
# 变更记录
# | 版本  | 日期       | 变更内容   |
# | 2.0.0 | 2026-03-04 | 初始版本   |
```

- [ ] **Step 2: 文件服务接口文档 — 在文件顶部加 changelog 注释**

```yaml
# 变更记录
# | 版本  | 日期       | 变更内容   |
# | 1.0.0 | 2026-03-06 | 初始版本   |
```

- [ ] **Step 3: 支撑系统接口文档 — 在文件顶部加 changelog 注释**

```yaml
# 变更记录
# | 版本  | 日期       | 变更内容   |
# | 1.0.0 | 2026-03-10 | 初始版本   |
```

- [ ] **Step 4: 提交**

```bash
git add docs/guides/2026-03-04-招标文件编制系统接口文档-openapi3.yaml docs/guides/2026-03-06-文件服务接口文档-openapi3.1.yaml docs/guides/2026-03-10-支撑系统接口文档-openapi3.1.yaml
git commit -m "docs(guides): OpenAPI YAML 文件添加变更记录注释"
```

---

### Task 9: 更新业务系统接入手册内容

**Files:**
- Modify: `docs/guides/业务系统接入手册.md`

- [ ] **Step 1: 更新依赖版本号**

§3 依赖中 `<version>2.0.0</version>` 改为 `<version>1.1.3-SNAPSHOT</version>`

- [ ] **Step 2: 更新最小配置**

§5 最小配置的 yaml 中，在 `file-download-path` 后添加：

```yaml
    file-upload-path: /api/file/upload
```

- [ ] **Step 3: 更新出站能力列表**

§8 出站能力列表中，将：

```markdown
- `getFileInfo`
- `downloadFile`
```

替换为：

```markdown
- `getFileInfo`
- `downloadFile`
- `uploadFile`（支持 byte[] 或本地文件路径两种方式）
```

- [ ] **Step 4: 更新联调顺序**

§11 推荐联调顺序第 5 步改为：

```markdown
5. 验证文件查询/下载/上传
```

- [ ] **Step 5: 更新预存流程说明**

§12.4 投标文件预存的说明前添加一段：

```markdown
> **提示**：从 1.1.3 起，可直接使用 `client.uploadFile(filePath, "BID_DOCUMENT")` 上传投标文件到文件服务，无需业务系统自行对接文件服务上传接口。

```java
// 上传投标文件
InteractionFileUploadResponse uploadResp = client.uploadFile(
        Paths.get("/path/to/bid-file.HzctTbs"), "BID_DOCUMENT");

// 推送预存
BidDocumentPushRequest req = new BidDocumentPushRequest();
req.setFileId(uploadResp.getFileId());
req.setFileSha256(uploadResp.getFileSha256());
BidDocumentPushResponse resp = client.pushBidDocument(token, req);
```

- [ ] **Step 6: 添加 FileClient 合并说明**

在 §4 模块组成的 `ele-tender-interaction-core` 说明后补充：

```markdown
> **注意**：1.1.3 版本起，原 `FileInfoClient` 和 `FileDownloadClient` 已合并为 `FileClient`，同时新增文件上传能力。
```

- [ ] **Step 7: 提交**

```bash
git add docs/guides/业务系统接入手册.md
git commit -m "docs(guides): 业务系统接入手册更新 FileClient 合并与上传能力说明"
```

---

### Task 10: 更新规范文档

**Files:**
- Modify: `docs/rules/INTERACTION_INTEGRATION_SPEC.md`
- Modify: `docs/rules/ELE_TENDER_CRYPTO_SPEC.md`
- Modify: `docs/rules/FILE_SERVICE_SPEC.md`

- [ ] **Step 1: 更新 INTERACTION_INTEGRATION_SPEC.md §6**

在 §6 出站客户端能力列表中，将：

```markdown
- 查询文件信息
- 下载文件
```

替换为：

```markdown
- 查询文件信息
- 下载文件
- 上传文件
```

- [ ] **Step 2: 更新 INTERACTION_INTEGRATION_SPEC.md §7**

在 §7 配置项中，`file-download-path` 后新增一行：

```markdown
- `file-upload-path`
```

- [ ] **Step 3: 更新 ELE_TENDER_CRYPTO_SPEC.md §2**

在 §2 模块职责第一条"接收业务系统推送的加密投标文件并完成预存"后补充说明：

```markdown
> 业务系统可通过 `ele-tender-interaction` starter 的 `FileClient.uploadFile()` 将投标文件上传到文件服务，然后调用 `pushBidDocument` 完成预存。
```

- [ ] **Step 4: 更新 FILE_SERVICE_SPEC.md §2**

在 §2 当前接口的 `POST /api/file/upload` 后补充说明：

```markdown
> `ele-tender-interaction` starter 已通过 `FileClient` 封装此接口，业务系统无需自行对接。
```

- [ ] **Step 5: 提交**

```bash
git add docs/rules/INTERACTION_INTEGRATION_SPEC.md docs/rules/ELE_TENDER_CRYPTO_SPEC.md docs/rules/FILE_SERVICE_SPEC.md
git commit -m "docs(rules): 规范文档同步更新 FileClient 上传能力和配置项"
```

---

### Task 11: 全量构建验证

- [ ] **Step 1: 运行全部 interaction 测试**

Run: `cd ele-tender-system && mvn -pl ele-tender-interaction/ele-tender-interaction-core -am test -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: 运行全量编译（确认无交叉引用问题）**

Run: `cd ele-tender-system && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交验证（如有未提交的修复）**

检查 `git status`，确认工作区干净。
