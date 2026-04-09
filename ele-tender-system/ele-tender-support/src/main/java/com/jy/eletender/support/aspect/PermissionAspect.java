package com.jy.eletender.support.aspect;

import com.jy.eletender.common.constant.RedisKeyConstant;
import com.jy.eletender.common.enums.ResponseCode;
import com.jy.eletender.common.exception.AuthException;
import com.jy.eletender.common.security.LoginUser;
import com.jy.eletender.common.security.SecurityContextHolder;
import com.jy.eletender.support.security.annotation.RequirePermission;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * 权限校验切面
 */
@Slf4j
@Aspect
@Component
public class PermissionAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Around("@annotation(com.jy.eletender.support.security.annotation.RequirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        LoginUser loginUser = SecurityContextHolder.getLoginUser();
        if (loginUser == null) {
            throw new AuthException(ResponseCode.UNAUTHORIZED);
        }

        // 获取方法上的权限注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequirePermission requirePermission = method.getAnnotation(RequirePermission.class);
        String permission = requirePermission.value();

        // 从Redis获取用户权限列表
        String redisKey = RedisKeyConstant.USER_PERMISSIONS_PREFIX + loginUser.getUserId();
        Set<String> permissions = redisTemplate.opsForSet().members(redisKey);

        if (permissions == null || !permissions.contains(permission)) {
            log.warn("用户[{}]无权限访问[{}], 需要权限: {}", loginUser.getUsername(), 
                    method.getName(), permission);
            throw new AuthException(ResponseCode.PERMISSION_DENIED);
        }

        return joinPoint.proceed();
    }
}
