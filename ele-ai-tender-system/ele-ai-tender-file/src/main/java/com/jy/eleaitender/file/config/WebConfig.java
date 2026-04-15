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

    @Bean
    public JwtAuthenticationFilter esignJwtAuthenticationFilter(StringRedisTemplate redisTemplate) {
        return new RequestParamJwtAuthenticationFilter(
                redisTemplate,
                List.of(),
                Set.of(CommonConstant.TOKEN_TYPE_EXTERNAL, CommonConstant.TOKEN_TYPE_INTERNAL);
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> fileJwtFilterRegistration(
            JwtAuthenticationFilter fileJwtAuthenticationFilter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(fileJwtAuthenticationFilter);
        registration.addUrlPatterns("/api/file/*");
        registration.setOrder(1);
        return registration;
    }

    @Bean
    public RequireLoginAspect requireLoginAspect() {
        return new RequireLoginAspect();
    }
}
