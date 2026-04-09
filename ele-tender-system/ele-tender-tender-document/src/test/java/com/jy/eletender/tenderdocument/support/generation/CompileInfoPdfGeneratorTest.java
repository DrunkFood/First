package com.jy.eletender.tenderdocument.support.generation;

import com.jy.eletender.tenderdocument.entity.TenderDocument;
import com.jy.eletender.tenderdocument.support.TenderDocumentGeneratedFile;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;
import com.jy.eletender.tenderdocument.support.interaction.TenderDocumentInteractionProperties;
import com.lowagie.text.PageSize;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CompileInfoPdfGeneratorTest {

    @Test
    void shouldGenerateCompileInfoPdfFollowingTemplateFields() throws IOException {
        TenderDocumentInteractionProperties properties = new TenderDocumentInteractionProperties();
        CompileInfoPdfGenerator generator = new CompileInfoPdfGenerator(properties);
        Map<String, Object> projectInfo = Map.of(
                "purchaserName", "浙江省政府采购中心",
                "purchaseMethod", "公开招标"
        );
        CompileInfoPdfGenerator.CompileInfoPdfModel model =
                generator.buildModel(buildDocument(), projectInfo, buildFinalPackageFile(), buildUserContext(), new Date(1765528083000L));

        byte[] pdfBytes = generator.generate(buildDocument(), projectInfo, buildFinalPackageFile(), buildUserContext(), new Date(1765528083000L));

        assertThat(pdfBytes).isNotEmpty();
        assertThat(new String(pdfBytes, java.nio.charset.StandardCharsets.ISO_8859_1)).startsWith("%PDF");
        assertThat(model.title()).isEqualTo("采购文件编制信息");
        assertThat(model.baseInfoLines()).containsExactly(
                new CompileInfoPdfGenerator.LabelValue("项目名称：", "长河单元小学、幼儿园新建工程及周边地下空间开发工程项目"),
                new CompileInfoPdfGenerator.LabelValue("项目编号：", "A3301080120530207001211"),
                new CompileInfoPdfGenerator.LabelValue("采购单位名称：", "浙江省政府采购中心"),
                new CompileInfoPdfGenerator.LabelValue("采购方式：", "公开招标"),
                new CompileInfoPdfGenerator.LabelValue("编制单位名称：", "浙江省成套工程有限公司"),
                new CompileInfoPdfGenerator.LabelValue("编制用户名称：", "张亮"),
                new CompileInfoPdfGenerator.LabelValue("编制完成时间：", "2025-12-12 16:28:03")
        );
        assertThat(model.fileInfoRows()).containsExactly(
                new CompileInfoPdfGenerator.LabelValue("编制文件名称", "[A3301080120530207001211]采购文件电子数据包20260318150000.HzctZbs"),
                new CompileInfoPdfGenerator.LabelValue("文件大小", "26KB"),
                new CompileInfoPdfGenerator.LabelValue("文件版本号", "V1.0"),
                new CompileInfoPdfGenerator.LabelValue("文件sha256码", "sha256-abc")
        );
    }

    @Test
    void shouldUseConfiguredWatermarkText() {
        TenderDocumentInteractionProperties properties = new TenderDocumentInteractionProperties();
        properties.setCompileInfoWatermarkText("城投集团采购平台");
        CompileInfoPdfGenerator generator = new CompileInfoPdfGenerator(properties);
        CompileInfoPdfGenerator.CompileInfoPdfModel model =
                generator.buildModel(buildDocument(), Map.of(), buildFinalPackageFile(), buildUserContext(), new Date(1765528083000L));

        assertThat(model.watermarkText()).isEqualTo("城投集团采购平台");
    }

    @Test
    void shouldTileWatermarkAcrossWholePage() {
        CompileInfoPdfGenerator generator = new CompileInfoPdfGenerator(new TenderDocumentInteractionProperties());

        assertThat(generator.buildWatermarkPlacements(PageSize.A4)).hasSizeGreaterThanOrEqualTo(8);
    }

    @Test
    void shouldRenderSolidWatermark() {
        CompileInfoPdfGenerator generator = new CompileInfoPdfGenerator(new TenderDocumentInteractionProperties());

        assertThat(generator.watermarkOpacity()).isEqualTo(1.0F);
    }

    @Test
    void shouldUseRefinedTemplateMetrics() {
        CompileInfoPdfGenerator generator = new CompileInfoPdfGenerator(new TenderDocumentInteractionProperties());

        assertThat(generator.watermarkFontSize()).isEqualTo(40F);
        assertThat(generator.titleSpacingAfter()).isEqualTo(12F);
        assertThat(generator.tableCellVerticalPadding()).isEqualTo(8F);
    }

    private TenderDocument buildDocument() {
        TenderDocument tenderDocument = new TenderDocument();
        tenderDocument.setProjectId("P-100");
        tenderDocument.setProjectCode("A3301080120530207001211");
        tenderDocument.setProjectName("长河单元小学、幼儿园新建工程及周边地下空间开发工程项目");
        tenderDocument.setPurchaseMethod("公开招标");
        tenderDocument.setVersionNo(1);
        return tenderDocument;
    }

    private TenderDocumentGeneratedFile buildFinalPackageFile() {
        TenderDocumentGeneratedFile file = new TenderDocumentGeneratedFile();
        file.setFileName("[A3301080120530207001211]采购文件电子数据包20260318150000.HzctZbs");
        file.setFileSize(26246L);
        file.setFileSha256("sha256-abc");
        return file;
    }

    private TenderDocumentUserContext buildUserContext() {
        TenderDocumentUserContext context = new TenderDocumentUserContext();
        context.setEnterpriseName("浙江省成套工程有限公司");
        context.setUserName("张亮");
        return context;
    }
}
