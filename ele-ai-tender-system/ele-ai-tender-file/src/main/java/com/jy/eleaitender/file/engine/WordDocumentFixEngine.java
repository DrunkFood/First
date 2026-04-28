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
     * 策略：两级匹配
     *   Level 1: 精确匹配（原文直接包含）
     *   Level 2: 规范化匹配（移除空白后匹配，还原实际子串范围）
     */
    private boolean replaceInParagraph(XWPFParagraph para, String original, String targeted) {
        String fullText = para.getText();
        if (fullText == null) {
            return false;
        }

        // Level 1: 精确匹配
        if (fullText.contains(original)) {
            doReplace(para, fullText, original, targeted);
            return true;
        }

        // Level 2: 规范化匹配
        String actualOriginal = findActualOriginal(fullText, original);
        if (actualOriginal != null) {
            doReplace(para, fullText, actualOriginal, targeted);
            return true;
        }

        return false;
    }

    /**
     * 执行实际替换：清空所有Run，将替换后文本写入第一个Run
     */
    private void doReplace(XWPFParagraph para, String fullText, String actualOriginal, String targeted) {
        List<XWPFRun> runs = para.getRuns();
        if (runs.isEmpty()) return;

        XWPFRun firstRun = runs.get(0);
        String replaced = fullText.replace(actualOriginal, targeted);

        for (XWPFRun run : runs) {
            run.setText("", 0);
        }
        firstRun.setText(replaced, 0);
    }

    /**
     * 规范化文本：移除所有空白字符（空格、换行、制表符、全角空格、不间断空格等）
     */
    static String normalize(String text) {
        if (text == null) return "";
        // \s 不覆盖不间断空格(U+00A0)和全角空格(U+3000)，需显式补充
        return text.replaceAll("[\\s\\u00A0\\u3000]+", "");
    }

    /**
     * 在原始文本中找到与 original 规范化等价的子串
     * 通过位置映射还原原文实际范围，保留原文中的空白字符
     *
     * @param rawText  文档中的实际文本
     * @param original AI检测给出的原文（可能与文档文本有空白差异）
     * @return 文档中与 original 规范化等价的实际子串，无匹配返回 null
     */
    static String findActualOriginal(String rawText, String original) {
        String normalizedRaw = normalize(rawText);
        String normalizedOriginal = normalize(original);

        int normStart = normalizedRaw.indexOf(normalizedOriginal);
        if (normStart < 0) return null;
        int normEnd = normStart + normalizedOriginal.length();

        // 构建位置映射：normToRaw[i] = 规范化文本第i个字符在原始文本中的起始位置
        int[] normToRaw = new int[normalizedRaw.length() + 1];
        int rawIdx = 0;
        int normIdx = 0;

        while (normIdx <= normalizedRaw.length() && rawIdx <= rawText.length()) {
            if (rawIdx < rawText.length() && isWhitespaceChar(rawText.charAt(rawIdx))) {
                rawIdx++;
                continue;
            }
            normToRaw[normIdx] = rawIdx;
            if (normIdx == normalizedRaw.length()) break;
            rawIdx++;
            normIdx++;
        }

        return rawText.substring(normToRaw[normStart], normToRaw[normEnd]);
    }

    /**
     * 判断字符是否为空白（包括不间断空格和全角空格）
     */
    private static boolean isWhitespaceChar(char c) {
        return Character.isWhitespace(c) || c == '\u00A0' || c == '\u3000';
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
