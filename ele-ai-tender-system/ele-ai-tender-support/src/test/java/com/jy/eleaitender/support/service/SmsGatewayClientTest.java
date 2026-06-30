package com.jy.eleaitender.support.service;

import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.support.config.SmsGatewayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestOperations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SmsGatewayClientTest {

    private SmsGatewayProperties properties;
    private RestOperations restOperations;
    private SmsGatewayClient client;

    @BeforeEach
    void setUp() {
        properties = new SmsGatewayProperties();
        properties.setUsername("sms-user");
        properties.setPwd("sms-password");
        properties.setExtend("1733");
        properties.setUrl("http://sms.example.test/getsms");

        restOperations = mock(RestOperations.class);
        client = new SmsGatewayClient(properties, restOperations);
    }

    @Test
    void sendCodeDoesNotCallGatewayWhenFormalModeDisabled() {
        properties.setIsformal(false);

        boolean formalSent = assertDoesNotThrow(() -> client.sendCode("13800138000", "123456", "LOGIN"));

        assertFalse(formalSent);

        verifyNoInteractions(restOperations);
    }

    @Test
    void sendCodePostsExpectedFormWhenGatewayReturnsSuccess() {
        properties.setIsformal(true);
        when(restOperations.postForObject(eq(properties.getUrl()), org.mockito.ArgumentMatchers.any(), eq(String.class)))
                .thenReturn("result=0&msg=success");

        boolean formalSent = client.sendCode("13800138000", "123456", "LOGIN");

        assertTrue(formalSent);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<MultiValueMap<String, String>>> entityCaptor =
                ArgumentCaptor.forClass((Class<HttpEntity<MultiValueMap<String, String>>>) (Class<?>) HttpEntity.class);
        verify(restOperations).postForObject(eq(properties.getUrl()), entityCaptor.capture(), eq(String.class));

        HttpEntity<MultiValueMap<String, String>> entity = entityCaptor.getValue();
        assertEquals(MediaType.APPLICATION_FORM_URLENCODED, entity.getHeaders().getContentType());
        MultiValueMap<String, String> form = entity.getBody();
        assertEquals("sms-user", form.getFirst("username"));
        assertEquals("sms-password", form.getFirst("password"));
        assertEquals("13800138000", form.getFirst("mobile"));
        assertEquals("1733", form.getFirst("extend"));
        assertEquals("1", form.getFirst("level"));
        assertTrue(form.getFirst("content").contains("123456"));
        assertTrue(form.getFirst("content").contains("5分钟内有效"));
    }

    @Test
    void sendCodeThrowsWhenGatewayReturnsFailureResult() {
        properties.setIsformal(true);
        when(restOperations.postForObject(eq(properties.getUrl()), org.mockito.ArgumentMatchers.any(), eq(String.class)))
                .thenReturn("result=1&msg=failed");

        assertThrows(BusinessException.class, () -> client.sendCode("13800138000", "123456", "LOGIN"));
    }

    @Test
    void sendCodeThrowsWhenGatewayReturnsBlankResponse() {
        properties.setIsformal(true);
        when(restOperations.postForObject(eq(properties.getUrl()), org.mockito.ArgumentMatchers.any(), eq(String.class)))
                .thenReturn(" ");

        assertThrows(BusinessException.class, () -> client.sendCode("13800138000", "123456", "LOGIN"));
    }

    @Test
    void sendCodeThrowsWhenGatewayCallFails() {
        properties.setIsformal(true);
        when(restOperations.postForObject(eq(properties.getUrl()), org.mockito.ArgumentMatchers.any(), eq(String.class)))
                .thenThrow(new ResourceAccessException("timeout"));

        assertThrows(BusinessException.class, () -> client.sendCode("13800138000", "123456", "LOGIN"));
    }

    @Test
    void parseGatewayResponseKeepsValuesContainingEquals() {
        Map<String, String> response = SmsGatewayClient.parseGatewayResponse("result=0&msg=a=b");

        assertEquals("0", response.get("result"));
        assertEquals("a=b", response.get("msg"));
    }
}
