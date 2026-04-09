package com.jy.eletender.tenderdocument.support.interaction;

import com.jy.eletender.common.enums.TenderDocumentScopeType;
import com.jy.eletender.tenderdocument.entity.TenderDocument;
import com.jy.eletender.tenderdocument.enums.TenderDocumentFileType;
import com.jy.eletender.tenderdocument.support.TenderDocumentGeneratedFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InteractionTenderDocumentGenerationGatewayTest {

    @Mock
    private FileTransferClient fileTransferClient;

    @Mock
    private TenderDocumentInteractionProperties properties;

    @InjectMocks
    private InteractionTenderDocumentGenerationGateway gateway;

    @Test
    void shouldSaveFinalPackageByUploadingEncryptedArtifact() {
        when(properties.getUploadBizType()).thenReturn("tender-document");
        when(fileTransferClient.upload(any(), any(), any(), any()))
                .thenAnswer(invocation -> new UploadedFileInfo(200L, invocation.getArgument(0), 128L, "sha-200"));

        TenderDocumentGeneratedFile result = gateway.saveFinalPackage(
                buildDocument(),
                TenderDocumentScopeType.PROJECT,
                null,
                "encrypted".getBytes(),
                ".HzctZbs"
        );

        assertThat(result.getFileId()).isEqualTo(200L);
        assertThat(result.getFileSha256()).isEqualTo("sha-200");
        assertThat(result.getFileRole()).isEqualTo(TenderDocumentFileType.FINAL_PACKAGE_FILE.name());
        assertThat(result.getFileName()).matches(Pattern.compile("\\[PC-100\\]采购文件电子数据包\\d{14}\\.HzctZbs"));
    }

    @Test
    void shouldSaveFinalPackageUsingTenderIdWhenTenderScoped() {
        when(properties.getUploadBizType()).thenReturn("tender-document");
        when(fileTransferClient.upload(any(), any(), any(), any()))
                .thenAnswer(invocation -> new UploadedFileInfo(200L, invocation.getArgument(0), 128L, "sha-200"));

        TenderDocument tenderScopedDocument = buildDocument();
        tenderScopedDocument.setCompileScope(TenderDocumentScopeType.TENDER.name());
        TenderDocumentGeneratedFile result = gateway.saveFinalPackage(
                tenderScopedDocument,
                TenderDocumentScopeType.TENDER,
                "T-01",
                "encrypted".getBytes(),
                ".HzctZbs"
        );

        assertThat(result.getFileName()).matches(Pattern.compile("\\[T-01\\]采购文件电子数据包\\d{14}\\.HzctZbs"));
    }

    @Test
    void shouldSaveCompileInfoPdfUsingProjectCodeAndTimestamp() {
        when(properties.getUploadBizType()).thenReturn("tender-document");
        when(fileTransferClient.upload(any(), any(), any(), any()))
                .thenAnswer(invocation -> new UploadedFileInfo(201L, invocation.getArgument(0), 128L, "sha-201"));

        TenderDocumentGeneratedFile result = gateway.saveCompileInfoPdf(buildDocument(), "pdf".getBytes());

        assertThat(result.getFileId()).isEqualTo(201L);
        assertThat(result.getFileName()).matches(Pattern.compile("\\[PC-100\\]采购文件编制信息\\d{14}\\.pdf"));
    }

    @Test
    void shouldSaveCompileInfoPdfUsingTenderIdWhenTenderScoped() {
        when(properties.getUploadBizType()).thenReturn("tender-document");
        when(fileTransferClient.upload(any(), any(), any(), any()))
                .thenAnswer(invocation -> new UploadedFileInfo(201L, invocation.getArgument(0), 128L, "sha-201"));

        TenderDocument tenderScopedDocument = buildDocument();
        tenderScopedDocument.setCompileScope(TenderDocumentScopeType.TENDER.name());
        tenderScopedDocument.setTenderId("T-01");
        TenderDocumentGeneratedFile result = gateway.saveCompileInfoPdf(tenderScopedDocument, "pdf".getBytes());

        assertThat(result.getFileName()).matches(Pattern.compile("\\[T-01\\]采购文件编制信息\\d{14}\\.pdf"));
    }

    private TenderDocument buildDocument() {
        TenderDocument tenderDocument = new TenderDocument();
        tenderDocument.setId(100L);
        tenderDocument.setProjectId("P-100");
        tenderDocument.setProjectCode("PC-100");
        tenderDocument.setProjectName("示例项目");
        return tenderDocument;
    }
}
