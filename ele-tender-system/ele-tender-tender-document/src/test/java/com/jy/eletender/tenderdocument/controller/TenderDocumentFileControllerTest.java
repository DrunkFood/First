package com.jy.eletender.tenderdocument.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentFileBindRequest;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentFilePageResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentFileView;
import com.jy.eletender.tenderdocument.service.ITenderDocumentFileService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TenderDocumentFileControllerTest {

    @Mock
    private ITenderDocumentFileService tenderDocumentFileService;

    @Mock
    private CurrentExternalUserResolver currentExternalUserResolver;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TenderDocumentFileController(tenderDocumentFileService, currentExternalUserResolver))
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldGetPurchaseFilePageSuccessfully() throws Exception {
        TenderDocumentFilePageResponse response = new TenderDocumentFilePageResponse();
        TenderDocumentFileView fileView = new TenderDocumentFileView();
        fileView.setFileId(1000L);
        fileView.setCreateTime(new Date(1_730_000_000_000L));
        response.setFileList(List.of(fileView));
        when(currentExternalUserResolver.resolve()).thenReturn(buildContext());
        when(tenderDocumentFileService.getPurchaseFilePage(eq(100L), any(TenderDocumentUserContext.class))).thenReturn(response);

                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/tender-documents/purchase-file")
                        .param("tenderDocumentId", "100")
                        .header("Authorization", "Bearer demo-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileList[0].fileId").value(1000L))
                .andExpect(jsonPath("$.data.fileList[0].createTime").exists());
    }

    @Test
    void shouldBindPurchaseFileSuccessfully() throws Exception {
        TenderDocumentFileBindRequest request = new TenderDocumentFileBindRequest();
        request.setFileId(1000L);
        request.setFileName("采购文件.pdf");
        request.setFileSize(1024L);
        when(currentExternalUserResolver.resolve()).thenReturn(buildContext());

        mockMvc.perform(post("/api/tender-documents/purchase-file/bind")
                        .param("tenderDocumentId", "100")
                        .header("Authorization", "Bearer demo-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(tenderDocumentFileService).bindPurchaseFile(eq(100L), any(TenderDocumentFileBindRequest.class), any(TenderDocumentUserContext.class));
    }

    @Test
    void shouldGetSignedFilePageSuccessfully() throws Exception {
        TenderDocumentFilePageResponse response = new TenderDocumentFilePageResponse();
        TenderDocumentFileView fileView = new TenderDocumentFileView();
        fileView.setFileId(2000L);
        response.setFileList(List.of(fileView));
        when(currentExternalUserResolver.resolve()).thenReturn(buildContext());
        when(tenderDocumentFileService.getSignedFilePage(eq(100L), any(TenderDocumentUserContext.class))).thenReturn(response);

        mockMvc.perform(get("/api/tender-documents/purchase-file/signed")
                        .param("tenderDocumentId", "100")
                        .header("Authorization", "Bearer demo-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileList[0].fileId").value(2000L));
    }

    @Test
    void shouldDeleteSignedFileSuccessfully() throws Exception {
        when(currentExternalUserResolver.resolve()).thenReturn(buildContext());

        mockMvc.perform(delete("/api/tender-documents/purchase-file/signed")
                        .param("tenderDocumentId", "100")
                        .header("Authorization", "Bearer demo-token")
                        .param("tenderId", "T-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(tenderDocumentFileService).removeSignedFile(eq(100L), eq("T-01"), any(TenderDocumentUserContext.class));
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
