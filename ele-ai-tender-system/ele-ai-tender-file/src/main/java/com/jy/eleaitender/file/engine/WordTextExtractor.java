package com.jy.eleaitender.file.engine;

import com.jy.eleaitender.common.dto.LocationRefVO;
import com.jy.eleaitender.common.util.TextNormalizeUtil;
import lombok.Data;
import org.apache.poi.xwpf.usermodel.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Word文档文本提取器
 * 提取段落级文本+位置索引，供检测定位使用
 */
public class WordTextExtractor {

    @Data
    public static class ExtractResult {
        private List<TextSegment> segments;
        private String fullText;
    }

    @Data
    public static class TextSegment {
        private String type;          // "paragraph" | "table"
        private int elementIndex;     // IBodyElement序号
        private Integer tableIndex;   // 表格序号（table类型）
        private Integer rowIndex;     // 行序号（table类型）
        private Integer cellIndex;    // 列序号（table类型）
        private String text;
        private int fullTextOffset;   // 在fullText中的起始偏移量
    }

    /**
     * 从Word文档提取文本+位置索引
     */
    public static ExtractResult extract(XWPFDocument document) {
        ExtractResult result = new ExtractResult();
        List<TextSegment> segments = new ArrayList<>();
        StringBuilder fullTextBuilder = new StringBuilder();

        List<IBodyElement> bodyElements = document.getBodyElements();
        int elementIndex = 0;
        int tableCounter = 0;

        for (IBodyElement element : bodyElements) {
            if (element instanceof XWPFParagraph para) {
                String text = para.getText();
                if (text == null) text = "";

                TextSegment seg = new TextSegment();
                seg.setType("paragraph");
                seg.setElementIndex(elementIndex);
                seg.setText(text);
                seg.setFullTextOffset(fullTextBuilder.length());
                segments.add(seg);

                if (!fullTextBuilder.isEmpty()) {
                    fullTextBuilder.append('\n');
                }
                fullTextBuilder.append(text);

            } else if (element instanceof XWPFTable table) {
                for (int rowIdx = 0; rowIdx < table.getNumberOfRows(); rowIdx++) {
                    XWPFTableRow row = table.getRow(rowIdx);
                    for (int cellIdx = 0; cellIdx < row.getTableCells().size(); cellIdx++) {
                        XWPFTableCell cell = row.getTableCells().get(cellIdx);
                        String text = cell.getText();
                        if (text == null) text = "";

                        TextSegment seg = new TextSegment();
                        seg.setType("table");
                        seg.setElementIndex(elementIndex);
                        seg.setTableIndex(tableCounter);
                        seg.setRowIndex(rowIdx);
                        seg.setCellIndex(cellIdx);
                        seg.setText(text);
                        seg.setFullTextOffset(fullTextBuilder.length());
                        segments.add(seg);

                        if (!fullTextBuilder.isEmpty()) {
                            fullTextBuilder.append('\n');
                        }
                        fullTextBuilder.append(text);
                    }
                }
                tableCounter++;
            }
            elementIndex++;
        }

        result.setSegments(segments);
        result.setFullText(fullTextBuilder.toString());
        return result;
    }

    /**
     * 在ExtractResult中定位original文本所在的段落
     * 策略：精确匹配优先 → 规范化匹配兜底
     *
     * @param result   提取结果
     * @param original 要定位的原文
     * @return 位置索引，匹配失败返回null
     */
    public static LocationRefVO locate(ExtractResult result, String original) {
        if (original == null || original.isEmpty() || result == null) {
            return null;
        }

        String fullText = result.getFullText();
        int offset = -1;

        // Level 1: 精确匹配
        offset = fullText.indexOf(original);

        // Level 2: 规范化匹配
        if (offset < 0) {
            offset = findNormalizedOffset(fullText, original);
        }

        if (offset < 0) {
            return null;
        }

        // 根据偏移量在segments中二分查找
        TextSegment target = findSegmentByOffset(result.getSegments(), offset);
        if (target == null) {
            return null;
        }

        LocationRefVO ref = new LocationRefVO();
        ref.setType(target.getType());
        ref.setElementIndex(target.getElementIndex());
        if ("table".equals(target.getType())) {
            ref.setTableIndex(target.getTableIndex());
            ref.setRowIndex(target.getRowIndex());
            ref.setCellIndex(target.getCellIndex());
        }
        return ref;
    }

    /**
     * 规范化匹配：在fullText中找到original规范化等价子串的偏移量
     */
    private static int findNormalizedOffset(String fullText, String original) {
        String normalizedFull = TextNormalizeUtil.normalize(fullText);
        String normalizedOriginal = TextNormalizeUtil.normalize(original);

        int normStart = normalizedFull.indexOf(normalizedOriginal);
        if (normStart < 0) return -1;

        // 构建normToRaw映射
        int[] normToRaw = buildNormToRawMapping(fullText, normalizedFull);
        return normToRaw[normStart];
    }

    /**
     * 构建规范化文本到原始文本的位置映射
     */
    private static int[] buildNormToRawMapping(String rawText, String normalizedText) {
        int[] normToRaw = new int[normalizedText.length() + 1];
        int rawIdx = 0;
        int normIdx = 0;

        while (normIdx <= normalizedText.length() && rawIdx <= rawText.length()) {
            if (rawIdx < rawText.length() && TextNormalizeUtil.isWhitespaceChar(rawText.charAt(rawIdx))) {
                rawIdx++;
                continue;
            }
            normToRaw[normIdx] = rawIdx;
            if (normIdx == normalizedText.length()) break;
            rawIdx++;
            normIdx++;
        }
        return normToRaw;
    }

    /**
     * 二分查找：根据fullTextOffset找到对应的TextSegment
     */
    private static TextSegment findSegmentByOffset(List<TextSegment> segments, int offset) {
        int lo = 0, hi = segments.size() - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            TextSegment seg = segments.get(mid);
            if (seg.getFullTextOffset() <= offset) {
                // 检查是否在当前segment范围内
                int segEnd = mid + 1 < segments.size()
                        ? segments.get(mid + 1).getFullTextOffset()
                        : Integer.MAX_VALUE;
                if (offset < segEnd) {
                    return seg;
                }
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return null;
    }
}
