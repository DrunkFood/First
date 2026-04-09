package com.jy.eletender.tenderdocument.support;

import com.jy.eletender.common.enums.TenderDocumentScopeType;
import com.jy.eletender.tenderdocument.entity.TenderDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DefaultTenderDocumentGenerationGatewayTest {

    @InjectMocks
    private DefaultTenderDocumentGenerationGateway gateway;

    @Test
    void shouldGeneratePackageNameUsingProjectCodeAndTimestamp() {
        TenderDocumentGeneratedFile result = gateway.saveFinalPackage(
                buildDocument(),
                TenderDocumentScopeType.PROJECT,
                null,
                "encrypted".getBytes(),
                ".HzctZbs"
        );

        assertThat(result.getFileName()).matches(Pattern.compile("\\[PC-100\\]采购文件电子数据包\\d{14}\\.HzctZbs"));
    }

    @Test
    void shouldGeneratePackageNameUsingTenderIdWhenTenderScoped() {
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
    void shouldGeneratePackageNameUsingCustomSuffix() {
        TenderDocumentGeneratedFile result = gateway.saveFinalPackage(
                buildDocument(),
                TenderDocumentScopeType.PROJECT,
                null,
                "encrypted".getBytes(),
                ".CustomZbs"
        );
        assertThat(result.getFileName()).matches(Pattern.compile("\\[PC-100\\]采购文件电子数据包\\d{14}\\.CustomZbs"));
    }

    @Test
    void shouldGenerateCompileInfoPdfNameUsingProjectCodeAndTimestamp() {
        TenderDocumentGeneratedFile result = gateway.saveCompileInfoPdf(buildDocument(), "pdf".getBytes());

        assertThat(result.getFileName()).matches(Pattern.compile("\\[PC-100\\]采购文件编制信息\\d{14}\\.pdf"));
    }

    @Test
    void shouldGenerateCompileInfoPdfNameUsingTenderIdWhenTenderScoped() {
        TenderDocument tenderScopedDocument = buildDocument();
        tenderScopedDocument.setCompileScope(TenderDocumentScopeType.TENDER.name());
        tenderScopedDocument.setTenderId("T-01");

        TenderDocumentGeneratedFile result = gateway.saveCompileInfoPdf(tenderScopedDocument, "pdf".getBytes());

        assertThat(result.getFileName()).matches(Pattern.compile("\\[T-01\\]采购文件编制信息\\d{14}\\.pdf"));
    }

    private TenderDocument buildDocument() {
        TenderDocument document = new TenderDocument();
        document.setId(1L);
        document.setProjectId("project-1");
        document.setProjectCode("PC-100");
        document.setCompileScope(TenderDocumentScopeType.PROJECT.name());
        return document;
    }
}
