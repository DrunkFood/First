package com.jy.eleaitender.interaction.autoconfigure;

import com.jy.eleaitender.common.interaction.spi.InteractionAiTaskResultReceiveService;
import com.jy.eleaitender.common.interaction.spi.InteractionEventLogger;
import com.jy.eleaitender.interaction.autoconfigure.controller.InteractionAiTaskResultCallbackController;
import com.jy.eleaitender.interaction.autoconfigure.handler.InteractionAiGlobalExceptionHandler;
import com.jy.eleaitender.interaction.autoconfigure.logging.DefaultInteractionAiEventLogger;
import com.jy.eleaitender.interaction.autoconfigure.web.InteractionAiSignatureInterceptor;
import com.jy.eleaitender.interaction.autoconfigure.web.InteractionAiWebMvcConfigurer;
import com.jy.eleaitender.interaction.core.client.AiTaskClient;
import com.jy.eleaitender.interaction.core.client.ExternalAuthClient;
import com.jy.eleaitender.interaction.core.client.ExternalUserInfoClient;
import com.jy.eleaitender.interaction.core.client.FileClient;
import com.jy.eleaitender.interaction.core.properties.EleAiTenderInteractionProperties;
import com.jy.eleaitender.interaction.core.support.InteractionRequestSigner;
import com.jy.eleaitender.interaction.core.support.InteractionRestTemplateFactory;
import com.jy.eleaitender.interaction.core.support.OutboundLogInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 电子标交互自动配置
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({EleAiTenderInteractionProperties.class, InteractionAiControllerProperties.class})
@ConditionalOnProperty(prefix = "ele-ai-tender.interaction", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EleAiTenderInteractionAutoConfiguration {

    static final String INTERACTION_REST_TEMPLATE_BEAN_NAME = "interactionRestTemplate";

    @Bean
    @ConditionalOnMissingBean
    public InteractionRequestSigner interactionRequestSigner(EleAiTenderInteractionProperties properties) {
        return new InteractionRequestSigner(properties);
    }

    @Bean(INTERACTION_REST_TEMPLATE_BEAN_NAME)
    @ConditionalOnMissingBean(name = INTERACTION_REST_TEMPLATE_BEAN_NAME)
    public RestTemplate interactionRestTemplate(EleAiTenderInteractionProperties properties,
                                                ObjectProvider<InteractionEventLogger> eventLoggerProvider) {
        return InteractionRestTemplateFactory.create(properties, new OutboundLogInterceptor(eventLoggerProvider.getIfAvailable()));
    }

    @Bean
    @ConditionalOnMissingBean
    public AiTaskClient aiTaskClient(@Qualifier(INTERACTION_REST_TEMPLATE_BEAN_NAME) RestTemplate interactionRestTemplate,
                                     EleAiTenderInteractionProperties properties) {
        return new AiTaskClient(interactionRestTemplate, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ExternalAuthClient externalAuthClient(@Qualifier(INTERACTION_REST_TEMPLATE_BEAN_NAME) RestTemplate interactionRestTemplate,
                                                 EleAiTenderInteractionProperties properties,
                                                 InteractionRequestSigner interactionRequestSigner) {
        return new ExternalAuthClient(interactionRestTemplate, properties, interactionRequestSigner);
    }

    @Bean
    @ConditionalOnMissingBean
    public ExternalUserInfoClient externalUserInfoClient(@Qualifier(INTERACTION_REST_TEMPLATE_BEAN_NAME) RestTemplate interactionRestTemplate,
                                                         EleAiTenderInteractionProperties properties,
                                                         InteractionRequestSigner interactionRequestSigner) {
        return new ExternalUserInfoClient(interactionRestTemplate, properties, interactionRequestSigner);
    }

    @Bean
    @ConditionalOnMissingBean
    public FileClient fileClient(@Qualifier(INTERACTION_REST_TEMPLATE_BEAN_NAME) RestTemplate interactionRestTemplate,
                                 EleAiTenderInteractionProperties properties) {
        return new FileClient(interactionRestTemplate, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public InteractionAiSignatureInterceptor interactionSignatureInterceptor(EleAiTenderInteractionProperties properties) {
        return new InteractionAiSignatureInterceptor(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public InteractionAiWebMvcConfigurer interactionWebMvcConfigurer(InteractionAiSignatureInterceptor signatureInterceptor) {
        return new InteractionAiWebMvcConfigurer(signatureInterceptor);
    }

    @Bean
    @ConditionalOnMissingBean
    public InteractionAiGlobalExceptionHandler interactionGlobalExceptionHandler() {
        return new InteractionAiGlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public InteractionEventLogger interactionEventLogger() {
        return new DefaultInteractionAiEventLogger();
    }

    @Bean
    @ConditionalOnBean(InteractionAiTaskResultReceiveService.class)
    @ConditionalOnMissingBean
    public InteractionAiTaskResultCallbackController interactionAiTaskResultCallbackController(InteractionAiTaskResultReceiveService receiveService,
                                                                                               ObjectProvider<InteractionEventLogger> eventLoggerProvider) {
        return new InteractionAiTaskResultCallbackController(receiveService, eventLoggerProvider.getIfAvailable());
    }
}
