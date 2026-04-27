package com.jy.eleaitender.support.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 内部AI服务客户端自动配置
 * 仅在配置了 internal.ai-service.base-url 时生效
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(InternalAiServiceProperties.class)
@ConditionalOnProperty(prefix = InternalAiServiceProperties.PREFIX, name = "base-url")
public class InternalAiServiceClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public InternalAiServiceClient internalAiServiceClient(
            InternalAiServiceProperties properties,
            RestTemplateBuilder restTemplateBuilder) {
        log.info("初始化内部AI服务客户端: baseUrl={}", properties.getBaseUrl());

        RestTemplate restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(properties.getConnectTimeout()))
                .setReadTimeout(Duration.ofMillis(properties.getReadTimeout()))
                .build();

        return new InternalAiServiceClient(restTemplate, properties);
    }
}
