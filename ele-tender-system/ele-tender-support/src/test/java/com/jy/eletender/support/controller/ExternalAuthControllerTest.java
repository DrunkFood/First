package com.jy.eletender.support.controller;

import com.jy.eletender.support.converter.ExternalTokenRequestMapper;
import com.jy.eletender.support.model.external.ExternalTokenIssueResult;
import com.jy.eletender.support.model.external.ExternalUserInfoView;
import com.jy.eletender.support.service.IAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExternalAuthControllerTest {

    private MockMvc mockMvc;
    private IAuthService authService;

    @BeforeEach
    void setUp() {
        authService = mock(IAuthService.class);

        ExternalAuthController controller = new ExternalAuthController();
        ReflectionTestUtils.setField(controller, "authService", authService);
        ReflectionTestUtils.setField(controller, "tokenRequestMapper", new ExternalTokenRequestMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void shouldReturnExternalTokenWhenSignatureIsValid() throws Exception {
        when(authService.verifyExternalSignature(anyString(), anyLong(), anyString())).thenReturn(true);
        when(authService.getExternalToken(anyString(), any())).thenReturn(new ExternalTokenIssueResult("abc", 1800L));

        mockMvc.perform(post("/api/external/token")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-App-Key", "demo-key")
                        .header("X-Timestamp", System.currentTimeMillis())
                        .header("X-Signature", "demo-signature")
                        .content("{\"userId\":\"U1\",\"userName\":\"张三\",\"enterpriseId\":\"E1\",\"enterpriseName\":\"企业A\",\"enterpriseCode\":\"QY001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("abc"));
    }

    @Test
    void shouldRejectExternalTokenRequestWhenContentTypeIsXml() throws Exception {
        mockMvc.perform(post("/api/external/token")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_XML)
                        .header("X-App-Key", "demo-key")
                        .header("X-Timestamp", System.currentTimeMillis())
                        .header("X-Signature", "demo-signature")
                        .content("<ExternalTokenRequest>" +
                                "<userId>U1</userId>" +
                                "<userName>张三</userName>" +
                                "<enterpriseId>E1</enterpriseId>" +
                                "<enterpriseName>企业A</enterpriseName>" +
                                "<enterpriseCode>QY001</enterpriseCode>" +
                                "</ExternalTokenRequest>"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void shouldReturnExternalUserInfo() throws Exception {
        ExternalUserInfoView view = new ExternalUserInfoView();
        view.setAppKey("demo-key");
        view.setUserId("U1");
        view.setUserName("张三");
        when(authService.getExternalUserInfo()).thenReturn(view);

        mockMvc.perform(get("/api/external/userinfo")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value("U1"));
    }
}
