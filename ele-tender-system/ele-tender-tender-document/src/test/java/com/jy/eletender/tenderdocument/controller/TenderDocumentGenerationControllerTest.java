package com.jy.eletender.tenderdocument.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentCallbackRequest;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentFileView;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentGeneratePageResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentGenerateResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentGenerationRecordView;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentUnifiedCallbackResponse;
import com.jy.eletender.tenderdocument.enums.TenderDocumentFileType;
import com.jy.eletender.tenderdocument.service.ITenderDocumentGenerationService;
import com.jy.eletender.tenderdocument.support.CurrentExternalUserResolver;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TenderDocumentGenerationControllerTest {

    @Mock
    private ITenderDocumentGenerationService tenderDocumentGenerationService;

    @Mock
    private CurrentExternalUserResolver currentExternalUserResolver;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TenderDocumentGenerationController(tenderDocumentGenerationService, currentExternalUserResolver))
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldGetGeneratePageSuccessfully() throws Exception {
        TenderDocumentGeneratePageResponse response = new TenderDocumentGeneratePageResponse();
        response.setStatus("DRAFT");
        Date compileCompleteTime = new Date(1765528083000L);
        response.setCompileCompleteTime(compileCompleteTime);
        TenderDocumentFileView signedFile = new TenderDocumentFileView();
        signedFile.setFileId(500L);
        signedFile.setFileRole(TenderDocumentFileType.SIGNED_PDF.name());
        TenderDocumentFileView finalPackageFile = new TenderDocumentFileView();
        finalPackageFile.setFileId(600L);
        finalPackageFile.setFileRole(TenderDocumentFileType.FINAL_PACKAGE_FILE.name());
        TenderDocumentFileView compileInfoFile = new TenderDocumentFileView();
        compileInfoFile.setFileId(700L);
        compileInfoFile.setFileRole(TenderDocumentFileType.COMPILE_INFO_PDF.name());
        TenderDocumentGenerationRecordView latestRecord = new TenderDocumentGenerationRecordView();
        latestRecord.setId(900L);
        latestRecord.setGenerateStatus("SUCCESS");
        response.setSignedFile(signedFile);
        response.setFinalPackageFile(finalPackageFile);
        response.setCompileInfoFile(compileInfoFile);
        response.setLatestGenerateRecord(latestRecord);
        when(currentExternalUserResolver.resolve()).thenReturn(buildContext());
        when(tenderDocumentGenerationService.getGeneratePage(eq(100L), any(TenderDocumentUserContext.class))).thenReturn(response);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/tender-documents/generate")
                        .param("tenderDocumentId", "100")
                        .header("Authorization", "Bearer demo-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.signedFile.fileId").value(500L))
                .andExpect(jsonPath("$.data.signedFile.fileRole").value(TenderDocumentFileType.SIGNED_PDF.name()))
                .andExpect(jsonPath("$.data.finalPackageFile.fileRole").value(TenderDocumentFileType.FINAL_PACKAGE_FILE.name()))
                .andExpect(jsonPath("$.data.compileCompleteTime").value(compileCompleteTime.getTime()))
                .andExpect(jsonPath("$.data.latestGenerateRecord.id").value(900L))
                .andExpect(jsonPath("$.data.latestGenerateRecord.generateStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.data.compileInfoFile.fileRole").value(TenderDocumentFileType.COMPILE_INFO_PDF.name()));
    }

    @Test
    void shouldGenerateTenderDocumentSuccessfully() throws Exception {
        TenderDocumentGenerateResponse response = new TenderDocumentGenerateResponse();
        response.setTenderDocumentId(100L);
        response.setVersionNo(2);
        response.setStatus("COMPLETED");
        TenderDocumentFileView finalPackageFile = new TenderDocumentFileView();
        finalPackageFile.setFileId(1000L);
        finalPackageFile.setFileRole(TenderDocumentFileType.FINAL_PACKAGE_FILE.name());
        response.setFinalPackageFile(finalPackageFile);
        TenderDocumentFileView compileInfoFile = new TenderDocumentFileView();
        compileInfoFile.setFileId(2000L);
        compileInfoFile.setFileRole(TenderDocumentFileType.COMPILE_INFO_PDF.name());
        response.setCompileInfoFile(compileInfoFile);
        when(currentExternalUserResolver.resolve()).thenReturn(buildContext());
        when(tenderDocumentGenerationService.generate(eq(100L), any(TenderDocumentUserContext.class))).thenReturn(response);

        mockMvc.perform(post("/api/tender-documents/generate")
                        .param("tenderDocumentId", "100")
                        .header("Authorization", "Bearer demo-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.finalPackageFile.fileId").value(1000L))
                .andExpect(jsonPath("$.data.compileInfoFile.fileId").value(2000L));
    }

    @Test
    void shouldCallbackAllSuccessfully() throws Exception {
        TenderDocumentCallbackRequest request = new TenderDocumentCallbackRequest();
        request.setTenderId("T-01");
        TenderDocumentUnifiedCallbackResponse response = new TenderDocumentUnifiedCallbackResponse();
        response.setSuccess(true);
        response.setMessage("回传完成");
        when(currentExternalUserResolver.resolve()).thenReturn(buildContext());
        when(tenderDocumentGenerationService.callbackAll(eq(100L), any(TenderDocumentCallbackRequest.class), any(TenderDocumentUserContext.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/tender-documents/generate/callback")
                        .param("tenderDocumentId", "100")
                        .header("Authorization", "Bearer demo-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true));
    }

    @Test
    void shouldReturnFailWhenAnyCallbackFails() throws Exception {
        TenderDocumentCallbackRequest request = new TenderDocumentCallbackRequest();
        request.setTenderId("T-01");
        TenderDocumentUnifiedCallbackResponse response = new TenderDocumentUnifiedCallbackResponse();
        response.setSuccess(false);
        response.setMessage("数据包回传失败: 业务系统不可用");
        when(currentExternalUserResolver.resolve()).thenReturn(buildContext());
        when(tenderDocumentGenerationService.callbackAll(eq(100L), any(TenderDocumentCallbackRequest.class), any(TenderDocumentUserContext.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/tender-documents/generate/callback")
                        .param("tenderDocumentId", "100")
                        .header("Authorization", "Bearer demo-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("数据包回传失败: 业务系统不可用"));
    }

    @Test
    void shouldGetGenerationRecordsSuccessfully() throws Exception {
        TenderDocumentGenerationRecordView recordView = new TenderDocumentGenerationRecordView();
        recordView.setId(901L);
        recordView.setGenerateStatus("FAIL");
        recordView.setErrorCode("6012");
        recordView.setErrorMessage("缺少可用的SIGNED_PDF文件");
        when(currentExternalUserResolver.resolve()).thenReturn(buildContext());
        when(tenderDocumentGenerationService.listGenerateRecords(eq(100L), any(TenderDocumentUserContext.class)))
                .thenReturn(List.of(recordView));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/tender-documents/generate/records")
                        .param("tenderDocumentId", "100")
                        .header("Authorization", "Bearer demo-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(901L))
                .andExpect(jsonPath("$.data[0].generateStatus").value("FAIL"))
                .andExpect(jsonPath("$.data[0].errorCode").value("6012"));
    }

    private TenderDocumentUserContext buildContext() {
        TenderDocumentUserContext context = new TenderDocumentUserContext();
        context.setAppKey("app-a");
        context.setUserId("u-1");
        context.setUserName("测试用户");
        context.setEnterpriseCode("913301");
        return context;
    }
}
