package com.jy.eletender.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class WebEncodingAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestBeans.class)
            .withConfiguration(AutoConfigurations.of(WebEncodingAutoConfiguration.class));

    @Test
    void shouldRegisterUtf8AndJsonContentTypeFilters() {
        contextRunner.run(context -> {
            FilterRegistrationBean<?> registrationBean = context.getBean("utf8ContentTypeFilterRegistration", FilterRegistrationBean.class);
            assertThat(registrationBean.getFilter()).isInstanceOf(Utf8ContentTypeFilter.class);
            FilterRegistrationBean<?> jsonRegistrationBean = context.getBean("jsonContentTypeFilterRegistration", FilterRegistrationBean.class);
            assertThat(jsonRegistrationBean.getFilter()).isInstanceOf(JsonContentTypeFilter.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class TestBeans {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
