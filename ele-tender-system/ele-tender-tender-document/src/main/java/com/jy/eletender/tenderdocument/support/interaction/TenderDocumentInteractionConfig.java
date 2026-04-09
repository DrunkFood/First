package com.jy.eletender.tenderdocument.support.interaction;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 交互链路配置。
 * 为业务系统与文件服务调用提供统一超时配置的 RestTemplate。
 */
@Configuration
@EnableConfigurationProperties(TenderDocumentInteractionProperties.class)
public class TenderDocumentInteractionConfig {

    /**
     * 构建交互专用 RestTemplate，避免与系统内其他 HTTP 客户端配置互相影响。
     */
    @Bean
    public RestTemplate tenderDocumentInteractionRestTemplate(TenderDocumentInteractionProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Math.toIntExact(properties.getConnectTimeout().toMillis()));
        factory.setReadTimeout(Math.toIntExact(properties.getReadTimeout().toMillis()));
        return new RestTemplate(factory);
    }
}
