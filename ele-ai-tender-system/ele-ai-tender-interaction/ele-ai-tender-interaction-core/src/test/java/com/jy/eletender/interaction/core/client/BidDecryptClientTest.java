package com.jy.eletender.interaction.core.client;

import com.jy.eleaitender.interaction.core.client.BidDecryptClient;
import com.jy.eletender.common.interaction.dto.BidDecryptStatusResponse;
import com.jy.eletender.common.interaction.dto.BidDecryptSubmitRequest;
import com.jy.eletender.common.interaction.dto.BidDecryptSubmitResponse;
import com.jy.eleaitender.interaction.core.properties.EleTenderInteractionProperties;
import com.jy.eleaitender.interaction.core.support.InteractionRequestSigner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BidDecryptClientTest {

    private RestTemplate restTemplate;
    private BidDecryptClient client;

    @BeforeEach
    void setUp() {
        EleTenderInteractionProperties properties = new EleTenderInteractionProperties();
        properties.setApiBaseUrl("http://localhost:8080");
        properties.setCryptoBaseUrl("http://localhost:8083");
        properties.setAppKey("demo-key");
        properties.setAppSecret("demo-secret");
        restTemplate = new RestTemplate();
        client = new BidDecryptClient(restTemplate, properties, new InteractionRequestSigner(properties));
    }

    @Test
    void shouldSubmitDecryptRequestWithAuthorization() {
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("http://localhost:8083/api/crypto/bid-decrypt/submit"))
                .andExpect(header("Authorization", "Bearer external-token"))
                .andExpect(header("X-App-Key", "demo-key"))
                .andRespond(withSuccess(
                        "{\"code\":200,\"message\":\"操作成功\",\"data\":{\"recordId\":\"R-1\"}}",
                        MediaType.APPLICATION_JSON
                ));

        BidDecryptSubmitRequest request = new BidDecryptSubmitRequest();
        request.setProjectId("P1");
        request.setTenderId("T1");
        request.setBidRecordId("B1");
        request.setBidderPwdStr("pwd");
        request.setFileSha256("sha");

        BidDecryptSubmitResponse response = client.submit("Bearer external-token", request);
        assertThat(response.getRecordId()).isEqualTo("R-1");
        server.verify();
    }

    @Test
    void shouldQueryDecryptStatusByRecordId() {
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("http://localhost:8083/api/crypto/bid-decrypt/status/R-1"))
                .andExpect(header("Authorization", "Bearer external-token"))
                .andExpect(header("X-App-Key", "demo-key"))
                .andRespond(withSuccess(
                        "{\"code\":200,\"message\":\"操作成功\",\"data\":{\"recordId\":\"R-1\",\"status\":\"SUCCESS\"}}",
                        MediaType.APPLICATION_JSON
                ));

        BidDecryptStatusResponse response = client.queryStatus("Bearer external-token", "R-1");
        assertThat(response.getRecordId()).isEqualTo("R-1");
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        server.verify();
    }
}
