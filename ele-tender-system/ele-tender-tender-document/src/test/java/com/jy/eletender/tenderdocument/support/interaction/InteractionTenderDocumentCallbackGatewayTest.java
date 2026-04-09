package com.jy.eletender.tenderdocument.support.interaction;

import com.jy.eletender.tenderdocument.entity.TenderDocument;
import com.jy.eletender.tenderdocument.entity.TenderDocumentFile;
import com.jy.eletender.tenderdocument.support.TenderDocumentCallbackGatewayResult;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class InteractionTenderDocumentCallbackGatewayTest {

    @Mock
    private BusinessSystemRemoteClient remoteClient;

    @InjectMocks
    private InteractionTenderDocumentCallbackGateway gateway;

    @Test
    void shouldReturnSuccessWhenTenderPdfCallbackAccepted() {
        doNothing().when(remoteClient).callbackTenderPdf(any(), any(), any());

        TenderDocumentCallbackGatewayResult result = gateway.callbackSignedFile(buildDocument(), buildFile(), "T-01", buildContext());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResponseCode()).isEqualTo("200");
    }

    private TenderDocument buildDocument() {
        TenderDocument tenderDocument = new TenderDocument();
        tenderDocument.setBizType(1);
        tenderDocument.setBizId("BIZ-1");
        tenderDocument.setProjectId("P-100");
        return tenderDocument;
    }

    private TenderDocumentFile buildFile() {
        TenderDocumentFile file = new TenderDocumentFile();
        file.setFileId(123L);
        file.setFileName("signed.pdf");
        return file;
    }

    private TenderDocumentUserContext buildContext() {
        TenderDocumentUserContext context = new TenderDocumentUserContext();
        context.setAppKey("demo-app");
        return context;
    }
}
