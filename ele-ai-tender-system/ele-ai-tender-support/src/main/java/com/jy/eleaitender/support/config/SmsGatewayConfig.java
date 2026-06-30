package com.jy.eleaitender.support.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 短信网关客户端配置。
 */
@Configuration
@EnableConfigurationProperties(SmsGatewayProperties.class)
public class SmsGatewayConfig {

    @Bean
    public RestTemplate smsGatewayRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }
}
