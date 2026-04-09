package com.jy.eletender.tenderdocument.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentRuleCopyRequest;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentRuleSaveRequest;
import com.jy.eletender.tenderdocument.dto.request.TenderDocumentScoreTypeSaveRequest;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentEvaluationRulesPageResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentRuleResponse;
import com.jy.eletender.tenderdocument.dto.response.TenderDocumentScoreTypeResponse;
import com.jy.eletender.tenderdocument.service.ITenderDocumentRuleService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TenderDocumentRuleControllerTest {

    @Mock
    private ITenderDocumentRuleService tenderDocumentRuleService;

    @Mock
    private CurrentExternalUserResolver currentExternalUserResolver;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TenderDocumentRuleController(tenderDocumentRuleService, currentExternalUserResolver))
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldGetRulePageSuccessfully() throws Exception {
        TenderDocumentEvaluationRulesPageResponse response = new TenderDocumentEvaluationRulesPageResponse();
        response.setEvalMethod("COMPREHENSIVE_SCORE");
        when(currentExternalUserResolver.resolve()).thenReturn(buildContext());
        when(tenderDocumentRuleService.getRulePage(eq(100L), any(TenderDocumentUserContext.class))).thenReturn(response);

        mockMvc.perform(get("/api/tender-documents/evaluation-rules")
                        .param("tenderDocumentId", "100")
                        .header("Authorization", "Bearer demo-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evalMethod").value("COMPREHENSIVE_SCORE"));
    }

    @Test
    void shouldSaveRuleTreeSuccessfully() throws Exception {
        TenderDocumentRuleSaveRequest request = new TenderDocumentRuleSaveRequest();
        request.setTenderId("T-01");
        request.setNodeCategory("CREDIT");
        when(currentExternalUserResolver.resolve()).thenReturn(buildContext());

        mockMvc.perform(put("/api/tender-documents/evaluation-rules/item")
                        .param("tenderDocumentId", "100")
                        .header("Authorization", "Bearer demo-token")
                        .param("tenderId", "T-01")
                        .param("nodeCategory", "CREDIT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(tenderDocumentRuleService).saveRuleTree(eq(100L), eq("T-01"), any(TenderDocumentRuleSaveRequest.class), any(TenderDocumentUserContext.class));
    }

    @Test
    void shouldGetRuleTreeSuccessfully() throws Exception {
        TenderDocumentRuleResponse response = new TenderDocumentRuleResponse();
        response.setTenderId("T-01");
        response.setNodeCategory("CREDIT");
        when(currentExternalUserResolver.resolve()).thenReturn(buildContext());
        when(tenderDocumentRuleService.getRuleTree(eq(100L), eq("T-01"), eq("CREDIT"), any(TenderDocumentUserContext.class)))
                .thenReturn(response);

        mockMvc.perform(get("/api/tender-documents/evaluation-rules/item")
                        .param("tenderDocumentId", "100")
                        .param("tenderId", "T-01")
                        .param("nodeCategory", "CREDIT")
                        .header("Authorization", "Bearer demo-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenderId").value("T-01"));
    }

    @Test
    void shouldCopyRuleTreeSuccessfully() throws Exception {
        TenderDocumentRuleCopyRequest request = new TenderDocumentRuleCopyRequest();
        request.setSourceTenderId("T-01");
        request.setTargetTenderId("T-02");
        request.setTargetTenderName("二标段");
        when(currentExternalUserResolver.resolve()).thenReturn(buildContext());

        mockMvc.perform(post("/api/tender-documents/evaluation-rules/copy")
                        .param("tenderDocumentId", "100")
                        .header("Authorization", "Bearer demo-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(tenderDocumentRuleService).copyRuleTree(eq(100L), any(TenderDocumentRuleCopyRequest.class), any(TenderDocumentUserContext.class));
    }

    @Test
    void shouldGetProjectLevelScoreTypeSuccessfully() throws Exception {
        TenderDocumentScoreTypeResponse response = new TenderDocumentScoreTypeResponse();
        response.setScoreType("ACTUAL");
        when(currentExternalUserResolver.resolve()).thenReturn(buildContext());
        when(tenderDocumentRuleService.getScoreType(eq(100L), any(TenderDocumentUserContext.class))).thenReturn(response);

        mockMvc.perform(get("/api/tender-documents/evaluation-rules/score-type")
                        .param("tenderDocumentId", "100")
                        .header("Authorization", "Bearer demo-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scoreType").value("ACTUAL"));
    }

    @Test
    void shouldUpdateProjectLevelScoreTypeSuccessfully() throws Exception {
        TenderDocumentScoreTypeSaveRequest request = new TenderDocumentScoreTypeSaveRequest();
        request.setScoreType("WEIGHT");
        when(currentExternalUserResolver.resolve()).thenReturn(buildContext());

        mockMvc.perform(put("/api/tender-documents/evaluation-rules/score-type")
                        .param("tenderDocumentId", "100")
                        .header("Authorization", "Bearer demo-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(tenderDocumentRuleService).updateScoreType(eq(100L), any(TenderDocumentScoreTypeSaveRequest.class), any(TenderDocumentUserContext.class));
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
