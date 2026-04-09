package com.jy.eletender.file.controller;

import com.jy.eletender.common.dto.response.FileUploadResponse;
import com.jy.eletender.common.enums.ResponseCode;
import com.jy.eletender.common.exception.FileException;
import com.jy.eletender.common.util.JwtUtil;
import com.jy.eletender.file.config.WebConfig;
import com.jy.eletender.file.handler.GlobalExceptionHandler;
import com.jy.eletender.file.service.IFileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EsignFileController.class)
class EsignFileControllerTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({EsignFileController.class, WebConfig.class, GlobalExceptionHandler.class})
    static class TestApplication {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IFileStorageService fileStorageService;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = org.mockito.Mockito.mock(ValueOperations.class);

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldUploadFileWhenTokenProvidedInRequestParameter() throws Exception {
        String token = JwtUtil.generateExternalToken("esign-app", "ext-user-1", "签章用户", "ent-1", "测试企业", "913301");
        when(valueOperations.get("external:token:esign-app:ext-user-1:" + JwtUtil.parseToken(token).getId())).thenReturn(token);
        when(fileStorageService.upload(any(), eq("esign")))
                .thenReturn(new FileUploadResponse(11L, "signed.pdf", 5L, "sha-11"));

        MockMultipartFile file = new MockMultipartFile("file", "signed.pdf", "application/pdf", "hello".getBytes());

        mockMvc.perform(multipart("/api/file/esign/upload")
                        .file(file)
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.fileId").value(11))
                .andExpect(jsonPath("$.data.fileName").value("signed.pdf"));

        verify(fileStorageService).upload(any(), eq("esign"));
    }

    @Test
    void shouldRejectUploadWhenTokenMissing() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "signed.pdf", "application/pdf", "hello".getBytes());

        mockMvc.perform(multipart("/api/file/esign/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));

        verify(fileStorageService, never()).upload(any(), eq("esign"));
    }

    @Test
    void shouldKeepCurrentFileTypeValidationRules() throws Exception {
        String token = JwtUtil.generateExternalToken("esign-app", "ext-user-2", "签章用户2", "ent-2", "测试企业2", "913302");
        when(valueOperations.get("external:token:esign-app:ext-user-2:" + JwtUtil.parseToken(token).getId())).thenReturn(token);
        when(fileStorageService.upload(any(), eq("esign")))
                .thenThrow(new FileException(ResponseCode.FILE_TYPE_NOT_ALLOWED, "不支持的文件格式"));

        MockMultipartFile file = new MockMultipartFile("file", "signed.exe", "application/octet-stream", "hello".getBytes());

        mockMvc.perform(multipart("/api/file/esign/upload")
                        .file(file)
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(5003));
    }
}
