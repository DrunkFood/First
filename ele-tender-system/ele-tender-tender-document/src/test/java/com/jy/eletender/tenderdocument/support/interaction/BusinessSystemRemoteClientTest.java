package com.jy.eletender.tenderdocument.support.interaction;

import com.jy.eletender.common.interaction.constant.InteractionHeaderConstants;
import com.jy.eletender.common.interaction.dto.CaKeysInfoQueryRequest;
import com.jy.eletender.common.interaction.dto.CaKeysInfoResponse;
import com.jy.eletender.common.interaction.dto.InteractionResult;
import com.jy.eletender.common.interaction.dto.ProjectBasicInfoQueryRequest;
import com.jy.eletender.common.interaction.dto.ProjectBasicInfoResponse;
import com.jy.eletender.common.interaction.dto.TenderPackageCallbackRequest;
import com.jy.eletender.common.interaction.enums.InteractionEvalMethod;
import com.jy.eletender.common.interaction.enums.InteractionProjectType;
import com.jy.eletender.common.entity.support.SysAccessLog;
import com.jy.eletender.common.logging.AccessLogPersistenceService;
import com.jy.eletender.common.logging.TraceConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BusinessSystemRemoteClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private StubExternalSystemAccessResolver resolver;
    private BusinessSystemRemoteClient client;
    private AccessLogPersistenceService accessLogPersistenceService;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        resolver = new StubExternalSystemAccessResolver();
        accessLogPersistenceService = mock(AccessLogPersistenceService.class);
        ObjectProvider<AccessLogPersistenceService> provider = mock(ObjectProvider.class);
        Environment environment = mock(Environment.class);
        when(provider.getIfAvailable()).thenReturn(accessLogPersistenceService);
        when(environment.getProperty("spring.application.name", "unknown-service")).thenReturn("ele-tender-tender-document");
        client = new BusinessSystemRemoteClient(restTemplate, resolver, provider, environment);
    }

    @Test
    void shouldQueryProjectBasicInfoWithSignedHeaders() {
        server.expect(requestTo("http://demo-system/api/eleTender/interaction/projects/basic-info"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(InteractionHeaderConstants.APP_KEY, "demo-app"))
                .andExpect(header(InteractionHeaderConstants.TIMESTAMP, org.hamcrest.Matchers.not(org.hamcrest.Matchers.blankOrNullString())))
                .andExpect(header(InteractionHeaderConstants.SIGNATURE, org.hamcrest.Matchers.not(org.hamcrest.Matchers.blankOrNullString())))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer ext-token"))
                .andExpect(header(TraceConstants.TRACE_ID_HEADER, "trace-001"))
                .andRespond(withSuccess("{\"code\":200,\"message\":\"操作成功\",\"data\":{\"projectId\":\"P-100\",\"projectNo\":\"PN-100\",\"projectName\":\"示例项目\",\"projectType\":\"PUBLIC\",\"evalMethod\":\"LOWEST_PRICE\",\"tenderList\":[{\"tenderId\":\"T-01\",\"tenderNo\":\"TN-01\",\"tenderName\":\"一标段\",\"purchaseContext\":\"一标段采购内容\"}]}}",
                        MediaType.APPLICATION_JSON));

        ProjectBasicInfoQueryRequest request = new ProjectBasicInfoQueryRequest();
        request.setBizType(1);
        request.setBizId("BIZ-1");
        request.setProjectId("P-100");

        ProjectBasicInfoResponse response = client.queryProjectBasicInfo("demo-app", "trace-001", "Bearer ext-token", request);

        assertThat(response.getProjectNo()).isEqualTo("PN-100");
        assertThat(response.getProjectType()).isEqualTo(InteractionProjectType.PUBLIC);
        assertThat(response.getEvalMethod()).isEqualTo(InteractionEvalMethod.LOWEST_PRICE);
        assertThat(response.getTenderList()).hasSize(1);
        assertThat(response.getTenderList().get(0).getPurchaseContext()).isEqualTo("一标段采购内容");
        ArgumentCaptor<SysAccessLog> accessLogCaptor = ArgumentCaptor.forClass(SysAccessLog.class);
        verify(accessLogPersistenceService).persist(accessLogCaptor.capture());
        assertThat(accessLogCaptor.getValue().getLogType()).isEqualTo("INTERACTION_OUT");
        assertThat(accessLogCaptor.getValue().getSuccessFlag()).isEqualTo(1);
        assertThat(accessLogCaptor.getValue().getRequestUri()).isEqualTo("http://demo-system/api/eleTender/interaction/projects/basic-info");
        server.verify();
    }

    @Test
    void shouldThrowWhenCallbackReturnsFailure() {
        server.expect(requestTo("http://demo-system/api/eleTender/interaction/callbacks/tender-package"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"code\":500,\"message\":\"业务系统拒绝接收\"}",
                        MediaType.APPLICATION_JSON));

        TenderPackageCallbackRequest request = new TenderPackageCallbackRequest();
        request.setBizType(1);
        request.setBizId("BIZ-1");
        request.setProjectId("P-100");
        request.setFileId(99L);
        request.setFileName("package.zip");

        assertThatThrownBy(() -> client.callbackTenderPackage("demo-app", "trace-001", request))
                .hasMessageContaining("业务系统拒绝接收");
        ArgumentCaptor<SysAccessLog> accessLogCaptor = ArgumentCaptor.forClass(SysAccessLog.class);
        verify(accessLogPersistenceService).persist(accessLogCaptor.capture());
        assertThat(accessLogCaptor.getValue().getLogType()).isEqualTo("INTERACTION_OUT");
        assertThat(accessLogCaptor.getValue().getSuccessFlag()).isEqualTo(0);
        assertThat(accessLogCaptor.getValue().getRequestUri()).isEqualTo("http://demo-system/api/eleTender/interaction/callbacks/tender-package");
    }

    @Test
    void shouldQueryCaKeysInfoWithSignedHeaders() {
        server.expect(requestTo("http://demo-system/api/eleTender/interaction/ca-keys/query"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(InteractionHeaderConstants.APP_KEY, "demo-app"))
                .andExpect(header(InteractionHeaderConstants.TIMESTAMP, org.hamcrest.Matchers.not(org.hamcrest.Matchers.blankOrNullString())))
                .andExpect(header(InteractionHeaderConstants.SIGNATURE, org.hamcrest.Matchers.not(org.hamcrest.Matchers.blankOrNullString())))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer ext-token"))
                .andRespond(withSuccess("{\"code\":200,\"message\":\"操作成功\",\"data\":{\"caKeysInfo\":[{\"encryptOrder\":1,\"userId\":\"u-1\",\"caId\":\"ca-id\",\"caNo\":\"ca-no\",\"publicKey\":\"public-key\"}]}}",
                        MediaType.APPLICATION_JSON));

        CaKeysInfoQueryRequest request = new CaKeysInfoQueryRequest();
        request.setProjectId("P-100");
        request.setTenderId("T-01");
        CaKeysInfoResponse response = client.queryCaKeysInfo("demo-app", "trace-001", "Bearer ext-token", request);

        assertThat(response.getCaKeysInfo()).hasSize(1);
        assertThat(response.getCaKeysInfo().get(0).getPublicKey()).isEqualTo("public-key");
        server.verify();
    }

    private static final class StubExternalSystemAccessResolver extends ExternalSystemAccessResolver {

        private StubExternalSystemAccessResolver() {
            super(null);
        }

        @Override
        public ResolvedExternalSystem resolve(String appKey) {
            return new ResolvedExternalSystem("demo-app", "demo-secret", "http://demo-system");
        }
    }
}
