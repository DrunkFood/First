package com.jy.eletender.crypto.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class CryptoRestTemplateConfig {

    @Bean
    public RestTemplate cryptoRestTemplate() {
        return new RestTemplate();
    }
}
