package com.jy.eletender.common.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestBeans.class)
            .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(LoggingAutoConfiguration.class));

    @Test
    void shouldRegisterHttpRequestLogFilterEvenWhenOtherFilterRegistrationBeanExists() {
        contextRunner.run(context -> {
            Collection<FilterRegistrationBean> registrations = context.getBeansOfType(FilterRegistrationBean.class).values();
            boolean hasHttpRequestLogFilter = registrations.stream()
                    .map(FilterRegistrationBean::getFilter)
                    .anyMatch(filter -> filter instanceof HttpRequestLogFilter);
            assertThat(hasHttpRequestLogFilter).isTrue();
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class TestBeans {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        FilterRegistrationBean<Filter> jwtFilterRegistration() {
            FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
            registration.setFilter((request, response, chain) -> chain.doFilter(request, response));
            registration.setOrder(1);
            return registration;
        }
    }
}
