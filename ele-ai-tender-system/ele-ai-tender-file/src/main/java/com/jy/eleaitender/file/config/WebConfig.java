package com.jy.eleaitender.file.config;

import com.jy.eleaitender.common.aspect.RequireLoginAspect;
import com.jy.eleaitender.common.constant.CommonConstant;
import com.jy.eleaitender.common.security.JwtAuthenticationFilter;
import com.jy.eleaitender.file.security.RequestParamJwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.List;
import java.util.Set;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class WebConfig {

    /**
     * esign 专用过滤器：从请求参数获取 token，支持 INTERNAL + EXTERNAL 类型
     */
    @Bean
    public JwtAuthenticationFilter esignJwtAuthenticationFilter(StringRedisTemplate redisTemplate) {
        return new RequestParamJwtAuthenticationFilter(
                redisTemplate,
                List.of(),
                Set.of(CommonConstant.TOKEN_TYPE_EXTERNAL, CommonConstant.TOKEN_TYPE_INTERNAL),
                "token"
        );
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> esignJwtFilterRegistration(
            JwtAuthenticationFilter esignJwtAuthenticationFilter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(esignJwtAuthenticationFilter);
        registration.addUrlPatterns("/api/file/esign/*");
        registration.setOrder(1);
        return registration;
    }

    /**
     * 文件服务 Bearer Token 认证过滤器
     * 覆盖 /api/file/* 路径（esign 除外，esign 使用独立的参数token过滤器）
     */
    @Bean
    public JwtAuthenticationFilter fileJwtAuthenticationFilter(StringRedisTemplate redisTemplate) {
        return new JwtAuthenticationFilter(
                redisTemplate,
                List.of("/api/file/esign"),
                Set.of(CommonConstant.TOKEN_TYPE_INTERNAL)
        );
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> fileJwtFilterRegistration(
            JwtAuthenticationFilter fileJwtAuthenticationFilter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(fileJwtAuthenticationFilter);
        registration.addUrlPatterns("/api/file/*");
        registration.setOrder(2);
        return registration;
    }

    @Bean
    public RequireLoginAspect requireLoginAspect() {
        return new RequireLoginAspect();
    }
}
