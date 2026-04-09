package com.jy.eletender.crypto.service;

import com.jy.eletender.common.entity.crypto.BdcDecryptRequest;
import com.jy.eletender.crypto.config.CryptoProperties;
import com.jy.eletender.crypto.mapper.SysAccessSystemReadMapper;
import com.jy.eletender.crypto.service.impl.OrganizedFileCopyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OrganizedFileCopyServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private SysAccessSystemReadMapper sysAccessSystemReadMapper;

    private OrganizedFileCopyService service;

    @BeforeEach
    void setUp() {
        CryptoProperties properties = new CryptoProperties();
        properties.setOrganizedFilePath(tempDir.toString());
        service = new OrganizedFileCopyService(properties, sysAccessSystemReadMapper);
    }

    private BdcDecryptRequest buildRequest() {
        BdcDecryptRequest request = new BdcDecryptRequest();
        request.setId(1L);
        request.setAppKey("app-a");
        request.setProjectId("P001");
        request.setTenderId("T001");
        request.setBidRecordId("B001");
        return request;
    }

    @Test
    void shouldCopyEncryptedFileToOrganizedDirectory() throws Exception {
        Path source = Files.createTempFile(tempDir, "enc", ".HzctTbs");
        Files.writeString(source, "encrypted-content");

        BdcDecryptRequest request = buildRequest();
        String result = service.copyEncryptedFile(request, source.toString());

        assertThat(result).isNotNull();
        Path dest = Path.of(result);
        assertThat(Files.exists(dest)).isTrue();
        assertThat(Files.readString(dest)).isEqualTo("encrypted-content");
        assertThat(dest.toString()).contains("app-a", "P001", "T001", "encrypted", "B001.HzctTbs");
    }

    @Test
    void shouldCopyDecryptedFileToOrganizedDirectory() throws Exception {
        Path source = Files.createTempFile(tempDir, "dec", ".json");
        Files.writeString(source, "{\"payload\":\"test\"}");

        BdcDecryptRequest request = buildRequest();
        String result = service.copyDecryptedFile(request, source.toString());

        assertThat(result).isNotNull();
        Path dest = Path.of(result);
        assertThat(Files.exists(dest)).isTrue();
        assertThat(Files.readString(dest)).isEqualTo("{\"payload\":\"test\"}");
        assertThat(dest.toString()).contains("app-a", "P001", "T001", "decrypted", "B001.json");
    }

    @Test
    void shouldSkipCopyWhenDestAlreadyExistsWithSameSha256() throws Exception {
        Path source = Files.createTempFile(tempDir, "enc", ".HzctTbs");
        Files.writeString(source, "same-content");

        BdcDecryptRequest request = buildRequest();
        String firstResult = service.copyEncryptedFile(request, source.toString());
        assertThat(firstResult).isNotNull();

        long modifiedBefore = Files.getLastModifiedTime(Path.of(firstResult)).toMillis();
        Thread.sleep(10);

        String secondResult = service.copyEncryptedFile(request, source.toString());
        assertThat(secondResult).isEqualTo(firstResult);
        // SHA256 一致时跳过复制，文件修改时间不变
        assertThat(Files.getLastModifiedTime(Path.of(secondResult)).toMillis()).isEqualTo(modifiedBefore);
    }

    @Test
    void shouldReturnNullWhenSourceFileDoesNotExist() {
        BdcDecryptRequest request = buildRequest();
        String result = service.copyEncryptedFile(request, "/nonexistent/path/file.HzctTbs");
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullWhenRequestIsNull() {
        String result = service.copyEncryptedFile(null, "/some/path");
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullWhenSourcePathIsBlank() {
        BdcDecryptRequest request = buildRequest();
        assertThat(service.copyEncryptedFile(request, null)).isNull();
        assertThat(service.copyEncryptedFile(request, "")).isNull();
    }

    @Test
    void shouldReturnNullWhenProjectIdContainsPathTraversal() throws Exception {
        Path source = Files.createTempFile(tempDir, "enc", ".HzctTbs");
        Files.writeString(source, "content");

        BdcDecryptRequest request = buildRequest();
        request.setProjectId("../evil");

        String result = service.copyEncryptedFile(request, source.toString());
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullWhenTenderIdContainsSlash() throws Exception {
        Path source = Files.createTempFile(tempDir, "enc", ".HzctTbs");
        Files.writeString(source, "content");

        BdcDecryptRequest request = buildRequest();
        request.setTenderId("T001/evil");

        String result = service.copyEncryptedFile(request, source.toString());
        assertThat(result).isNull();
    }
}
