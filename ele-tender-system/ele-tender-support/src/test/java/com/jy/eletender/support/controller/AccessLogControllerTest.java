package com.jy.eletender.support.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eletender.common.entity.support.SysAccessLog;
import com.jy.eletender.support.service.IAccessLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccessLogControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AccessLogController controller = new AccessLogController();
        ReflectionTestUtils.setField(controller, "accessLogService", new IAccessLogService() {
            @Override
            public Page<SysAccessLog> getAccessLogPage(Integer pageNum,
                                                       Integer pageSize,
                                                       String traceId,
                                                       String serviceName,
                                                       Integer statusCode,
                                                       String bizType,
                                                       String bizId,
                                                       String projectId,
                                                       String tenderId,
                                                       Date startTime,
                                                       Date endTime) {
                SysAccessLog record = new SysAccessLog();
                record.setTraceId(traceId);
                record.setServiceName(serviceName);
                record.setStatusCode(statusCode);
                record.setBizType(bizType);
                record.setBizId(bizId);
                record.setProjectId(projectId);
                record.setTenderId(tenderId);

                Page<SysAccessLog> page = new Page<SysAccessLog>(pageNum, pageSize);
                page.setRecords(Collections.singletonList(record));
                page.setTotal(1L);
                return page;
            }
        });
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void shouldReturnAccessLogPageWithFilters() throws Exception {
        mockMvc.perform(get("/api/access-logs")
                        .accept(MediaType.APPLICATION_JSON)
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("traceId", "trace-001")
                        .param("serviceName", "ele-tender-support")
                        .param("statusCode", "200")
                        .param("bizType", "1")
                        .param("bizId", "BIZ-1")
                        .param("projectId", "P-1")
                        .param("tenderId", "T-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].traceId").value("trace-001"))
                .andExpect(jsonPath("$.data.records[0].serviceName").value("ele-tender-support"))
                .andExpect(jsonPath("$.data.records[0].statusCode").value(200))
                .andExpect(jsonPath("$.data.records[0].bizId").value("BIZ-1"));
    }
}
