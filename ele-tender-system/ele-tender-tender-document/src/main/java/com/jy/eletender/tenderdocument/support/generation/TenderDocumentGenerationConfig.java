package com.jy.eletender.tenderdocument.support.generation;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TenderDocumentVersionProperties.class)
public class TenderDocumentGenerationConfig {
}
