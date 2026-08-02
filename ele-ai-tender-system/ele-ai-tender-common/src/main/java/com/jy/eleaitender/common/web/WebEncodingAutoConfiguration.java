package com.jy.eleaitender.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

/**
 * 全局 Web 编码自动配置
 */
@Slf4j
@AutoConfiguration
public class WebEncodingAutoConfiguration {

    @Bean(name = "jsonContentTypeFilterRegistration")
    @ConditionalOnMissingBean(name = "jsonContentTypeFilterRegistration")
    public FilterRegistrationBean<JsonContentTypeFilter> jsonContentTypeFilterRegistration(Environment environment,
                                                                                           ObjectMapper objectMapper) {
        String serviceName = environment.getProperty("spring.application.name", "unknown-service");
        log.info("JSON content-type filter init service={}", serviceName);
        FilterRegistrationBean<JsonContentTypeFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new JsonContentTypeFilter(objectMapper));
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registrationBean;
    }

    @Bean(name = "utf8ContentTypeFilterRegistration")
    @ConditionalOnMissingBean(name = "utf8ContentTypeFilterRegistration")
    public FilterRegistrationBean<Utf8ContentTypeFilter> utf8ContentTypeFilterRegistration(Environment environment) {
        String serviceName = environment.getProperty("spring.application.name", "unknown-service");
        log.info("UTF-8 encoding filter init service={}", serviceName);
        FilterRegistrationBean<Utf8ContentTypeFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new Utf8ContentTypeFilter());
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        return registrationBean;
    }
}
