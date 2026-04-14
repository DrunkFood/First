package com.jy.eleaitender.support.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eleaitender.common.entity.support.SysOperationLog;
import com.jy.eleaitender.common.logging.OperationLog;
import com.jy.eleaitender.common.security.SecurityContextHolder;
import com.jy.eleaitender.support.mapper.SysOperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Date;

/**
 * 操作日志切面
 * 拦截 @OperationLog 注解，异步记录操作日志到数据库
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    @Autowired
    private SysOperationLogMapper operationLogMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Around("@annotation(com.jy.eleaitender.common.logging.OperationLog)")
    public Object recordLog(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 在主线程中提取上下文信息
        Long userId = SecurityContextHolder.getUserId();
        String userName = SecurityContextHolder.getRealName();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperationLog annotation = method.getAnnotation(OperationLog.class);
        String operation = annotation.value();
        String methodName = signature.getDeclaringTypeName() + "." + method.getName();
        String params = truncateParams(joinPoint.getArgs());
        String ip = getClientIp();

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            // 即使业务异常也记录日志
            long executeTime = System.currentTimeMillis() - startTime;
            saveLogAsync(userId, userName, operation, methodName, params, ip, executeTime);
            throw e;
        }

        long executeTime = System.currentTimeMillis() - startTime;
        saveLogAsync(userId, userName, operation, methodName, params, ip, executeTime);
        return result;
    }

    @Async
    public void saveLogAsync(Long userId, String userName, String operation,
                             String methodName, String params, String ip, long executeTime) {
        try {
            SysOperationLog logEntity = new SysOperationLog();
            logEntity.setUserId(userId);
            logEntity.setUserName(userName);
            logEntity.setOperation(operation);
            logEntity.setMethod(methodName);
            logEntity.setParams(params);
            logEntity.setIp(ip);
            logEntity.setExecuteTime(executeTime);
            logEntity.setCreateTime(new Date());
            operationLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.error("保存操作日志失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取客户端IP（支持反向代理）
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return "unknown";
            }
            HttpServletRequest request = attributes.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            // X-Forwarded-For 可能含多个IP，取第一个
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 截取参数，避免超长
     */
    private String truncateParams(Object[] args) {
        try {
            if (args == null || args.length == 0) {
                return "";
            }
            // 过滤掉 HttpServletRequest/Response 等不可序列化对象
            StringBuilder sb = new StringBuilder();
            for (Object arg : args) {
                if (arg == null) continue;
                String className = arg.getClass().getName();
                if (className.startsWith("jakarta.servlet") || className.startsWith("org.springframework.web")) {
                    continue;
                }
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(objectMapper.writeValueAsString(arg));
            }
            String result = sb.toString();
            return result.length() > 2000 ? result.substring(0, 2000) : result;
        } catch (Exception e) {
            return "参数序列化失败";
        }
    }
}
