package com.jy.eletender.crypto.config;

import com.jy.eletender.common.constant.CommonConstant;
import com.jy.eletender.common.security.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Set;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(StringRedisTemplate redisTemplate) {
        return new JwtAuthenticationFilter(
                redisTemplate,
                List.of("/swagger-ui", "/v3/api-docs", "/webjars"),
                Set.of(CommonConstant.TOKEN_TYPE_EXTERNAL)
        );
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter jwtAuthenticationFilter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(jwtAuthenticationFilter);
        registration.addUrlPatterns("/api/crypto/*");
        registration.setOrder(1);
        return registration;
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(MediaType.APPLICATION_JSON);
    }
}
