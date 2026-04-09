package com.jy.eletender.interaction.autoconfigure.controller;

import com.jy.eletender.common.interaction.dto.ExternalUserInfoResponse;
import com.jy.eletender.common.interaction.dto.IdentityQueryResponse;
import com.jy.eletender.common.interaction.spi.InteractionIdentityService;
import com.jy.eletender.common.interaction.util.InteractionSignatureUtil;
import com.jy.eletender.interaction.autoconfigure.handler.InteractionGlobalExceptionHandler;
import com.jy.eletender.interaction.autoconfigure.web.InteractionSignatureInterceptor;
import com.jy.eletender.interaction.core.client.EleTenderInteractionClient;
import com.jy.eletender.interaction.core.properties.EleTenderInteractionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InteractionIdentityControllerTest {

    private MockMvc mockMvc;
    private EleTenderInteractionProperties properties;

    @BeforeEach
    void setUp() {
        properties = new EleTenderInteractionProperties();
        properties.setAppKey("demo-key");
        properties.setAppSecret("demo-secret");

        EleTenderInteractionClient client = mock(EleTenderInteractionClient.class);
        InteractionIdentityService identityService = mock(InteractionIdentityService.class);

        ExternalUserInfoResponse externalUserInfo = new ExternalUserInfoResponse();
        externalUserInfo.setAppKey("demo-key");
        externalUserInfo.setUserId("U-100");
        externalUserInfo.setUserName("张三");
        externalUserInfo.setEnterpriseId("E-1");
        externalUserInfo.setEnterpriseName("企业A");
        externalUserInfo.setEnterpriseCode("QY001");
        when(client.getCurrentExternalUser("Bearer demo-token")).thenReturn(externalUserInfo);

        IdentityQueryResponse identityResponse = new IdentityQueryResponse();
        identityResponse.setUserId("U-100");
        identityResponse.setUserName("张三");
        when(identityService.queryCurrentIdentity(any())).thenReturn(identityResponse);

        InteractionIdentityController controller = new InteractionIdentityController(client, identityService, null);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(new InteractionSignatureInterceptor(properties))
                .setControllerAdvice(new InteractionGlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldResolveCurrentIdentityFromCurrentElectronicTenderToken() throws Exception {
        long timestamp = System.currentTimeMillis();
        mockMvc.perform(get("/api/eleTender/interaction/identity/current")
                        .header("Authorization", "Bearer demo-token")
                        .header("X-App-Key", "demo-key")
                        .header("X-Timestamp", String.valueOf(timestamp))
                        .header("X-Signature", InteractionSignatureUtil.generateSignature("demo-key", timestamp, "demo-secret")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value("U-100"));
    }
}
