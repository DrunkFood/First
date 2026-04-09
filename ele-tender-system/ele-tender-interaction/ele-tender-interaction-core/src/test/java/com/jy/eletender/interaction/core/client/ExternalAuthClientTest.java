package com.jy.eletender.interaction.core.client;

import com.jy.eletender.common.interaction.dto.ExternalTokenRequest;
import com.jy.eletender.common.interaction.dto.ExternalTokenResponse;
import com.jy.eletender.interaction.core.properties.EleTenderInteractionProperties;
import com.jy.eletender.interaction.core.support.InteractionRequestSigner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ExternalAuthClientTest {

    private RestTemplate restTemplate;
    private ExternalAuthClient client;

    @BeforeEach
    void setUp() {
        EleTenderInteractionProperties properties = new EleTenderInteractionProperties();
        properties.setApiBaseUrl("http://localhost:8080");
        properties.setAppKey("demo-key");
        properties.setAppSecret("demo-secret");
        restTemplate = new RestTemplate();
        client = new ExternalAuthClient(restTemplate, properties, new InteractionRequestSigner(properties));
    }

    @Test
    void shouldCallExternalTokenEndpointWithSignedHeaders() {
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("http://localhost:8080/api/external/token"))
                .andExpect(header("X-App-Key", "demo-key"))
                .andExpect(header("X-Timestamp", org.hamcrest.Matchers.not(org.hamcrest.Matchers.blankOrNullString())))
                .andExpect(header("X-Signature", org.hamcrest.Matchers.not(org.hamcrest.Matchers.blankOrNullString())))
                .andExpect(header("Content-Type", org.hamcrest.Matchers.containsString(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(header("Accept", org.hamcrest.Matchers.containsString(MediaType.APPLICATION_JSON_VALUE)))
                .andRespond(withSuccess("{\"code\":200,\"message\":\"操作成功\",\"data\":{\"token\":\"abc\",\"expireIn\":1800}}",
                        MediaType.APPLICATION_JSON));

        ExternalTokenRequest request = new ExternalTokenRequest();
        request.setUserId("U1");
        request.setUserName("张三");
        request.setEnterpriseId("E1");
        request.setEnterpriseName("企业A");
        request.setEnterpriseCode("QY001");

        ExternalTokenResponse response = client.getExternalToken(request);
        assertThat(response.getToken()).isEqualTo("abc");
        server.verify();
    }
}
