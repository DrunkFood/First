package com.jy.eletender.file.config;

import com.jy.eletender.common.aspect.RequireLoginAspect;
import com.jy.eletender.common.constant.CommonConstant;
import com.jy.eletender.common.security.JwtAuthenticationFilter;
import com.jy.eletender.file.security.RequestParamJwtAuthenticationFilter;
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
                Set.of(CommonConstant.TOKEN_TYPE_EXTERNAL),
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

    @Bean
    public RequireLoginAspect requireLoginAspect() {
        return new RequireLoginAspect();
    }
}
