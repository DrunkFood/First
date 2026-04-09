package com.jy.eletender.tenderdocument.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentEntryRequest;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentEntryResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentOverviewResponse;
import com.jy.eletender.tenderdocument.enums.TenderDocumentStatus;
import com.jy.eletender.tenderdocument.enums.TenderDocumentStepCode;
import com.jy.eletender.tenderdocument.service.ITenderDocumentService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TenderDocumentControllerTest {

    @Mock
    private ITenderDocumentService tenderDocumentService;

    @Mock
    private CurrentExternalUserResolver currentExternalUserResolver;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TenderDocumentController(tenderDocumentService, currentExternalUserResolver))
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldEnterTenderDocumentSuccessfully() throws Exception {
        TenderDocumentEntryRequest request = new TenderDocumentEntryRequest();
        request.setProjectId("P-100");
        request.setTenderId("T-01");
        request.setBizType(1);
        request.setBizId("BIZ-100");
        TenderDocumentEntryResponse response = new TenderDocumentEntryResponse();
        response.setTenderDocumentId(100L);
        response.setStatus(TenderDocumentStatus.DRAFT.name());
        response.setCurrentStepCode(TenderDocumentStepCode.BASIC_INFO.name());

        when(currentExternalUserResolver.resolve()).thenReturn(buildContext());
        when(tenderDocumentService.enter(any(TenderDocumentEntryRequest.class), any(TenderDocumentUserContext.class))).thenReturn(response);

        mockMvc.perform(post("/api/tender-documents/entry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenderDocumentId").value(100L))
                .andExpect(jsonPath("$.data.currentStepCode").value("BASIC_INFO"));
    }

    @Test
    void shouldGetTenderDocumentOverviewSuccessfully() throws Exception {
        TenderDocumentOverviewResponse response = new TenderDocumentOverviewResponse();
        response.setTenderDocumentId(100L);
        response.setStatus("DRAFT");
        when(currentExternalUserResolver.resolve()).thenReturn(buildContext());
        when(tenderDocumentService.getOverview(eq(100L), any(TenderDocumentUserContext.class))).thenReturn(response);

        mockMvc.perform(get("/api/tender-documents/overview")
                        .param("tenderDocumentId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
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
