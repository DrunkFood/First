package com.jy.eleaitender.support.service.impl;

import com.jy.eleaitender.common.dto.request.PhoneLoginRequest;
import com.jy.eleaitender.common.dto.request.ResetPasswordRequest;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.support.mapper.SysUserMapper;
import com.jy.eleaitender.support.service.ISmsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplSmsSceneTest {

    private static final String PHONE = "13800138000";
    private static final String CODE = "123456";

    @Mock
    private ISmsService smsService;

    @Mock
    private SysUserMapper userMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void phoneLoginVerifiesLoginScene() {
        PhoneLoginRequest request = new PhoneLoginRequest();
        request.setPhone(PHONE);
        request.setCode(CODE);
        request.setAgreementAccepted(true);
        request.setAcceptedAgreementTypes(List.of("USER_SERVICE_AGREEMENT", "PRIVACY_POLICY"));
        request.setAgreementVersion("2026-07-31");
        when(smsService.verifyCode(PHONE, CODE, "LOGIN")).thenReturn(false);

        assertThrows(BusinessException.class, () -> authService.phoneLogin(request));

        verify(smsService).verifyCode(PHONE, CODE, "LOGIN");
    }

    @Test
    void resetPasswordVerifiesResetPasswordScene() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setPhone(PHONE);
        request.setCode(CODE);
        when(smsService.verifyCode(PHONE, CODE, "RESET_PWD")).thenReturn(false);

        assertThrows(BusinessException.class, () -> authService.resetPasswordByPhone(request));

        verify(smsService).verifyCode(PHONE, CODE, "RESET_PWD");
    }
}
