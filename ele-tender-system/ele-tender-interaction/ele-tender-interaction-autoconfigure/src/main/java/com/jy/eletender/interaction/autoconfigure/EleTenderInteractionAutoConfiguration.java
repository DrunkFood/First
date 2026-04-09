package com.jy.eletender.interaction.autoconfigure;

import com.jy.eletender.common.interaction.spi.InteractionBidRecordSchemeService;
import com.jy.eletender.common.interaction.spi.InteractionBidDecryptResultReceiveService;
import com.jy.eletender.common.interaction.spi.InteractionBidDocumentResultReceiveService;
import com.jy.eletender.common.interaction.spi.InteractionCaKeysInfoService;
import com.jy.eletender.common.interaction.spi.InteractionEventLogger;
import com.jy.eletender.common.interaction.spi.InteractionIdentityService;
import com.jy.eletender.common.interaction.spi.InteractionProjectInfoService;
import com.jy.eletender.common.interaction.spi.InteractionTenderPackageReceiveService;
import com.jy.eletender.common.interaction.spi.InteractionTenderPdfReceiveService;
import com.jy.eletender.interaction.autoconfigure.logging.DefaultInteractionEventLogger;
import com.jy.eletender.interaction.autoconfigure.controller.InteractionBidRecordSchemeController;
import com.jy.eletender.interaction.autoconfigure.controller.InteractionBidDecryptResultCallbackController;
import com.jy.eletender.interaction.autoconfigure.controller.InteractionBidDocumentResultCallbackController;
import com.jy.eletender.interaction.autoconfigure.controller.InteractionCaKeysInfoController;
import com.jy.eletender.interaction.autoconfigure.controller.InteractionIdentityController;
import com.jy.eletender.interaction.autoconfigure.controller.InteractionProjectInfoController;
import com.jy.eletender.interaction.autoconfigure.controller.InteractionTenderPackageCallbackController;
import com.jy.eletender.interaction.autoconfigure.controller.InteractionTenderPdfCallbackController;
import com.jy.eletender.interaction.core.client.BidDecryptClient;
import com.jy.eletender.interaction.core.client.BidDocumentPushClient;
import com.jy.eletender.interaction.core.client.EnvelopeClient;
import com.jy.eletender.interaction.autoconfigure.handler.InteractionGlobalExceptionHandler;
import com.jy.eletender.interaction.autoconfigure.web.InteractionSignatureInterceptor;
import com.jy.eletender.interaction.autoconfigure.web.InteractionWebMvcConfigurer;
import com.jy.eletender.interaction.core.client.EleTenderInteractionClient;
import com.jy.eletender.interaction.core.client.ExternalAuthClient;
import com.jy.eletender.interaction.core.client.ExternalUserInfoClient;
import com.jy.eletender.interaction.core.client.FileClient;
import com.jy.eletender.interaction.core.client.TenderDocumentEntryUrlBuilder;
import com.jy.eletender.interaction.core.properties.EleTenderInteractionProperties;
import com.jy.eletender.interaction.core.support.InteractionRequestSigner;
import com.jy.eletender.interaction.core.support.InteractionRestTemplateFactory;
import com.jy.eletender.interaction.core.support.OutboundLogInterceptor;
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
@EnableConfigurationProperties({EleTenderInteractionProperties.class, InteractionControllerProperties.class})
@ConditionalOnProperty(prefix = "ele-tender.interaction", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EleTenderInteractionAutoConfiguration {

    static final String INTERACTION_REST_TEMPLATE_BEAN_NAME = "interactionRestTemplate";

    @Bean
    @ConditionalOnMissingBean
    public InteractionRequestSigner interactionRequestSigner(EleTenderInteractionProperties properties) {
        return new InteractionRequestSigner(properties);
    }

    @Bean(INTERACTION_REST_TEMPLATE_BEAN_NAME)
    @ConditionalOnMissingBean(name = INTERACTION_REST_TEMPLATE_BEAN_NAME)
    public RestTemplate interactionRestTemplate(EleTenderInteractionProperties properties,
                                                ObjectProvider<InteractionEventLogger> eventLoggerProvider) {
        return InteractionRestTemplateFactory.create(properties, new OutboundLogInterceptor(eventLoggerProvider.getIfAvailable()));
    }

    @Bean
    @ConditionalOnMissingBean
    public ExternalAuthClient externalAuthClient(@Qualifier(INTERACTION_REST_TEMPLATE_BEAN_NAME) RestTemplate interactionRestTemplate,
                                                 EleTenderInteractionProperties properties,
                                                 InteractionRequestSigner interactionRequestSigner) {
        return new ExternalAuthClient(interactionRestTemplate, properties, interactionRequestSigner);
    }

    @Bean
    @ConditionalOnMissingBean
    public ExternalUserInfoClient externalUserInfoClient(@Qualifier(INTERACTION_REST_TEMPLATE_BEAN_NAME) RestTemplate interactionRestTemplate,
                                                         EleTenderInteractionProperties properties,
                                                         InteractionRequestSigner interactionRequestSigner) {
        return new ExternalUserInfoClient(interactionRestTemplate, properties, interactionRequestSigner);
    }

    @Bean
    @ConditionalOnMissingBean
    public FileClient fileClient(@Qualifier(INTERACTION_REST_TEMPLATE_BEAN_NAME) RestTemplate interactionRestTemplate,
                                 EleTenderInteractionProperties properties,
                                 InteractionRequestSigner interactionRequestSigner) {
        return new FileClient(interactionRestTemplate, properties, interactionRequestSigner);
    }

    @Bean
    @ConditionalOnMissingBean
    public BidDocumentPushClient bidDocumentPushClient(@Qualifier(INTERACTION_REST_TEMPLATE_BEAN_NAME) RestTemplate interactionRestTemplate,
                                               EleTenderInteractionProperties properties,
                                               InteractionRequestSigner interactionRequestSigner) {
        return new BidDocumentPushClient(interactionRestTemplate, properties, interactionRequestSigner);
    }

    @Bean
    @ConditionalOnMissingBean
    public BidDecryptClient bidDecryptClient(@Qualifier(INTERACTION_REST_TEMPLATE_BEAN_NAME) RestTemplate interactionRestTemplate,
                                             EleTenderInteractionProperties properties,
                                             InteractionRequestSigner interactionRequestSigner) {
        return new BidDecryptClient(interactionRestTemplate, properties, interactionRequestSigner);
    }

    @Bean
    @ConditionalOnMissingBean
    public EnvelopeClient envelopeClient(@Qualifier(INTERACTION_REST_TEMPLATE_BEAN_NAME) RestTemplate interactionRestTemplate,
                                         EleTenderInteractionProperties properties,
                                         InteractionRequestSigner interactionRequestSigner) {
        return new EnvelopeClient(interactionRestTemplate, properties, interactionRequestSigner);
    }

    @Bean
    @ConditionalOnMissingBean
    public TenderDocumentEntryUrlBuilder tenderDocumentEntryUrlBuilder(EleTenderInteractionProperties properties) {
        return new TenderDocumentEntryUrlBuilder(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public EleTenderInteractionClient eleTenderInteractionClient(ExternalAuthClient externalAuthClient,
                                                                 ExternalUserInfoClient externalUserInfoClient,
                                                                 FileClient fileClient,
                                                                 BidDocumentPushClient bidDocumentPushClient,
                                                                 BidDecryptClient bidDecryptClient,
                                                                 EnvelopeClient envelopeClient,
                                                                 TenderDocumentEntryUrlBuilder tenderDocumentEntryUrlBuilder) {
        return new EleTenderInteractionClient(
                externalAuthClient,
                externalUserInfoClient,
                fileClient,
                bidDocumentPushClient,
                bidDecryptClient,
                envelopeClient,
                tenderDocumentEntryUrlBuilder);
    }

    @Bean
    @ConditionalOnMissingBean
    public InteractionSignatureInterceptor interactionSignatureInterceptor(EleTenderInteractionProperties properties) {
        return new InteractionSignatureInterceptor(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public InteractionWebMvcConfigurer interactionWebMvcConfigurer(InteractionSignatureInterceptor signatureInterceptor) {
        return new InteractionWebMvcConfigurer(signatureInterceptor);
    }

    @Bean
    @ConditionalOnMissingBean
    public InteractionGlobalExceptionHandler interactionGlobalExceptionHandler() {
        return new InteractionGlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public InteractionEventLogger interactionEventLogger() {
        return new DefaultInteractionEventLogger();
    }

    @Bean
    @ConditionalOnBean(InteractionIdentityService.class)
    @ConditionalOnMissingBean
    public InteractionIdentityController interactionIdentityController(EleTenderInteractionClient interactionClient,
                                                                       InteractionIdentityService identityService,
                                                                       ObjectProvider<InteractionEventLogger> eventLoggerProvider) {
        return new InteractionIdentityController(interactionClient, identityService, eventLoggerProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnBean(InteractionProjectInfoService.class)
    @ConditionalOnMissingBean
    public InteractionProjectInfoController interactionProjectInfoController(InteractionProjectInfoService projectInfoService,
                                                                             ObjectProvider<InteractionEventLogger> eventLoggerProvider) {
        return new InteractionProjectInfoController(projectInfoService, eventLoggerProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnBean(InteractionBidRecordSchemeService.class)
    @ConditionalOnMissingBean
    public InteractionBidRecordSchemeController interactionBidRecordSchemeController(InteractionBidRecordSchemeService bidRecordSchemeService,
                                                                                     ObjectProvider<InteractionEventLogger> eventLoggerProvider) {
        return new InteractionBidRecordSchemeController(bidRecordSchemeService, eventLoggerProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnBean(InteractionCaKeysInfoService.class)
    @ConditionalOnMissingBean
    public InteractionCaKeysInfoController interactionCaKeysInfoController(InteractionCaKeysInfoService caKeysService,
                                                                               ObjectProvider<InteractionEventLogger> eventLoggerProvider) {
        return new InteractionCaKeysInfoController(caKeysService, eventLoggerProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnBean(InteractionTenderPdfReceiveService.class)
    @ConditionalOnMissingBean
    public InteractionTenderPdfCallbackController interactionTenderPdfCallbackController(InteractionTenderPdfReceiveService tenderPdfReceiveService,
                                                                                         ObjectProvider<InteractionEventLogger> eventLoggerProvider) {
        return new InteractionTenderPdfCallbackController(tenderPdfReceiveService, eventLoggerProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnBean(InteractionTenderPackageReceiveService.class)
    @ConditionalOnMissingBean
    public InteractionTenderPackageCallbackController interactionTenderPackageCallbackController(InteractionTenderPackageReceiveService tenderPackageReceiveService,
                                                                                                 ObjectProvider<InteractionEventLogger> eventLoggerProvider) {
        return new InteractionTenderPackageCallbackController(tenderPackageReceiveService, eventLoggerProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnBean(InteractionBidDocumentResultReceiveService.class)
    @ConditionalOnMissingBean
    public InteractionBidDocumentResultCallbackController interactionBidDocumentResultCallbackController(InteractionBidDocumentResultReceiveService receiveService,
                                                                                                  ObjectProvider<InteractionEventLogger> eventLoggerProvider) {
        return new InteractionBidDocumentResultCallbackController(receiveService, eventLoggerProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnBean(InteractionBidDecryptResultReceiveService.class)
    @ConditionalOnMissingBean
    public InteractionBidDecryptResultCallbackController interactionBidDecryptResultCallbackController(InteractionBidDecryptResultReceiveService receiveService,
                                                                                                        ObjectProvider<InteractionEventLogger> eventLoggerProvider) {
        return new InteractionBidDecryptResultCallbackController(receiveService, eventLoggerProvider.getIfAvailable());
    }
}
