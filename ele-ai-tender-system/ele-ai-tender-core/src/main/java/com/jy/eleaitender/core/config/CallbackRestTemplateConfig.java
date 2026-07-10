package com.jy.eleaitender.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 回调专用RestTemplate配置
 */
@Configuration
public class CallbackRestTemplateConfig {

    @Value("${ele-ai-tender.external.ai-task.callback-connect-timeout:5000}")
    private long connectTimeout;

    @Value("${ele-ai-tender.external.ai-task.callback-read-timeout:10000}")
    private long readTimeout;

    @Bean("callbackRestTemplate")
    public RestTemplate callbackRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) connectTimeout);
        requestFactory.setReadTimeout((int) readTimeout);
        return new RestTemplate(requestFactory);
    }
}
