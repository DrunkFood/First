package com.jy.eletender.support.service;

import com.jy.eletender.common.entity.support.SysAccessSystem;
import com.jy.eletender.common.util.SignatureUtil;
import com.jy.eletender.support.mapper.SysAccessSystemMapper;
import com.jy.eletender.support.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplSignatureWindowTest {

    @Mock
    private SysAccessSystemMapper accessSystemMapper;

    @Test
    void shouldAllowOlderSignatureWhenConfiguredWindowIsLarger() {
        AuthServiceImpl authService = new AuthServiceImpl();
        ReflectionTestUtils.setField(authService, "accessSystemMapper", accessSystemMapper);
        ReflectionTestUtils.setField(authService, "externalTokenExpireSeconds", 7L * 24 * 60 * 60);
        ReflectionTestUtils.setField(authService, "signatureExpireSeconds", 15L * 60);

        SysAccessSystem system = new SysAccessSystem();
        system.setAppKey("demo-key");
        system.setAppSecret("demo-secret");
        system.setStatus(1);
        when(accessSystemMapper.selectByAppKey("demo-key")).thenReturn(system);

        long oldTimestamp = System.currentTimeMillis() - 10 * 60 * 1000L;
        String signature = SignatureUtil.generateSignature("demo-key", oldTimestamp, "demo-secret");

        boolean valid = authService.verifyExternalSignature("demo-key", oldTimestamp, signature);
        assertThat(valid).isTrue();
    }
}
