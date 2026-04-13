package com.jy.eleaitender.common.aspect;

import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.AuthException;
import com.jy.eleaitender.common.security.SecurityContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RequireLoginAspect {

    @Around("@annotation(com.jy.eleaitender.common.security.annotation.RequireLogin) || @within(com.jy.eleaitender.common.security.annotation.RequireLogin)")
    public Object checkLogin(ProceedingJoinPoint joinPoint) throws Throwable {
        if (SecurityContextHolder.getLoginUser() == null) {
            throw new AuthException(ResponseCode.UNAUTHORIZED);
        }
        return joinPoint.proceed();
    }
}
