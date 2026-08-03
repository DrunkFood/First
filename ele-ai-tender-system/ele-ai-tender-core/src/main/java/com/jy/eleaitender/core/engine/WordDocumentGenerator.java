package com.jy.eleaitender.core.engine;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Word文档生成器
 * 使用 Apache POI + jsoup 将 HTML 内容渲染为格式化 Word 文档
 * 确保与前端HTML预览格式一致
 */
@Slf4j
@Component
public class WordDocumentGenerator {

    private static final String AI_DISCLAIMER_TITLE = "【AI辅助生成·仅供参考】";
    private static final String AI_DISCLAIMER_CONTENT = "本文档由AI工具辅助生成，仅供使用者参考、编辑与格式借鉴，不构成我们提供的任何形式的专业法律、技术或商业建议，不构成可直接提交的最终招标文件，亦不代表我们对招标项目内容、数据的任何承诺、审查或保证。使用者必须结合具体项目需求、法律法规及招标文件要求，对本文档的全部内容进行独立审查、修正和核实，并自行承担使用本文档产生的全部风险与责任。因未履行上述审核义务而直接使用本文档所造成的任何损失，我们均不承担任何责任。";
    private static final String DISCLAIMER_BACKGROUND_COLOR = "FFFF00";

    /**
     * 根据数据模型和HTML内容生成Word文档字节数组
     * HTML内容通过jsoup解析后渲染为格式化的Word段落
     *
     * @param documentData 文档数据
     * @param htmlContent  已生成的HTML内容
     * @return .docx 文件字节数组
     */
    public byte[] generate(Map<String, Object> documentData, String htmlContent) {
        log.info("开始生成Word文档，数据字段数: {}", documentData.size());

        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            addAiDisclaimer(doc);

            // 文档标题
            String projectName = String.valueOf(documentData.getOrDefault("projectName", "招标文件"));
            addTitle(doc, projectName);

            // 解析HTML并渲染到Word
            org.jsoup.nodes.Document htmlDoc = Jsoup.parse(htmlContent);
            renderHtmlToWord(doc, htmlDoc.body());

            doc.write(out);
            byte[] result = out.toByteArray();
            log.info("Word文档生成完成，文件大小: {} bytes", result.length);
            return result;
        } catch (IOException e) {
            log.error("Word文档生成失败", e);
            throw new RuntimeException("Word文档生成失败: " + e.getMessage(), e);
        }
    }

    private void addAiDisclaimer(XWPFDocument doc) {
        XWPFParagraph titleParagraph = doc.createParagraph();
        titleParagraph.setSpacingBefore(0);
        titleParagraph.setSpacingAfter(0);
        titleParagraph.setIndentationLeft(0);
        setParagraphShading(titleParagraph, DISCLAIMER_BACKGROUND_COLOR);
        XWPFRun titleRun = titleParagraph.createRun();
        titleRun.setText(AI_DISCLAIMER_TITLE);
        titleRun.setBold(true);
        titleRun.setFontSize(14);
        titleRun.setFontFamily("宋体");

        XWPFParagraph contentParagraph = doc.createParagraph();
        contentParagraph.setSpacingBefore(0);
        contentParagraph.setSpacingAfter(200);
        contentParagraph.setIndentationLeft(0);
        setParagraphShading(contentParagraph, DISCLAIMER_BACKGROUND_COLOR);
        XWPFRun contentRun = contentParagraph.createRun();
        contentRun.setText(AI_DISCLAIMER_CONTENT);
        contentRun.setFontSize(12);
        contentRun.setFontFamily("宋体");
    }

    private void setParagraphShading(XWPFParagraph paragraph, String colorHex) {
        try {
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr paragraphProperties =
                    paragraph.getCTP().isSetPPr() ? paragraph.getCTP().getPPr() : paragraph.getCTP().addNewPPr();
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd shading =
                    paragraphProperties.isSetShd() ? paragraphProperties.getShd() : paragraphProperties.addNewShd();
            shading.setFill(colorHex);
        } catch (Exception e) {
            log.debug("设置免责声明背景色失败（可忽略）", e);
        }
    }

    private void addTitle(XWPFDocument doc, String title) {
        XWPFParagraph para = doc.createParagraph();
        para.setAlignment(ParagraphAlignment.CENTER);
        para.setSpacingAfter(200);
        XWPFRun run = para.createRun();
        run.setText(title);
        run.setBold(true);
        run.setFontSize(22);
        run.setFontFamily("宋体");
    }

    /**
     * 将jsoup解析的HTML元素树渲染为Word内容
     */
    private void renderHtmlToWord(XWPFDocument doc, Element container) {
        for (Element child : container.children()) {
            String tagName = child.tagName().toLowerCase();
            switch (tagName) {
                case "h1" -> addHeading(doc, child.text(), 22, true);
                case "h2" -> addHeading(doc, child.text(), 18, true);
                case "h3" -> addHeading(doc, child.text(), 16, true);
                case "h4" -> addHeading(doc, child.text(), 14, true);
                case "h5" -> addHeading(doc, child.text(), 13, true);
                case "h6" -> addHeading(doc, child.text(), 12, true);
                case "p" -> addRichParagraph(doc, child);
                case "table" -> addTable(doc, child);
                case "ul" -> addList(doc, child, false);
                case "ol" -> addList(doc, child, true);
                case "blockquote" -> addBlockquote(doc, child);
                case "hr" -> addHorizontalRule(doc);
                default -> {
                    // 对于未识别的标签，递归处理子元素
                    if (!child.children().isEmpty()) {
                        renderHtmlToWord(doc, child);
                    } else if (!child.text().isBlank()) {
                        addParagraph(doc, child.text());
                    }
                }
            }
        }
    }

    private void addHeading(XWPFDocument doc, String text, int fontSize, boolean bold) {
        XWPFParagraph para = doc.createParagraph();
        para.setSpacingBefore(200);
        para.setSpacingAfter(100);
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setFontSize(fontSize);
        run.setFontFamily("宋体");
    }

    private void addParagraph(XWPFDocument doc, String text) {
        XWPFParagraph para = doc.createParagraph();
        para.setSpacingAfter(80);
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setFontSize(12);
        run.setFontFamily("宋体");
    }

    /**
     * 渲染包含内联格式（加粗、斜体等）的段落
     */
    private void addRichParagraph(XWPFDocument doc, Element pElement) {
        XWPFParagraph para = doc.createParagraph();
        para.setSpacingAfter(80);
        addInlineContent(para, pElement);
    }

    /**
     * 递归处理内联内容（文本+加粗+斜体等）
     */
    private void addInlineContent(XWPFParagraph para, Element element) {
        for (org.jsoup.nodes.Node node : element.childNodes()) {
            if (node instanceof org.jsoup.nodes.TextNode textNode) {
                String text = textNode.text();
                if (!text.isBlank()) {
                    XWPFRun run = para.createRun();
                    run.setText(text);
                    run.setFontSize(12);
                    run.setFontFamily("宋体");
                }
            } else if (node instanceof Element childEl) {
                String tag = childEl.tagName().toLowerCase();
                XWPFRun run = para.createRun();
                run.setFontSize(12);
                run.setFontFamily("宋体");

                switch (tag) {
                    case "strong", "b" -> {
                        run.setBold(true);
                        run.setText(childEl.text());
                    }
                    case "em", "i" -> {
                        run.setItalic(true);
                        run.setText(childEl.text());
                    }
                    case "u" -> {
                        run.setUnderline(UnderlinePatterns.SINGLE);
                        run.setText(childEl.text());
                    }
                    case "code" -> {
                        run.setFontFamily("Courier New");
                        run.setFontSize(11);
                        run.setText(childEl.text());
                    }
                    case "br" -> run.addBreak();
                    case "a" -> {
                        String href = childEl.attr("href");
                        run.setText(childEl.text() + (href.isEmpty() ? "" : "(" + href + ")"));
                        run.setColor("0563C1");
                        run.setUnderline(UnderlinePatterns.SINGLE);
                    }
                    default -> run.setText(childEl.text());
                }
            }
        }
    }

    /**
     * 渲染HTML表格为Word表格
     */
    private void addTable(XWPFDocument doc, Element tableEl) {
        Elements rows = tableEl.select("tr");
        if (rows.isEmpty()) return;

        // 计算列数
        int maxCols = rows.stream()
                .mapToInt(row -> row.select("td, th").size())
                .max().orElse(0);
        if (maxCols == 0) return;

        XWPFTable table = doc.createTable(rows.size(), maxCols);
        table.setWidth("100%");

        // 设置表格样式
        table.setStyleID("TableGrid");

        int rowIdx = 0;
        for (Element row : rows) {
            Elements cells = row.select("td, th");
            boolean isHeader = row.select("th").size() > 0 && row.select("td").isEmpty();

            for (int colIdx = 0; colIdx < cells.size() && colIdx < maxCols; colIdx++) {
                XWPFTableCell cell = table.getRow(rowIdx).getCell(colIdx);
                String cellText = cells.get(colIdx).text();

                // 清除默认段落，添加格式化内容
                cell.removeParagraph(0);
                XWPFParagraph para = cell.addParagraph();
                para.setSpacingAfter(0);
                para.setSpacingBefore(0);
                XWPFRun run = para.createRun();
                run.setText(cellText);
                run.setFontSize(11);
                run.setFontFamily("宋体");

                if (isHeader) {
                    run.setBold(true);
                    // 表头背景色通过CTShd设置
                    setCellShading(cell, "D9E2F3");
                }
            }
            rowIdx++;
        }

        // 添加表格后的空行
        doc.createParagraph();
    }

    private void setCellShading(XWPFTableCell cell, String colorHex) {
        try {
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd shd = cell.getCTTc()
                    .addNewTcPr()
                    .addNewShd();
            shd.setFill(colorHex);
        } catch (Exception e) {
            log.debug("设置单元格背景色失败（可忽略）", e);
        }
    }

    /**
     * 渲染列表（无序/有序）
     */
    private void addList(XWPFDocument doc, Element listEl, boolean ordered) {
        Elements items = listEl.select("> li");
        int orderNum = 1;

        for (Element item : items) {
            XWPFParagraph para = doc.createParagraph();
            para.setSpacingAfter(40);
            para.setIndentationLeft(400);

            XWPFRun bulletRun = para.createRun();
            bulletRun.setFontSize(12);
            bulletRun.setFontFamily("宋体");

            if (ordered) {
                bulletRun.setText(orderNum++ + ". ");
            } else {
                bulletRun.setText("• ");
            }

            // 列表项内容（可能含内联格式）
            addInlineContent(para, item);
        }
    }

    /**
     * 渲染引用块
     */
    private void addBlockquote(XWPFDocument doc, Element blockquoteEl) {
        XWPFParagraph para = doc.createParagraph();
        para.setSpacingAfter(80);
        para.setIndentationLeft(600);

        XWPFRun run = para.createRun();
        run.setText(blockquoteEl.text());
        run.setFontSize(12);
        run.setFontFamily("宋体");
        run.setItalic(true);
        run.setColor("666666");
    }

    /**
     * 添加水平线
     */
    private void addHorizontalRule(XWPFDocument doc) {
        XWPFParagraph para = doc.createParagraph();
        para.setSpacingBefore(100);
        para.setSpacingAfter(100);
        para.setBorderBottom(Borders.SINGLE);
    }
}
