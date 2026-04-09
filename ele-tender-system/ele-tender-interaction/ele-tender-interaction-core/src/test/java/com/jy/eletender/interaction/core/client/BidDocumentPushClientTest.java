package com.jy.eletender.interaction.core.client;

import com.jy.eletender.common.interaction.dto.BidDocumentPushRequest;
import com.jy.eletender.common.interaction.dto.BidDocumentPushResponse;
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

class BidDocumentPushClientTest {

    private RestTemplate restTemplate;
    private BidDocumentPushClient client;

    @BeforeEach
    void setUp() {
        EleTenderInteractionProperties properties = new EleTenderInteractionProperties();
        properties.setApiBaseUrl("http://localhost:8080");
        properties.setCryptoBaseUrl("http://localhost:8083");
        properties.setAppKey("demo-key");
        properties.setAppSecret("demo-secret");
        restTemplate = new RestTemplate();
        client = new BidDocumentPushClient(restTemplate, properties, new InteractionRequestSigner(properties));
    }

    @Test
    void shouldCallCryptoBidDocumentPushWithAuthorization() {
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("http://localhost:8083/api/crypto/bid-document/push"))
                .andExpect(header("X-App-Key", "demo-key"))
                .andExpect(header("Authorization", "Bearer external-token"))
                .andExpect(header("Content-Type", org.hamcrest.Matchers.containsString(MediaType.APPLICATION_JSON_VALUE)))
                .andRespond(withSuccess(
                        "{\"code\":200,\"message\":\"操作成功\",\"data\":{\"fileId\":101,\"uploadResult\":\"SUCCESS\"}}",
                        MediaType.APPLICATION_JSON
                ));

        BidDocumentPushRequest request = new BidDocumentPushRequest();
        request.setFileId(101L);
        request.setFileSha256("sha");

        BidDocumentPushResponse response = client.push("Bearer external-token", request);
        assertThat(response.getFileId()).isEqualTo(101L);
        assertThat(response.getUploadResult()).isEqualTo("SUCCESS");
        server.verify();
    }
}
