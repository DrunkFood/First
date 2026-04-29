package com.jy.eleaitender.file.engine;

import com.jy.eleaitender.common.dto.LocationRefVO;
import org.apache.poi.xwpf.usermodel.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WordTextExtractorTest {

    private XWPFDocument createDocWithParagraphs(String... texts) {
        XWPFDocument doc = new XWPFDocument();
        for (String text : texts) {
            XWPFParagraph para = doc.createParagraph();
            XWPFRun run = para.createRun();
            run.setText(text);
        }
        return doc;
    }

    @Nested
    @DisplayName("ExtractResult 测试")
    class ExtractTest {

        @Test
        @DisplayName("单段落提取")
        void singleParagraph() throws Exception {
            XWPFDocument doc = createDocWithParagraphs("第一章 招标公告");
            WordTextExtractor.ExtractResult result = WordTextExtractor.extract(doc);

            assertEquals(1, result.getSegments().size());
            assertEquals("第一章 招标公告", result.getSegments().get(0).getText());
            assertEquals("paragraph", result.getSegments().get(0).getType());
            assertEquals(0, result.getSegments().get(0).getElementIndex());
            assertEquals("第一章 招标公告", result.getFullText());
        }

        @Test
        @DisplayName("多段落提取")
        void multipleParagraphs() throws Exception {
            XWPFDocument doc = createDocWithParagraphs("第一段", "第二段", "第三段");
            WordTextExtractor.ExtractResult result = WordTextExtractor.extract(doc);

            assertEquals(3, result.getSegments().size());
            assertEquals("第一段\n第二段\n第三段", result.getFullText());
            assertEquals(0, result.getSegments().get(0).getElementIndex());
            assertEquals(1, result.getSegments().get(1).getElementIndex());
            assertEquals(2, result.getSegments().get(2).getElementIndex());
        }

        @Test
        @DisplayName("含表格提取")
        void withTable() throws Exception {
            XWPFDocument doc = new XWPFDocument();
            XWPFParagraph p1 = doc.createParagraph();
            p1.createRun().setText("段落1");

            XWPFTable table = doc.createTable(2, 2);
            table.getRow(0).getCell(0).setText("A1");
            table.getRow(0).getCell(1).setText("B1");
            table.getRow(1).getCell(0).setText("A2");
            table.getRow(1).getCell(1).setText("B2");

            XWPFParagraph p2 = doc.createParagraph();
            p2.createRun().setText("段落2");

            WordTextExtractor.ExtractResult result = WordTextExtractor.extract(doc);

            assertEquals(6, result.getSegments().size());
            assertEquals("paragraph", result.getSegments().get(0).getType());
            assertEquals(0, result.getSegments().get(0).getElementIndex());
            assertEquals("table", result.getSegments().get(1).getType());
            assertEquals(1, result.getSegments().get(1).getElementIndex());
            assertEquals(0, result.getSegments().get(1).getTableIndex());
            assertEquals("paragraph", result.getSegments().get(5).getType());
            assertEquals(2, result.getSegments().get(5).getElementIndex());
        }

        @Test
        @DisplayName("空文档提取")
        void emptyDocument() throws Exception {
            XWPFDocument doc = new XWPFDocument();
            WordTextExtractor.ExtractResult result = WordTextExtractor.extract(doc);

            assertTrue(result.getSegments().isEmpty());
            assertEquals("", result.getFullText());
        }
    }

    @Nested
    @DisplayName("匹配定位测试")
    class LocateTest {

        @Test
        @DisplayName("精确匹配定位")
        void exactMatchLocate() throws Exception {
            XWPFDocument doc = createDocWithParagraphs("第一章", "招标公告", "项目概况");
            WordTextExtractor.ExtractResult result = WordTextExtractor.extract(doc);

            LocationRefVO ref = WordTextExtractor.locate(result, "招标公告");
            assertNotNull(ref);
            assertEquals("paragraph", ref.getType());
            assertEquals(1, ref.getElementIndex());
        }

        @Test
        @DisplayName("规范化匹配定位")
        void normalizedMatchLocate() throws Exception {
            XWPFDocument doc = createDocWithParagraphs("第一章", "招标 公告", "项目概况");
            WordTextExtractor.ExtractResult result = WordTextExtractor.extract(doc);

            LocationRefVO ref = WordTextExtractor.locate(result, "招标公告");
            assertNotNull(ref);
            assertEquals("paragraph", ref.getType());
            assertEquals(1, ref.getElementIndex());
        }

        @Test
        @DisplayName("匹配失败返回null")
        void noMatchReturnsNull() throws Exception {
            XWPFDocument doc = createDocWithParagraphs("第一章");
            WordTextExtractor.ExtractResult result = WordTextExtractor.extract(doc);

            LocationRefVO ref = WordTextExtractor.locate(result, "不存在的文本");
            assertNull(ref);
        }
    }
}
