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

    @Bean(name = "interactionAiRequestSigner")
    @ConditionalOnMissingBean
    public InteractionRequestSigner interactionAiRequestSigner(EleAiTenderInteractionProperties properties) {
        return new InteractionRequestSigner(properties);
    }

    @Bean(name = "interactionAiRestTemplate")
    @ConditionalOnMissingBean
    public RestTemplate interactionRestTemplate(EleAiTenderInteractionProperties properties,
                                                ObjectProvider<InteractionEventLogger> eventLoggerProvider) {
        return InteractionRestTemplateFactory.create(properties, new OutboundLogInterceptor(eventLoggerProvider.getIfAvailable()));
    }

    @Bean(name = "aiTaskClient")
    @ConditionalOnMissingBean
    public AiTaskClient aiTaskClient(@Qualifier("interactionAiRestTemplate") RestTemplate interactionRestTemplate,
                                     EleAiTenderInteractionProperties properties) {
        return new AiTaskClient(interactionRestTemplate, properties);
    }

    @Bean(name = "aiExternalAuthClient")
    @ConditionalOnMissingBean
    public ExternalAuthClient externalAuthClient(@Qualifier("interactionAiRestTemplate") RestTemplate interactionRestTemplate,
                                                 @Qualifier("interactionAiRequestSigner") InteractionRequestSigner interactionRequestSigner,
                                                 EleAiTenderInteractionProperties properties) {
        return new ExternalAuthClient(interactionRestTemplate, properties, interactionRequestSigner);
    }

    @Bean(name = "aiExternalUserInfoClient")
    @ConditionalOnMissingBean
    public ExternalUserInfoClient externalUserInfoClient(@Qualifier("interactionAiRestTemplate") RestTemplate interactionRestTemplate,
                                                         @Qualifier("interactionAiRequestSigner") InteractionRequestSigner interactionRequestSigner,
                                                         EleAiTenderInteractionProperties properties) {
        return new ExternalUserInfoClient(interactionRestTemplate, properties, interactionRequestSigner);
    }

    @Bean(name = "aiFileClient")
    @ConditionalOnMissingBean
    public FileClient fileClient(@Qualifier("interactionAiRestTemplate") RestTemplate interactionRestTemplate,
                                 EleAiTenderInteractionProperties properties) {
        return new FileClient(interactionRestTemplate, properties);
    }

    @Bean(name = "interactionAiSignatureInterceptor")
    @ConditionalOnMissingBean
    public InteractionAiSignatureInterceptor interactionAiSignatureInterceptor(EleAiTenderInteractionProperties properties) {
        return new InteractionAiSignatureInterceptor(properties);
    }

    @Bean(name = "interactionAiWebMvcConfigurer")
    @ConditionalOnMissingBean
    public InteractionAiWebMvcConfigurer interactionAiWebMvcConfigurer(@Qualifier("interactionAiSignatureInterceptor") InteractionAiSignatureInterceptor signatureInterceptor) {
        return new InteractionAiWebMvcConfigurer(signatureInterceptor);
    }

    @Bean(name = "interactionAiGlobalExceptionHandler")
    @ConditionalOnMissingBean
    public InteractionAiGlobalExceptionHandler interactionAiGlobalExceptionHandler() {
        return new InteractionAiGlobalExceptionHandler();
    }

    @Bean(name = "interactionAiEventLogger")
    @ConditionalOnMissingBean
    public InteractionEventLogger interactionAiEventLogger() {
        return new DefaultInteractionAiEventLogger();
    }

    @Bean(name = "interactionAiTaskResultCallbackController")
    @ConditionalOnBean(InteractionAiTaskResultReceiveService.class)
    @ConditionalOnMissingBean
    public InteractionAiTaskResultCallbackController interactionAiTaskResultCallbackController(InteractionAiTaskResultReceiveService receiveService,
                                                                                               ObjectProvider<InteractionEventLogger> eventLoggerProvider) {
        return new InteractionAiTaskResultCallbackController(receiveService, eventLoggerProvider.getIfAvailable());
    }

}
