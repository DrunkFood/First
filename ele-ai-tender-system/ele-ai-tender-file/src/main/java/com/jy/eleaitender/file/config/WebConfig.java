package com.jy.eleaitender.file.config;

import com.jy.eleaitender.common.aspect.RequireLoginAspect;
import com.jy.eleaitender.common.constant.CommonConstant;
import com.jy.eleaitender.common.security.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Set;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class WebConfig {

    /**
     * 文件服务 Bearer Token 认证过滤器
     * 覆盖 /api/file/* 路径，接受外部系统(EXTERNAL)和内部服务(INTERNAL/SERVICE)的JWT
     */
    @Bean
    public JwtAuthenticationFilter fileJwtAuthenticationFilter(StringRedisTemplate redisTemplate) {
        return new JwtAuthenticationFilter(
                redisTemplate,
                List.of(),
                Set.of(CommonConstant.TOKEN_TYPE_EXTERNAL, CommonConstant.TOKEN_TYPE_INTERNAL, CommonConstant.TOKEN_TYPE_SERVICE)
        );
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> fileJwtFilterRegistration(JwtAuthenticationFilter fileJwtAuthenticationFilter) {
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
