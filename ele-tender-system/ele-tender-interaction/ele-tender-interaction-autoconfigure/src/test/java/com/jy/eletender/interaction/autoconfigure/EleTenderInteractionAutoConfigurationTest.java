package com.jy.eletender.interaction.autoconfigure;

import com.jy.eletender.common.interaction.dto.BidRecordSchemeQueryRequest;
import com.jy.eletender.common.interaction.dto.BidRecordSchemeResponse;
import com.jy.eletender.common.interaction.dto.BidDecryptResultCallbackRequest;
import com.jy.eletender.common.interaction.dto.BidDocumentResultCallbackRequest;
import com.jy.eletender.common.interaction.dto.IdentityContext;
import com.jy.eletender.common.interaction.dto.IdentityQueryResponse;
import com.jy.eletender.common.interaction.dto.ProjectBasicInfoQueryRequest;
import com.jy.eletender.common.interaction.dto.ProjectBasicInfoResponse;
import com.jy.eletender.common.interaction.dto.CaKeysInfoQueryRequest;
import com.jy.eletender.common.interaction.dto.CaKeysInfoResponse;
import com.jy.eletender.common.interaction.dto.TenderPackageCallbackRequest;
import com.jy.eletender.common.interaction.dto.TenderPdfCallbackRequest;
import com.jy.eletender.common.interaction.spi.InteractionBidRecordSchemeService;
import com.jy.eletender.common.interaction.spi.InteractionBidDecryptResultReceiveService;
import com.jy.eletender.common.interaction.spi.InteractionBidDocumentResultReceiveService;
import com.jy.eletender.common.interaction.spi.InteractionCaKeysInfoService;
import com.jy.eletender.common.interaction.spi.InteractionEventLogger;
import com.jy.eletender.common.interaction.spi.InteractionIdentityService;
import com.jy.eletender.common.interaction.spi.InteractionProjectInfoService;
import com.jy.eletender.common.interaction.spi.InteractionTenderPackageReceiveService;
import com.jy.eletender.common.interaction.spi.InteractionTenderPdfReceiveService;
import com.jy.eletender.interaction.core.client.EleTenderInteractionClient;
import com.jy.eletender.interaction.core.client.ExternalAuthClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class EleTenderInteractionAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EleTenderInteractionAutoConfiguration.class))
            .withUserConfiguration(TestSpiConfiguration.class)
            .withPropertyValues(
                    "ele-tender.interaction.api-base-url=http://localhost:8080",
                    "ele-tender.interaction.page-base-url=http://localhost:8080",
                    "ele-tender.interaction.app-key=demo-key",
                    "ele-tender.interaction.app-secret=demo-secret");

    @Test
    void shouldLoadStarterBeansWhenSpiImplementationsExist() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(EleTenderInteractionClient.class);
            assertThat(context).hasSingleBean(InteractionEventLogger.class);
            assertThat(context).hasBean("interactionIdentityController");
            assertThat(context).hasBean("interactionCaKeysInfoController");
            assertThat(context).hasBean("interactionBidDocumentResultCallbackController");
            assertThat(context).hasBean("interactionBidDecryptResultCallbackController");
        });
    }

    @Test
    void shouldCreateDedicatedInteractionRestTemplateWhenHostDefinesGlobalRestTemplate() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(EleTenderInteractionAutoConfiguration.class))
                .withUserConfiguration(TestSpiConfiguration.class, HostRestTemplateConfiguration.class)
                .withPropertyValues(
                        "ele-tender.interaction.api-base-url=http://localhost:8080",
                        "ele-tender.interaction.page-base-url=http://localhost:8080",
                        "ele-tender.interaction.app-key=demo-key",
                        "ele-tender.interaction.app-secret=demo-secret")
                .run(context -> {
                    RestTemplate hostRestTemplate = context.getBean("hostRestTemplate", RestTemplate.class);
                    ExternalAuthClient externalAuthClient = context.getBean(ExternalAuthClient.class);
                    Object injectedRestTemplate = ReflectionTestUtils.getField(externalAuthClient, "restTemplate");

                    assertThat(context).hasBean("interactionRestTemplate");
                    assertThat(injectedRestTemplate).isInstanceOf(RestTemplate.class);
                    assertThat(injectedRestTemplate).isNotSameAs(hostRestTemplate);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class TestSpiConfiguration {

        @Bean
        InteractionIdentityService interactionIdentityService() {
            return new InteractionIdentityService() {
                @Override
                public IdentityQueryResponse queryCurrentIdentity(IdentityContext context) {
                    return new IdentityQueryResponse();
                }
            };
        }

        @Bean
        InteractionProjectInfoService interactionProjectInfoService() {
            return new InteractionProjectInfoService() {
                @Override
                public ProjectBasicInfoResponse queryProjectBasicInfo(ProjectBasicInfoQueryRequest request) {
                    return new ProjectBasicInfoResponse();
                }
            };
        }

        @Bean
        InteractionBidRecordSchemeService interactionBidRecordSchemeService() {
            return new InteractionBidRecordSchemeService() {
                @Override
                public BidRecordSchemeResponse queryBidRecordScheme(BidRecordSchemeQueryRequest request) {
                    return new BidRecordSchemeResponse();
                }
            };
        }

        @Bean
        InteractionCaKeysInfoService interactionCaKeysService() {
            return new InteractionCaKeysInfoService() {
                @Override
                public CaKeysInfoResponse queryCaKeysInfo(CaKeysInfoQueryRequest request) {
                    return new CaKeysInfoResponse();
                }
            };
        }

        @Bean
        InteractionTenderPdfReceiveService interactionTenderPdfReceiveService() {
            return new InteractionTenderPdfReceiveService() {
                @Override
                public void receive(TenderPdfCallbackRequest request) {
                }
            };
        }

        @Bean
        InteractionTenderPackageReceiveService interactionTenderPackageReceiveService() {
            return new InteractionTenderPackageReceiveService() {
                @Override
                public void receive(TenderPackageCallbackRequest request) {
                }
            };
        }

        @Bean
        InteractionBidDocumentResultReceiveService interactionBidDocumentResultReceiveService() {
            return new InteractionBidDocumentResultReceiveService() {
                @Override
                public void onBidDocumentResult(BidDocumentResultCallbackRequest request) {
                }
            };
        }

        @Bean
        InteractionBidDecryptResultReceiveService interactionBidDecryptResultReceiveService() {
            return new InteractionBidDecryptResultReceiveService() {
                @Override
                public void onDecryptResult(BidDecryptResultCallbackRequest request) {
                }
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class HostRestTemplateConfiguration {

        @Bean
        RestTemplate hostRestTemplate() {
            return new RestTemplate();
        }
    }
}
