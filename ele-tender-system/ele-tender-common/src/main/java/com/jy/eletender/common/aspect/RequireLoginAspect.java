package com.jy.eletender.common.aspect;

import com.jy.eletender.common.enums.ResponseCode;
import com.jy.eletender.common.exception.AuthException;
import com.jy.eletender.common.security.SecurityContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RequireLoginAspect {

    @Around("@annotation(com.jy.eletender.common.security.annotation.RequireLogin) || @within(com.jy.eletender.common.security.annotation.RequireLogin)")
    public Object checkLogin(ProceedingJoinPoint joinPoint) throws Throwable {
        if (SecurityContextHolder.getLoginUser() == null) {
            throw new AuthException(ResponseCode.UNAUTHORIZED);
        }
        return joinPoint.proceed();
    }
}
