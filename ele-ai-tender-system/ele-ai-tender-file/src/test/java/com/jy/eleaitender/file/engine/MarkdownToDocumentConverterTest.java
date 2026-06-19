package com.jy.eleaitender.file.engine;

import com.deepoove.poi.data.DocumentRenderData;
import com.deepoove.poi.data.NumberingRenderData;
import com.deepoove.poi.data.ParagraphRenderData;
import com.deepoove.poi.data.TextRenderData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownToDocumentConverterTest {

    private final MarkdownToDocumentConverter converter = new MarkdownToDocumentConverter();

    // ==================== 辅助方法 ====================

    /**
     * 从 DocumentRenderData 中提取所有段落
     */
    private List<ParagraphRenderData> getParagraphs(DocumentRenderData doc) {
        return doc.getContents().stream()
                .filter(r -> r instanceof ParagraphRenderData)
                .map(r -> (ParagraphRenderData) r)
                .collect(Collectors.toList());
    }

    /**
     * 从 DocumentRenderData 中提取所有编号列表
     */
    private List<NumberingRenderData> getNumberings(DocumentRenderData doc) {
        return doc.getContents().stream()
                .filter(r -> r instanceof NumberingRenderData)
                .map(r -> (NumberingRenderData) r)
                .collect(Collectors.toList());
    }

    /**
     * 从段落中提取纯文本内容
     */
    private String getParagraphText(ParagraphRenderData para) {
        if (para.getContents() == null) {
            return "";
        }
        return para.getContents().stream()
                .filter(r -> r instanceof TextRenderData)
                .map(r -> ((TextRenderData) r).getText())
                .collect(Collectors.joining());
    }

    /**
     * 从段落中提取所有 TextRenderData
     */
    private List<TextRenderData> getTextRenderDataList(ParagraphRenderData para) {
        if (para.getContents() == null) {
            return List.of();
        }
        return para.getContents().stream()
                .filter(r -> r instanceof TextRenderData)
                .map(r -> (TextRenderData) r)
                .collect(Collectors.toList());
    }

    // ==================== 边界情况测试 ====================

    @Nested
    @DisplayName("边界情况测试")
    class EdgeCaseTest {

        @Test
        @DisplayName("null输入返回非null空对象")
        void nullInput() {
            DocumentRenderData result = converter.convert(null);
            assertNotNull(result, "null输入不应返回null");
            assertTrue(result.getContents().isEmpty(), "null输入应返回空内容列表");
        }

        @Test
        @DisplayName("空白字符串返回非null空对象")
        void blankInput() {
            DocumentRenderData result = converter.convert("   ");
            assertNotNull(result, "空白字符串不应返回null");
            assertTrue(result.getContents().isEmpty(), "空白字符串应返回空内容列表");
        }

        @Test
        @DisplayName("空字符串返回非null空对象")
        void emptyInput() {
            DocumentRenderData result = converter.convert("");
            assertNotNull(result, "空字符串不应返回null");
            assertTrue(result.getContents().isEmpty(), "空字符串应返回空内容列表");
        }
    }

    // ==================== 标题转换测试 ====================

    @Nested
    @DisplayName("标题转换测试")
    class HeadingTest {

        @Test
        @DisplayName("一级标题转换后内容非空")
        void heading1() {
            DocumentRenderData result = converter.convert("# 招标公告");
            assertFalse(result.getContents().isEmpty(), "一级标题应产生内容");

            List<ParagraphRenderData> paragraphs = getParagraphs(result);
            assertFalse(paragraphs.isEmpty(), "一级标题应产生段落");
            assertTrue(getParagraphText(paragraphs.get(0)).contains("招标公告"),
                    "一级标题文本应包含'招标公告'");
        }

        @Test
        @DisplayName("二级标题转换后内容非空")
        void heading2() {
            DocumentRenderData result = converter.convert("## 项目概述");
            List<ParagraphRenderData> paragraphs = getParagraphs(result);
            assertFalse(paragraphs.isEmpty(), "二级标题应产生段落");
            assertTrue(getParagraphText(paragraphs.get(0)).contains("项目概述"),
                    "二级标题文本应包含'项目概述'");
        }

        @Test
        @DisplayName("三级标题转换后内容非空")
        void heading3() {
            DocumentRenderData result = converter.convert("### 技术要求");
            List<ParagraphRenderData> paragraphs = getParagraphs(result);
            assertFalse(paragraphs.isEmpty(), "三级标题应产生段落");
            assertTrue(getParagraphText(paragraphs.get(0)).contains("技术要求"),
                    "三级标题文本应包含'技术要求'");
        }

        @Test
        @DisplayName("标题应具有加粗样式")
        void headingIsBold() {
            DocumentRenderData result = converter.convert("# 标题文本");
            ParagraphRenderData para = getParagraphs(result).get(0);
            List<TextRenderData> texts = getTextRenderDataList(para);
            assertFalse(texts.isEmpty(), "标题段落应有文本内容");
            // 标题行内文本应该有加粗样式
            boolean hasBold = texts.stream()
                    .anyMatch(t -> t.getStyle() != null && Boolean.TRUE.equals(t.getStyle().isBold()));
            assertTrue(hasBold, "标题文本应有加粗样式");
        }
    }

    // ==================== 段落文本测试 ====================

    @Nested
    @DisplayName("段落文本转换测试")
    class ParagraphTest {

        @Test
        @DisplayName("纯文本段落转换后内容非空")
        void plainParagraph() {
            DocumentRenderData result = converter.convert("这是一段普通文本。");
            List<ParagraphRenderData> paragraphs = getParagraphs(result);
            assertFalse(paragraphs.isEmpty(), "段落应产生内容");
            assertTrue(getParagraphText(paragraphs.get(0)).contains("普通文本"),
                    "段落文本应包含'普通文本'");
        }

        @Test
        @DisplayName("多个段落转换后产生多个段落")
        void multipleParagraphs() {
            DocumentRenderData result = converter.convert("第一段\n\n第二段");
            List<ParagraphRenderData> paragraphs = getParagraphs(result);
            assertEquals(2, paragraphs.size(), "两个段落应产生两个ParagraphRenderData");
        }
    }

    // ==================== 行内格式测试 ====================

    @Nested
    @DisplayName("行内格式转换测试")
    class InlineFormatTest {

        @Test
        @DisplayName("加粗文本转换后内容非空")
        void boldText() {
            DocumentRenderData result = converter.convert("这是**加粗**文本");
            List<ParagraphRenderData> paragraphs = getParagraphs(result);
            assertFalse(paragraphs.isEmpty(), "加粗文本应产生段落");

            String text = getParagraphText(paragraphs.get(0));
            assertTrue(text.contains("加粗"), "加粗文本应包含'加粗'字样");
        }

        @Test
        @DisplayName("加粗文本应有加粗样式")
        void boldTextStyle() {
            DocumentRenderData result = converter.convert("**加粗内容**");
            ParagraphRenderData para = getParagraphs(result).get(0);
            List<TextRenderData> texts = getTextRenderDataList(para);
            boolean hasBold = texts.stream()
                    .anyMatch(t -> t.getStyle() != null && Boolean.TRUE.equals(t.getStyle().isBold()));
            assertTrue(hasBold, "加粗文本应有加粗样式");
        }

        @Test
        @DisplayName("斜体文本转换后内容非空")
        void italicText() {
            DocumentRenderData result = converter.convert("这是*斜体*文本");
            List<ParagraphRenderData> paragraphs = getParagraphs(result);
            assertFalse(paragraphs.isEmpty());
            String text = getParagraphText(paragraphs.get(0));
            assertTrue(text.contains("斜体"), "斜体文本应包含'斜体'字样");
        }

        @Test
        @DisplayName("行内代码转换后内容非空")
        void inlineCode() {
            DocumentRenderData result = converter.convert("使用`code`代码");
            List<ParagraphRenderData> paragraphs = getParagraphs(result);
            assertFalse(paragraphs.isEmpty());
            String text = getParagraphText(paragraphs.get(0));
            assertTrue(text.contains("code"), "行内代码应包含代码内容");
        }
    }

    // ==================== 列表转换测试 ====================

    @Nested
    @DisplayName("列表转换测试")
    class ListTest {

        @Test
        @DisplayName("无序列表转换后内容非空")
        void bulletList() {
            DocumentRenderData result = converter.convert("- 项目一\n- 项目二\n- 项目三");
            List<NumberingRenderData> numberings = getNumberings(result);
            assertFalse(numberings.isEmpty(), "无序列表应产生NumberingRenderData");
        }

        @Test
        @DisplayName("无序列表包含正确项目数")
        void bulletListItemCount() {
            DocumentRenderData result = converter.convert("- 项目一\n- 项目二\n- 项目三");
            NumberingRenderData numbering = getNumberings(result).get(0);
            assertFalse(numbering.getItems().isEmpty(), "无序列表应包含列表项");
            assertEquals(3, numbering.getItems().size(), "应包含3个列表项");
        }

        @Test
        @DisplayName("有序列表转换后内容非空")
        void orderedList() {
            DocumentRenderData result = converter.convert("1. 第一步\n2. 第二步\n3. 第三步");
            List<NumberingRenderData> numberings = getNumberings(result);
            assertFalse(numberings.isEmpty(), "有序列表应产生NumberingRenderData");
        }

        @Test
        @DisplayName("有序列表包含正确项目数")
        void orderedListItemCount() {
            DocumentRenderData result = converter.convert("1. 第一步\n2. 第二步");
            NumberingRenderData numbering = getNumberings(result).get(0);
            assertFalse(numbering.getItems().isEmpty(), "有序列表应包含列表项");
            assertEquals(2, numbering.getItems().size(), "应包含2个列表项");
        }
    }

    // ==================== 引用转换测试 ====================

    @Nested
    @DisplayName("引用转换测试")
    class BlockQuoteTest {

        @Test
        @DisplayName("引用转换后内容非空")
        void blockQuote() {
            DocumentRenderData result = converter.convert("> 这是引用文本");
            List<ParagraphRenderData> paragraphs = getParagraphs(result);
            assertFalse(paragraphs.isEmpty(), "引用应产生段落");
        }

        @Test
        @DisplayName("引用文本包含引用前缀")
        void blockQuoteHasPrefix() {
            DocumentRenderData result = converter.convert("> 引用内容");
            ParagraphRenderData para = getParagraphs(result).get(0);
            String text = getParagraphText(para);
            assertTrue(text.startsWith(">"), "引用段落文本应以'>'开头");
        }

        @Test
        @DisplayName("引用内含正确文本")
        void blockQuoteTextContent() {
            DocumentRenderData result = converter.convert("> 引用内容");
            ParagraphRenderData para = getParagraphs(result).get(0);
            String text = getParagraphText(para);
            assertTrue(text.contains("引用内容"), "引用段落应包含原文内容");
        }
    }

    // ==================== 代码块转换测试 ====================

    @Nested
    @DisplayName("代码块转换测试")
    class CodeBlockTest {

        @Test
        @DisplayName("代码块转换后内容非空")
        void fencedCodeBlock() {
            DocumentRenderData result = converter.convert("```\ncode line\n```");
            List<ParagraphRenderData> paragraphs = getParagraphs(result);
            assertFalse(paragraphs.isEmpty(), "代码块应产生段落");
        }

        @Test
        @DisplayName("代码块包含代码内容")
        void fencedCodeBlockContent() {
            DocumentRenderData result = converter.convert("```java\nSystem.out.println\n```");
            ParagraphRenderData para = getParagraphs(result).get(0);
            String text = getParagraphText(para);
            assertTrue(text.contains("System.out.println"), "代码块应包含代码内容");
        }
    }

    // ==================== 混合内容测试 ====================

    @Nested
    @DisplayName("混合内容转换测试")
    class MixedContentTest {

        @Test
        @DisplayName("混合内容转换后内容非空")
        void mixedContent() {
            String markdown = """
                    # 招标公告

                    本项目为小额交易工程。

                    **重要提示**：请仔细阅读。

                    - 条款一
                    - 条款二

                    > 引用内容
                    """;
            DocumentRenderData result = converter.convert(markdown);
            assertFalse(result.getContents().isEmpty(), "混合内容应产生非空结果");
        }

        @Test
        @DisplayName("混合内容产生多种类型的渲染数据")
        void mixedContentMultipleTypes() {
            String markdown = """
                    # 标题

                    段落文本

                    - 列表项
                    """;
            DocumentRenderData result = converter.convert(markdown);

            boolean hasParagraph = result.getContents().stream()
                    .anyMatch(r -> r instanceof ParagraphRenderData);
            boolean hasNumbering = result.getContents().stream()
                    .anyMatch(r -> r instanceof NumberingRenderData);

            assertTrue(hasParagraph, "混合内容应包含段落");
            assertTrue(hasNumbering, "混合内容应包含编号列表");
        }

        @Test
        @DisplayName("标题+段落+列表的完整场景")
        void fullScenario() {
            String markdown = """
                    # 项目名称

                    项目描述内容。

                    1. 第一步
                    2. 第二步

                    > 注意事项
                    """;
            DocumentRenderData result = converter.convert(markdown);
            List<ParagraphRenderData> paragraphs = getParagraphs(result);
            List<NumberingRenderData> numberings = getNumberings(result);

            assertFalse(paragraphs.isEmpty(), "应有段落内容");
            assertFalse(numberings.isEmpty(), "应有列表内容");
            assertTrue(paragraphs.size() >= 3, "至少应有标题、描述段落和引用3个段落");
        }
    }
}
