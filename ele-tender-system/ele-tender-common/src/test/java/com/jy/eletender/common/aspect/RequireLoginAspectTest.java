package com.jy.eletender.common.aspect;

import com.jy.eletender.common.enums.ResponseCode;
import com.jy.eletender.common.exception.AuthException;
import com.jy.eletender.common.security.LoginUser;
import com.jy.eletender.common.security.SecurityContextHolder;
import com.jy.eletender.common.security.annotation.RequireLogin;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequireLoginAspectTest {

    private final RequireLoginAspect aspect = new RequireLoginAspect();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    void shouldRejectWhenLoginUserMissing() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(DemoTarget.class.getDeclaredMethod("securedMethod"));

        assertThatThrownBy(() -> aspect.checkLogin(joinPoint))
                .isInstanceOf(AuthException.class)
                .hasMessage(ResponseCode.UNAUTHORIZED.getMessage());
    }

    @Test
    void shouldProceedWhenLoginUserExists() throws Throwable {
        LoginUser loginUser = new LoginUser();
        loginUser.setExternalUserId("user-1");
        SecurityContextHolder.setLoginUser(loginUser);

        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        Method method = DemoTarget.class.getDeclaredMethod("securedMethod");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(joinPoint.proceed()).thenReturn("ok");

        aspect.checkLogin(joinPoint);

        verify(joinPoint).proceed();
    }

    static class DemoTarget {

        @RequireLogin
        public void securedMethod() {
        }
    }
}
