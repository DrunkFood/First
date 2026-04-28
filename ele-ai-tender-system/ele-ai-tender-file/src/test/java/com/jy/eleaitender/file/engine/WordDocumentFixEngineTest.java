package com.jy.eleaitender.file.engine;

import com.jy.eleaitender.common.dto.FixReplacement;
import org.apache.poi.xwpf.usermodel.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WordDocumentFixEngineTest {

    private final WordDocumentFixEngine engine = new WordDocumentFixEngine();

    // ==================== 辅助方法 ====================

    private byte[] createDocWithText(String text) {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XWPFParagraph para = doc.createParagraph();
            XWPFRun run = para.createRun();
            run.setText(text, 0);
            doc.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("创建测试文档失败", e);
        }
    }

    private byte[] createDocWithTableText(String header, String value) {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XWPFTable table = doc.createTable(1, 1);
            XWPFTableCell cell = table.getRow(0).getCell(0);
            XWPFParagraph para = cell.getParagraphs().get(0);
            XWPFRun run = para.createRun();
            run.setText(value, 0);
            doc.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("创建测试文档失败", e);
        }
    }

    private FixReplacement replacement(String original, String targeted) {
        FixReplacement rep = new FixReplacement();
        rep.setOriginal(original);
        rep.setTargeted(targeted);
        return rep;
    }

    private void assertDocContains(byte[] docBytes, String expected) {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docBytes))) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph para : doc.getParagraphs()) {
                sb.append(para.getText());
            }
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph para : cell.getParagraphs()) {
                            sb.append(para.getText());
                        }
                    }
                }
            }
            assertTrue(sb.toString().contains(expected),
                    "文档中未找到期望文本: \"" + expected + "\", 实际内容: \"" + sb + "\"");
        } catch (Exception e) {
            throw new RuntimeException("读取文档失败", e);
        }
    }

    // ==================== normalize 测试 ====================

    @Nested
    @DisplayName("normalize 方法测试")
    class NormalizeTest {

        @Test
        @DisplayName("移除普通空格")
        void removeSpaces() {
            assertEquals("abc", WordDocumentFixEngine.normalize("a b c"));
        }

        @Test
        @DisplayName("移除换行")
        void removeNewlines() {
            assertEquals("abc", WordDocumentFixEngine.normalize("a\nb\nc"));
        }

        @Test
        @DisplayName("移除制表符")
        void removeTabs() {
            assertEquals("abc", WordDocumentFixEngine.normalize("a\tb\tc"));
        }

        @Test
        @DisplayName("移除全角空格")
        void removeFullWidthSpace() {
            assertEquals("abc", WordDocumentFixEngine.normalize("a　b　c"));
        }

        @Test
        @DisplayName("移除不间断空格")
        void removeNonBreakingSpace() {
            assertEquals("abc", WordDocumentFixEngine.normalize("a b c"));
        }

        @Test
        @DisplayName("移除前后空白")
        void trimWhitespace() {
            assertEquals("abc", WordDocumentFixEngine.normalize("  abc  "));
        }

        @Test
        @DisplayName("null返回空串")
        void nullReturnsEmpty() {
            assertEquals("", WordDocumentFixEngine.normalize(null));
        }

        @Test
        @DisplayName("空串返回空串")
        void emptyReturnsEmpty() {
            assertEquals("", WordDocumentFixEngine.normalize(""));
        }

        @Test
        @DisplayName("纯中文无空白不变")
        void pureChineseUnchanged() {
            assertEquals("招标文件", WordDocumentFixEngine.normalize("招标文件"));
        }

        @Test
        @DisplayName("混合空白全部移除")
        void mixedWhitespaceAllRemoved() {
            assertEquals("abc", WordDocumentFixEngine.normalize(" a \t b \n c "));
        }
    }

    // ==================== findActualOriginal 测试 ====================

    @Nested
    @DisplayName("findActualOriginal 方法测试")
    class FindActualOriginalTest {

        @Test
        @DisplayName("精确匹配 - 原样返回")
        void exactMatch() {
            assertEquals("招标文件", WordDocumentFixEngine.findActualOriginal("这是招标文件内容", "招标文件"));
        }

        @Test
        @DisplayName("AI原文有额外换行 - 仍能匹配文档实际文本")
        void aiHasExtraNewlines() {
            // 文档中是 "招标文件"，AI 检测结果中 original 是 "招\n标\n文\n件"
            String result = WordDocumentFixEngine.findActualOriginal("这是招标文件内容", "招\n标\n文\n件");
            assertEquals("招标文件", result);
        }

        @Test
        @DisplayName("文档有额外空格 - 匹配含空格的实际子串")
        void docHasExtraSpaces() {
            // 文档中是 "招 标 文 件"，AI 检测结果中 original 是 "招标文件"
            String result = WordDocumentFixEngine.findActualOriginal("这是招 标 文 件内容", "招标文件");
            assertEquals("招 标 文 件", result);
        }

        @Test
        @DisplayName("无匹配 - 返回null")
        void noMatch() {
            assertNull(WordDocumentFixEngine.findActualOriginal("这是内容", "不存在"));
        }

        @Test
        @DisplayName("匹配在中间位置")
        void matchInMiddle() {
            String rawText = "前缀内容招标文件后缀内容";
            String result = WordDocumentFixEngine.findActualOriginal(rawText, "招标文件");
            assertEquals("招标文件", result);
        }

        @Test
        @DisplayName("文档有全角空格 - 匹配含全角空格的实际子串")
        void docHasFullWidthSpace() {
            String result = WordDocumentFixEngine.findActualOriginal("这是招　标文件内容", "招标文件");
            assertEquals("招　标文件", result);
        }

        @Test
        @DisplayName("文档有不间断空格 - 匹配含不间断空格的实际子串")
        void docHasNbsp() {
            String result = WordDocumentFixEngine.findActualOriginal("这是招 标文件内容", "招标文件");
            assertEquals("招 标文件", result);
        }

        @Test
        @DisplayName("AI原文同时有换行和空格差异")
        void aiHasMixedWhitespaceDifference() {
            String result = WordDocumentFixEngine.findActualOriginal("这是招标文件内容", "招\n标 文 件");
            assertEquals("招标文件", result);
        }

        @Test
        @DisplayName("文档有换行但AI原文没有 - 反向差异")
        void docHasNewlineAiDoesNot() {
            String result = WordDocumentFixEngine.findActualOriginal("这是招\n标文件内容", "招标文件");
            assertEquals("招\n标文件", result);
        }
    }

    // ==================== 完整 fix 替换测试 ====================

    @Nested
    @DisplayName("完整 fix 替换测试")
    class FixTest {

        @Test
        @DisplayName("精确匹配替换")
        void exactMatchReplace() {
            byte[] doc = createDocWithText("本项目为限额以下工程");
            WordDocumentFixEngine.FixResult result = engine.fix(doc,
                    List.of(replacement("限额以下工程", "政府采购服务")));

            assertEquals(1, result.getFixedCount());
            assertEquals(0, result.getFailedCount());
            assertDocContains(result.getDocumentBytes(), "政府采购服务");
        }

        @Test
        @DisplayName("换行差异替换 - AI原文含换行，文档不含")
        void newlineDifferenceReplace() {
            byte[] doc = createDocWithText("本项目为限额以下工程");
            WordDocumentFixEngine.FixResult result = engine.fix(doc,
                    List.of(replacement("限额\n以下\n工程", "政府采购服务")));

            assertEquals(1, result.getFixedCount());
            assertEquals(0, result.getFailedCount());
            assertDocContains(result.getDocumentBytes(), "政府采购服务");
        }

        @Test
        @DisplayName("空格差异替换 - AI原文无空格，文档含空格")
        void spaceDifferenceReplace() {
            byte[] doc = createDocWithText("本项目为限 额 以 下 工程");
            WordDocumentFixEngine.FixResult result = engine.fix(doc,
                    List.of(replacement("限额以下工程", "政府采购服务")));

            assertEquals(1, result.getFixedCount());
            assertEquals(0, result.getFailedCount());
            assertDocContains(result.getDocumentBytes(), "政府采购服务");
        }

        @Test
        @DisplayName("全角空格差异替换 - 文档含全角空格")
        void fullWidthSpaceDifferenceReplace() {
            byte[] doc = createDocWithText("本项目为限　额以下工程");
            WordDocumentFixEngine.FixResult result = engine.fix(doc,
                    List.of(replacement("限额以下工程", "政府采购服务")));

            assertEquals(1, result.getFixedCount());
            assertEquals(0, result.getFailedCount());
            assertDocContains(result.getDocumentBytes(), "政府采购服务");
        }

        @Test
        @DisplayName("无匹配返回失败")
        void noMatchReturnsFailed() {
            byte[] doc = createDocWithText("本项目为限额以下工程");
            WordDocumentFixEngine.FixResult result = engine.fix(doc,
                    List.of(replacement("完全不存在的内容", "替换文本")));

            assertEquals(0, result.getFixedCount());
            assertEquals(1, result.getFailedCount());
        }

        @Test
        @DisplayName("表格单元格内替换")
        void tableCellReplace() {
            byte[] doc = createDocWithTableText("标题", "限额以下工程内容");
            WordDocumentFixEngine.FixResult result = engine.fix(doc,
                    List.of(replacement("限额以下工程", "政府采购服务")));

            assertEquals(1, result.getFixedCount());
            assertEquals(0, result.getFailedCount());
            assertDocContains(result.getDocumentBytes(), "政府采购服务");
        }

        @Test
        @DisplayName("多项替换 - 精确和模糊混合")
        void multipleReplacements() {
            byte[] doc = createDocWithText("限额以下工程和产权交易项目");
            WordDocumentFixEngine.FixResult result = engine.fix(doc,
                    List.of(
                            replacement("限额以下工程", "政府采购服务"),
                            replacement("产权\n交易", "公开招标")));

            assertEquals(2, result.getFixedCount());
            assertEquals(0, result.getFailedCount());
            assertDocContains(result.getDocumentBytes(), "政府采购服务");
            assertDocContains(result.getDocumentBytes(), "公开招标");
        }
    }
}
