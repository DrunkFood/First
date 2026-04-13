package com.jy.eletender.interaction.core.client;

import com.jy.eleaitender.interaction.core.client.FileClient;
import com.jy.eletender.common.interaction.dto.InteractionFileDownloadResponse;
import com.jy.eletender.common.interaction.dto.InteractionFileInfoResponse;
import com.jy.eletender.common.interaction.dto.InteractionFileUploadResponse;
import com.jy.eleaitender.interaction.core.properties.EleTenderInteractionProperties;
import com.jy.eleaitender.interaction.core.support.InteractionRequestSigner;
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
