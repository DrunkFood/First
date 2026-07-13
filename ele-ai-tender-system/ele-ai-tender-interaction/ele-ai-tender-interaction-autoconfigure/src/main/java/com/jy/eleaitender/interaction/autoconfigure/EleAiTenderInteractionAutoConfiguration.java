package com.jy.eleaitender.interaction.autoconfigure;

import com.jy.eleaitender.common.interaction.spi.InteractionAiTaskResultReceiveService;
import com.jy.eleaitender.common.interaction.spi.InteractionEventLogger;
import com.jy.eleaitender.interaction.autoconfigure.controller.InteractionAiTaskResultCallbackController;
import com.jy.eleaitender.interaction.autoconfigure.handler.InteractionAiGlobalExceptionHandler;
import com.jy.eleaitender.interaction.autoconfigure.logging.DefaultInteractionAiEventLogger;
import com.jy.eleaitender.interaction.autoconfigure.web.InteractionAiSignatureInterceptor;
import com.jy.eleaitender.interaction.autoconfigure.web.InteractionAiWebMvcConfigurer;
import com.jy.eleaitender.interaction.core.client.AiExternalAuthClient;
import com.jy.eleaitender.interaction.core.client.AiExternalUserInfoClient;
import com.jy.eleaitender.interaction.core.client.AiFileClient;
import com.jy.eleaitender.interaction.core.client.AiTaskClient;
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

    @Bean(name = "interactionAiRequestSigner")
    @ConditionalOnMissingBean(name = "interactionAiRequestSigner")
    public InteractionRequestSigner interactionAiRequestSigner(EleAiTenderInteractionProperties properties) {
        return new InteractionRequestSigner(properties);
    }

    @Bean(name = "interactionAiRestTemplate")
    @ConditionalOnMissingBean(name = "interactionAiRestTemplate")
    public RestTemplate interactionRestTemplate(EleAiTenderInteractionProperties properties,
                                                ObjectProvider<InteractionEventLogger> eventLoggerProvider) {
        return InteractionRestTemplateFactory.create(properties, new OutboundLogInterceptor(eventLoggerProvider.getIfAvailable()));
    }

    @Bean(name = "aiTaskClient")
    @ConditionalOnMissingBean(name = "aiTaskClient")
    public AiTaskClient aiTaskClient(@Qualifier("interactionAiRestTemplate") RestTemplate interactionRestTemplate,
                                     EleAiTenderInteractionProperties properties) {
        return new AiTaskClient(interactionRestTemplate, properties);
    }

    @Bean(name = "aiExternalAuthClient")
    @ConditionalOnMissingBean(name = "aiExternalAuthClient")
    public AiExternalAuthClient aiExternalAuthClient(@Qualifier("interactionAiRestTemplate") RestTemplate interactionRestTemplate,
                                                     @Qualifier("interactionAiRequestSigner") InteractionRequestSigner interactionRequestSigner,
                                                     EleAiTenderInteractionProperties properties) {
        return new AiExternalAuthClient(interactionRestTemplate, properties, interactionRequestSigner);
    }

    @Bean(name = "aiExternalUserInfoClient")
    @ConditionalOnMissingBean(name = "aiExternalUserInfoClient")
    public AiExternalUserInfoClient aiExternalUserInfoClient(@Qualifier("interactionAiRestTemplate") RestTemplate interactionRestTemplate,
                                                             @Qualifier("interactionAiRequestSigner") InteractionRequestSigner interactionRequestSigner,
                                                             EleAiTenderInteractionProperties properties) {
        return new AiExternalUserInfoClient(interactionRestTemplate, properties, interactionRequestSigner);
    }

    @Bean(name = "aiFileClient")
    @ConditionalOnMissingBean(name = "aiFileClient")
    public AiFileClient aiFileClient(@Qualifier("interactionAiRestTemplate") RestTemplate interactionRestTemplate,
                                     EleAiTenderInteractionProperties properties) {
        return new AiFileClient(interactionRestTemplate, properties);
    }

    @Bean(name = "interactionAiSignatureInterceptor")
    @ConditionalOnMissingBean(name = "interactionAiSignatureInterceptor")
    public InteractionAiSignatureInterceptor interactionAiSignatureInterceptor(EleAiTenderInteractionProperties properties) {
        return new InteractionAiSignatureInterceptor(properties);
    }

    @Bean(name = "interactionAiWebMvcConfigurer")
    @ConditionalOnMissingBean(name = "interactionAiWebMvcConfigurer")
    public InteractionAiWebMvcConfigurer interactionAiWebMvcConfigurer(@Qualifier("interactionAiSignatureInterceptor") InteractionAiSignatureInterceptor signatureInterceptor) {
        return new InteractionAiWebMvcConfigurer(signatureInterceptor);
    }

    @Bean(name = "interactionAiGlobalExceptionHandler")
    @ConditionalOnMissingBean(name = "interactionAiGlobalExceptionHandler")
    public InteractionAiGlobalExceptionHandler interactionAiGlobalExceptionHandler() {
        return new InteractionAiGlobalExceptionHandler();
    }

    @Bean(name = "interactionAiEventLogger")
    @ConditionalOnMissingBean(name = "interactionAiEventLogger")
    public InteractionEventLogger interactionAiEventLogger() {
        return new DefaultInteractionAiEventLogger();
    }

    @Bean(name = "interactionAiTaskResultCallbackController")
    @ConditionalOnBean(InteractionAiTaskResultReceiveService.class)
    @ConditionalOnMissingBean(name = "interactionAiTaskResultCallbackController")
    public InteractionAiTaskResultCallbackController interactionAiTaskResultCallbackController(InteractionAiTaskResultReceiveService receiveService,
                                                                                               ObjectProvider<InteractionEventLogger> eventLoggerProvider) {
        return new InteractionAiTaskResultCallbackController(receiveService, eventLoggerProvider.getIfAvailable());
    }

}
