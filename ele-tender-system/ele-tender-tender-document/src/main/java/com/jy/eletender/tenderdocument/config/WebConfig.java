package com.jy.eletender.tenderdocument.config;

import com.jy.eletender.common.constant.CommonConstant;
import com.jy.eletender.common.security.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Set;

@Configuration
public class WebConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(StringRedisTemplate redisTemplate) {
        return new JwtAuthenticationFilter(
                redisTemplate,
                List.of(),
                Set.of(CommonConstant.TOKEN_TYPE_EXTERNAL)
        );
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter jwtAuthenticationFilter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(jwtAuthenticationFilter);
        registration.addUrlPatterns("/api/tender-documents/*");
        registration.setOrder(1);
        return registration;
    }
}
