package com.jy.eletender.support.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eletender.common.entity.crypto.BdcDecryptRequest;
import com.jy.eletender.support.service.ICryptoManageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CryptoManageControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CryptoManageController controller = new CryptoManageController();
        ReflectionTestUtils.setField(controller, "cryptoManageService", new ICryptoManageService() {
            @Override
            public Page<BdcDecryptRequest> getRequestPage(Integer pageNum,
                                                          Integer pageSize,
                                                          String appKey,
                                                          String projectId,
                                                          String tenderId,
                                                          String bidRecordId,
                                                          String status,
                                                          String callbackStatus) {
                BdcDecryptRequest item = new BdcDecryptRequest();
                item.setAppKey(appKey);
                item.setProjectId(projectId);
                item.setTenderId(tenderId);
                item.setBidRecordId(bidRecordId);
                item.setStatus(status);
                item.setCallbackStatus(callbackStatus);
                Page<BdcDecryptRequest> page = new Page<>(pageNum, pageSize);
                page.setRecords(Collections.singletonList(item));
                page.setTotal(1L);
                return page;
            }

            @Override
            public com.jy.eletender.common.entity.crypto.BdcDecryptArtifact getArtifactDetail(Long artifactId) {
                return null;
            }

            @Override
            public void retryRequest(Long requestId) {
            }
        });
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void shouldReturnCryptoRequestPage() throws Exception {
        mockMvc.perform(get("/api/crypto/manage/requests")
                        .accept(MediaType.APPLICATION_JSON)
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("appKey", "demo-app")
                        .param("projectId", "P1")
                        .param("tenderId", "T1")
                        .param("bidRecordId", "B1")
                        .param("status", "FAILED")
                        .param("callbackStatus", "FAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].appKey").value("demo-app"))
                .andExpect(jsonPath("$.data.records[0].projectId").value("P1"))
                .andExpect(jsonPath("$.data.records[0].status").value("FAILED"));
    }
}
