package com.jy.eleaitender.file.engine;

import com.jy.eleaitender.common.dto.ColumnDef;
import com.jy.eleaitender.common.dto.TableData;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TableGeneratorTest {

    private final TableGenerator tableGenerator = new TableGenerator();

    @Test
    void replaceTablePlaceholderWithLeadingWhitespace() throws Exception {
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph paragraph = doc.createParagraph();
            paragraph.createRun().setText("  " + TableGenerator.TABLE_PLACEHOLDER_PREFIX + "allReviewItems");

            TableData tableData = new TableData();
            tableData.setColumns(List.of(
                    new ColumnDef("categoryName", "类别"),
                    new ColumnDef("reviewStandard", "评审标准")
            ));
            tableData.setRows(List.of(Map.of(
                    "categoryName", "技术标",
                    "reviewStandard", "技术方案"
            )));

            tableGenerator.replaceTablePlaceholders(doc, Map.of("allReviewItems", tableData));

            assertEquals(1, doc.getTables().size());
            XWPFTable table = doc.getTables().get(0);
            assertEquals("类别", table.getRow(0).getCell(0).getText());
            assertEquals("技术方案", table.getRow(1).getCell(1).getText());
            assertFalse(doc.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .anyMatch(text -> text.contains(TableGenerator.TABLE_PLACEHOLDER_PREFIX)));
        }
    }
}
