package com.jy.eletender.tenderdocument.support;

import com.jy.eletender.tenderdocument.mapper.TenderDocumentFileMapper;
import com.jy.eletender.tenderdocument.support.interaction.BusinessSystemRemoteClient;
import com.jy.eletender.tenderdocument.support.interaction.FileTransferClient;
import com.jy.eletender.tenderdocument.support.interaction.InteractionTenderDocumentCallbackGateway;
import com.jy.eletender.tenderdocument.support.interaction.InteractionTenderDocumentGenerationGateway;
import com.jy.eletender.tenderdocument.support.interaction.InteractionTenderDocumentSyncGateway;
import com.jy.eletender.tenderdocument.support.interaction.TenderDocumentInteractionProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TenderDocumentCallbackGatewayConditionalTest {

    private final ApplicationContextRunner callbackContextRunner = new ApplicationContextRunner()
            .withBean(BusinessSystemRemoteClient.class, () -> mock(BusinessSystemRemoteClient.class))
            .withUserConfiguration(
                    DefaultTenderDocumentCallbackGateway.class,
                    InteractionTenderDocumentCallbackGateway.class
            );

    private final ApplicationContextRunner syncContextRunner = new ApplicationContextRunner()
            .withBean(BusinessSystemRemoteClient.class, () -> mock(BusinessSystemRemoteClient.class))
            .withUserConfiguration(
                    DefaultTenderDocumentSyncGateway.class,
                    InteractionTenderDocumentSyncGateway.class
            );

    private final ApplicationContextRunner generationContextRunner = new ApplicationContextRunner()
            .withBean(TenderDocumentFileMapper.class, () -> mock(TenderDocumentFileMapper.class))
            .withBean(FileTransferClient.class, () -> mock(FileTransferClient.class))
            .withBean(TenderDocumentInteractionProperties.class, TenderDocumentInteractionProperties::new)
            .withUserConfiguration(
                    DefaultTenderDocumentGenerationGateway.class,
                    InteractionTenderDocumentGenerationGateway.class
            );

    @Test
    void shouldOnlyLoadInteractionCallbackGatewayWhenAnotherImplementationExists() {
        callbackContextRunner.run(context -> {
            assertThat(context).hasSingleBean(TenderDocumentCallbackGateway.class);
            assertThat(context.getBean(TenderDocumentCallbackGateway.class))
                    .isInstanceOf(InteractionTenderDocumentCallbackGateway.class);
            assertThat(context).doesNotHaveBean(DefaultTenderDocumentCallbackGateway.class);
        });
    }

    @Test
    void shouldOnlyLoadInteractionSyncGatewayWhenAnotherImplementationExists() {
        syncContextRunner.run(context -> {
            assertThat(context).hasSingleBean(TenderDocumentSyncGateway.class);
            assertThat(context.getBean(TenderDocumentSyncGateway.class))
                    .isInstanceOf(InteractionTenderDocumentSyncGateway.class);
            assertThat(context).doesNotHaveBean(DefaultTenderDocumentSyncGateway.class);
        });
    }

    @Test
    void shouldOnlyLoadInteractionGenerationGatewayWhenAnotherImplementationExists() {
        generationContextRunner.run(context -> {
            assertThat(context).hasSingleBean(TenderDocumentGenerationGateway.class);
            assertThat(context.getBean(TenderDocumentGenerationGateway.class))
                    .isInstanceOf(InteractionTenderDocumentGenerationGateway.class);
            assertThat(context).doesNotHaveBean(DefaultTenderDocumentGenerationGateway.class);
        });
    }
}
