package com.jy.eletender.tenderdocument.support.generation;

import com.jy.eletender.tenderdocument.entity.TenderDocument;
import com.jy.eletender.tenderdocument.support.TenderDocumentGeneratedFile;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;
import com.jy.eletender.tenderdocument.support.interaction.TenderDocumentInteractionProperties;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

@Component
public class CompileInfoPdfGenerator {

    private static final String CJK_FONT_NAME = "STSong-Light";
    private static final String CJK_ENCODING = "UniGB-UCS2-H";
    private static final float PAGE_MARGIN = 72F;
    private static final float TABLE_TOTAL_WIDTH = 426F;
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final TenderDocumentInteractionProperties properties;

    public CompileInfoPdfGenerator(TenderDocumentInteractionProperties properties) {
        this.properties = properties;
    }

    public byte[] generate(TenderDocument tenderDocument,
                           Map<String, Object> projectInfo,
                           TenderDocumentGeneratedFile finalPackageFile,
                           TenderDocumentUserContext userContext,
                           Date completedAt) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, PAGE_MARGIN, PAGE_MARGIN, PAGE_MARGIN, PAGE_MARGIN);
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            BaseFont baseFont = BaseFont.createFont(CJK_FONT_NAME, CJK_ENCODING, BaseFont.NOT_EMBEDDED);
            CompileInfoPdfModel model = buildModel(tenderDocument, projectInfo, finalPackageFile, userContext, completedAt);
            document.open();

            addWatermark(writer, baseFont, model.watermarkText());
            addTitle(document, baseFont);
            addBaseInfo(document, baseFont, model);
            addFileTable(document, baseFont, model);

            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("生成采购文件编制信息PDF失败", ex);
        }
    }

    CompileInfoPdfModel buildModel(TenderDocument tenderDocument,
                                   Map<String, Object> projectInfo,
                                   TenderDocumentGeneratedFile finalPackageFile,
                                   TenderDocumentUserContext userContext,
                                   Date completedAt) {
        Map<String, Object> safeProjectInfo = projectInfo != null ? projectInfo : java.util.Collections.emptyMap();
        return new CompileInfoPdfModel(
                "采购文件编制信息",
                resolveWatermarkText(),
                List.of(
                        new LabelValue("项目名称：", resolveProjectName(tenderDocument)),
                        new LabelValue("项目编号：", resolveProjectCode(tenderDocument)),
                        new LabelValue("采购单位名称：", defaultValue(asString(safeProjectInfo.get("purchaserName")))),
                        new LabelValue("采购方式：", defaultValue(tenderDocument == null ? null : tenderDocument.getPurchaseMethod())),
                        new LabelValue("编制单位名称：", resolveEnterpriseName(userContext)),
                        new LabelValue("编制用户名称：", defaultValue(userContext == null ? null : userContext.getUserName())),
                        new LabelValue("编制完成时间：", DATE_TIME_FORMAT.format(completedAt == null ? new Date() : completedAt))
                ),
                List.of(
                        new LabelValue("编制文件名称", defaultValue(finalPackageFile == null ? null : finalPackageFile.getFileName())),
                        new LabelValue("文件大小", formatFileSize(finalPackageFile == null ? null : finalPackageFile.getFileSize())),
                        new LabelValue("文件版本号", formatVersionNo(tenderDocument == null ? null : tenderDocument.getVersionNo())),
                        new LabelValue("文件sha256码", defaultValue(finalPackageFile == null ? null : finalPackageFile.getFileSha256()))
                )
        );
    }

    private void addWatermark(PdfWriter writer, BaseFont baseFont, String watermarkText) throws DocumentException {
        if (!StringUtils.hasText(watermarkText)) {
            return;
        }
        PdfContentByte canvas = writer.getDirectContentUnder();
        Rectangle pageSize = writer.getPageSize();
        Font watermarkFont = new Font(baseFont, watermarkFontSize(), Font.BOLD);
        watermarkFont.setColor(215, 215, 215);
        Phrase phrase = new Phrase(watermarkText, watermarkFont);
        PdfGState state = new PdfGState();
        state.setFillOpacity(watermarkOpacity());
        canvas.saveState();
        canvas.setGState(state);
        for (WatermarkPlacement placement : buildWatermarkPlacements(pageSize)) {
            ColumnText.showTextAligned(canvas,
                    Element.ALIGN_CENTER,
                    phrase,
                    placement.x(),
                    placement.y(),
                    35);
        }
        canvas.restoreState();
    }

    List<WatermarkPlacement> buildWatermarkPlacements(Rectangle pageSize) {
        List<WatermarkPlacement> placements = new java.util.ArrayList<>();
        float xStep = 210F;
        float yStep = 250F;
        for (float y = 110F; y < pageSize.getHeight() + 80F; y += yStep) {
            float rowOffset = ((int) ((y - 110F) / yStep) % 2 == 0) ? 0F : 90F;
            for (float x = 70F + rowOffset; x < pageSize.getWidth() + 110F; x += xStep) {
                placements.add(new WatermarkPlacement(x, y));
            }
        }
        return placements;
    }

    float watermarkOpacity() {
        return 1.0F;
    }

    float watermarkFontSize() {
        return 40F;
    }

    private void addTitle(Document document, BaseFont baseFont) throws DocumentException {
        Font titleFont = new Font(baseFont, 20, Font.NORMAL);
        Paragraph title = new Paragraph("采购文件编制信息", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setLeading(0F, 1.5F);
        title.setSpacingAfter(titleSpacingAfter());
        document.add(title);
    }

    float titleSpacingAfter() {
        return 12F;
    }

    private void addBaseInfo(Document document,
                             BaseFont baseFont,
                             CompileInfoPdfModel model) throws DocumentException {
        Font labelFont = new Font(baseFont, 12, Font.BOLD);
        Font valueFont = new Font(baseFont, 12, Font.NORMAL);

        for (LabelValue line : model.baseInfoLines()) {
            document.add(buildLine(line.label(), line.value(), labelFont, valueFont));
        }

        Paragraph fileInfoTitle = new Paragraph();
        fileInfoTitle.setLeading(0F, 1.5F);
        fileInfoTitle.setSpacingBefore(8F);
        fileInfoTitle.setSpacingAfter(8F);
        fileInfoTitle.add(new Phrase("编制文件信息：", labelFont));
        document.add(fileInfoTitle);
    }

    private Paragraph buildLine(String label, String value, Font labelFont, Font valueFont) {
        Paragraph paragraph = new Paragraph();
        paragraph.setLeading(0F, 1.5F);
        paragraph.setSpacingAfter(1F);
        paragraph.add(new Phrase(label, labelFont));
        paragraph.add(new Phrase(value, valueFont));
        return paragraph;
    }

    private void addFileTable(Document document,
                              BaseFont baseFont,
                              CompileInfoPdfModel model) throws DocumentException {
        Font cellFont = new Font(baseFont, 12, Font.NORMAL);
        PdfPTable table = new PdfPTable(new float[]{95F, 331F});
        table.setTotalWidth(TABLE_TOTAL_WIDTH);
        table.setLockedWidth(true);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.setSpacingBefore(3F);

        for (LabelValue row : model.fileInfoRows()) {
            addRow(table, row.label(), row.value(), cellFont);
        }

        document.add(table);
    }

    private void addRow(PdfPTable table, String label, String value, Font cellFont) {
        table.addCell(buildCell(label, cellFont));
        table.addCell(buildCell(value, cellFont));
    }

    private PdfPCell buildCell(String value, Font cellFont) {
        PdfPCell cell = new PdfPCell(new Phrase(value, cellFont));
        cell.setPaddingTop(tableCellVerticalPadding());
        cell.setPaddingBottom(tableCellVerticalPadding());
        cell.setPaddingLeft(8F);
        cell.setPaddingRight(8F);
        cell.setLeading(0F, 1.35F);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    float tableCellVerticalPadding() {
        return 8F;
    }

    private String resolveProjectName(TenderDocument tenderDocument) {
        return defaultValue(tenderDocument == null ? null : tenderDocument.getProjectName());
    }

    private String resolveProjectCode(TenderDocument tenderDocument) {
        if (tenderDocument == null) {
            return "-";
        }
        return defaultValue(StringUtils.hasText(tenderDocument.getProjectCode())
                ? tenderDocument.getProjectCode()
                : tenderDocument.getProjectId());
    }

    private String resolveEnterpriseName(TenderDocumentUserContext userContext) {
        if (userContext == null) {
            return "-";
        }
        if (StringUtils.hasText(userContext.getEnterpriseName())) {
            return userContext.getEnterpriseName();
        }
        if (StringUtils.hasText(userContext.getEnterpriseCode())) {
            return userContext.getEnterpriseCode();
        }
        return "-";
    }

    private String resolveWatermarkText() {
        return properties == null ? null : properties.getCompileInfoWatermarkText();
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String defaultValue(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }

    private String formatVersionNo(Integer versionNo) {
        return versionNo == null ? "-" : "V" + versionNo + ".0";
    }

    private String formatFileSize(Long fileSize) {
        if (fileSize == null || fileSize <= 0) {
            return "-";
        }
        long kb = Math.max(1L, (fileSize + 1023L) / 1024L);
        return NumberFormat.getIntegerInstance(Locale.US).format(kb) + "KB";
    }

    record CompileInfoPdfModel(String title,
                               String watermarkText,
                               List<LabelValue> baseInfoLines,
                               List<LabelValue> fileInfoRows) {
    }

    record LabelValue(String label, String value) {
    }

    record WatermarkPlacement(float x, float y) {
    }
}
