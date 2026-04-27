package com.jy.eleaitender.file.engine;

import com.jy.eleaitender.common.dto.FixReplacement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Word文档修复引擎
 * 基于Apache POI实现文本查找替换，用于智能检测修复
 */
@Slf4j
@Component
public class WordDocumentFixEngine {

    /**
     * 对Word文档执行文本替换
     *
     * @param docxBytes   原始文档字节
     * @param replacements 替换列表
     * @return 修复结果
     */
    public FixResult fix(byte[] docxBytes, List<FixReplacement> replacements) {
        int fixedCount = 0;
        int failedCount = 0;

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docxBytes));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            for (FixReplacement rep : replacements) {
                boolean found = replaceInDocument(doc, rep.getOriginal(), rep.getTargeted());
                if (found) {
                    fixedCount++;
                } else {
                    failedCount++;
                    log.warn("未找到原文，跳过修复: original={}",
                            rep.getOriginal().substring(0, Math.min(50, rep.getOriginal().length())));
                }
            }

            doc.write(out);
            return new FixResult(out.toByteArray(), fixedCount, failedCount);
        } catch (Exception e) {
            throw new RuntimeException("Word文档修复失败: " + e.getMessage(), e);
        }
    }

    private boolean replaceInDocument(XWPFDocument doc, String original, String targeted) {
        boolean found = false;

        for (XWPFParagraph para : doc.getParagraphs()) {
            if (replaceInParagraph(para, original, targeted)) {
                found = true;
            }
        }

        for (XWPFTable table : doc.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph para : cell.getParagraphs()) {
                        if (replaceInParagraph(para, original, targeted)) {
                            found = true;
                        }
                    }
                }
            }
        }

        for (XWPFHeader header : doc.getHeaderList()) {
            for (XWPFParagraph para : header.getParagraphs()) {
                if (replaceInParagraph(para, original, targeted)) {
                    found = true;
                }
            }
        }
        for (XWPFFooter footer : doc.getFooterList()) {
            for (XWPFParagraph para : footer.getParagraphs()) {
                if (replaceInParagraph(para, original, targeted)) {
                    found = true;
                }
            }
        }

        return found;
    }

    /**
     * 替换段落中的文本
     * 策略：合并所有Run文本，若包含原文则清空所有Run并将替换后文本写入第一个Run
     */
    private boolean replaceInParagraph(XWPFParagraph para, String original, String targeted) {
        String fullText = para.getText();
        if (fullText == null || !fullText.contains(original)) {
            return false;
        }

        List<XWPFRun> runs = para.getRuns();
        if (runs.isEmpty()) return false;

        XWPFRun firstRun = runs.get(0);
        String replaced = fullText.replace(original, targeted);

        for (XWPFRun run : runs) {
            run.setText("", 0);
        }
        firstRun.setText(replaced, 0);

        return true;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FixResult {
        private byte[] documentBytes;
        private int fixedCount;
        private int failedCount;
    }
}
