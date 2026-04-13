package com.jy.eletender.interaction.starter;

import com.jy.eletender.common.interaction.dto.BidRecordSchemeQueryRequest;
import com.jy.eletender.common.interaction.dto.BidRecordSchemeResponse;
import com.jy.eletender.common.interaction.dto.IdentityContext;
import com.jy.eletender.common.interaction.dto.IdentityQueryResponse;
import com.jy.eletender.common.interaction.dto.ProjectBasicInfoQueryRequest;
import com.jy.eletender.common.interaction.dto.ProjectBasicInfoResponse;
import com.jy.eletender.common.interaction.dto.CaKeysInfoQueryRequest;
import com.jy.eletender.common.interaction.dto.CaKeysInfoResponse;
import com.jy.eletender.common.interaction.dto.TenderPackageCallbackRequest;
import com.jy.eletender.common.interaction.dto.TenderPdfCallbackRequest;
import com.jy.eletender.common.interaction.enums.InteractionEvalMethod;
import com.jy.eletender.common.interaction.enums.InteractionProjectType;
import com.jy.eletender.common.interaction.spi.InteractionBidRecordSchemeService;
import com.jy.eletender.common.interaction.spi.InteractionCaKeysInfoService;
import com.jy.eletender.common.interaction.spi.InteractionIdentityService;
import com.jy.eletender.common.interaction.spi.InteractionProjectInfoService;
import com.jy.eletender.common.interaction.spi.InteractionTenderPackageReceiveService;
import com.jy.eletender.common.interaction.spi.InteractionTenderPdfReceiveService;
import com.jy.eletender.interaction.autoconfigure.EleTenderInteractionAutoConfiguration;
import com.jy.eleaitender.interaction.core.client.EleTenderInteractionClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class StarterSmokeTest {

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
            assertThat(context).hasBean("interactionIdentityController");
            assertThat(context).hasBean("interactionCaKeysInfoController");
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
                    ProjectBasicInfoResponse response = new ProjectBasicInfoResponse();
                    response.setProjectType(InteractionProjectType.PUBLIC);
                    response.setEvalMethod(InteractionEvalMethod.COMPREHENSIVE_SCORE);
                    return response;
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
    }
}
