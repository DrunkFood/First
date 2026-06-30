package com.jy.eleaitender.support.service.impl;

import com.jy.eleaitender.common.entity.support.SupSmsCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.support.mapper.SmsCodeMapper;
import com.jy.eleaitender.support.service.SmsGatewayClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsServiceImplTest {

    private static final String PHONE = "13800138000";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SmsCodeMapper smsCodeMapper;

    @Mock
    private SmsGatewayClient smsGatewayClient;

    private SmsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SmsServiceImpl();
        ReflectionTestUtils.setField(service, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(service, "smsCodeMapper", smsCodeMapper);
        ReflectionTestUtils.setField(service, "smsGatewayClient", smsGatewayClient);
    }

    @Test
    void sendSmsCodeDoesNotPersistUsableCodeWhenGatewayFails() {
        when(redisTemplate.hasKey("ai:sms:code:rate:" + PHONE)).thenReturn(false);
        when(smsCodeMapper.selectList(any())).thenReturn(Collections.emptyList());
        doThrow(new BusinessException("验证码发送失败，请稍后重试"))
                .when(smsGatewayClient)
                .sendCode(eq(PHONE), anyString(), eq("LOGIN"));

        assertThrows(BusinessException.class, () -> service.sendSmsCode(PHONE, "LOGIN", "127.0.0.1"));

        verify(valueOperations, never()).set(eq("ai:sms:code:LOGIN:" + PHONE), anyString(), eq(5L), eq(TimeUnit.MINUTES));
        verify(valueOperations, never()).set(eq("ai:sms:code:rate:" + PHONE), eq("1"), eq(60L), eq(TimeUnit.SECONDS));
        verify(smsCodeMapper, never()).insert(any(SupSmsCode.class));
    }

    @Test
    void sendSmsCodeWritesSceneAwareRedisKeyAfterGatewaySuccess() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey("ai:sms:code:rate:" + PHONE)).thenReturn(false);
        when(smsCodeMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(smsGatewayClient.sendCode(eq(PHONE), anyString(), eq("RESET_PWD"))).thenReturn(true);

        String code = service.sendSmsCode(PHONE, "RESET_PWD", "127.0.0.1");

        verify(smsGatewayClient).sendCode(eq(PHONE), anyString(), eq("RESET_PWD"));
        verify(valueOperations).set(eq("ai:sms:code:RESET_PWD:" + PHONE), anyString(), eq(5L), eq(TimeUnit.MINUTES));
        verify(valueOperations).set(eq("ai:sms:code:rate:" + PHONE), eq("1"), eq(60L), eq(TimeUnit.SECONDS));
        assertNull(code);

        ArgumentCaptor<SupSmsCode> smsCodeCaptor = ArgumentCaptor.forClass(SupSmsCode.class);
        verify(smsCodeMapper).insert(smsCodeCaptor.capture());
        assertEquals(PHONE, smsCodeCaptor.getValue().getPhone());
        assertEquals("RESET_PWD", smsCodeCaptor.getValue().getScene());
        assertEquals("UNUSED", smsCodeCaptor.getValue().getStatus());
    }

    @Test
    void sendSmsCodeReturnsCodeWhenGatewaySkipsFormalSend() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey("ai:sms:code:rate:" + PHONE)).thenReturn(false);
        when(smsCodeMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(smsGatewayClient.sendCode(eq(PHONE), anyString(), eq("LOGIN"))).thenReturn(false);

        String code = service.sendSmsCode(PHONE, "LOGIN", "127.0.0.1");

        assertTrue(code.matches("\\d{6}"));
        verify(valueOperations).set(eq("ai:sms:code:LOGIN:" + PHONE), eq(code), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test
    void verifyCodeUsesSceneAwareRedisKeyAndMarksCodeUsed() {
        SupSmsCode smsCode = new SupSmsCode();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ai:sms:code:LOGIN:" + PHONE)).thenReturn("123456");
        when(smsCodeMapper.selectOne(any())).thenReturn(smsCode);

        boolean valid = service.verifyCode(PHONE, "123456", "LOGIN");

        assertTrue(valid);
        verify(redisTemplate).delete("ai:sms:code:LOGIN:" + PHONE);
        verify(smsCodeMapper).updateById(smsCode);
        assertEquals("USED", smsCode.getStatus());
    }
}
