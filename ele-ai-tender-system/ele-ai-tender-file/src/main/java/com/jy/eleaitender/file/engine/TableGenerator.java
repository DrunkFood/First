package com.jy.eleaitender.file.engine;

import com.jy.eleaitender.common.dto.ColumnDef;
import com.jy.eleaitender.common.dto.MergeRule;
import com.jy.eleaitender.common.dto.MergeStrategy;
import com.jy.eleaitender.common.dto.TableData;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.math.BigInteger;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * POI 编程生成表格
 * 绕过 poi-tl 的循环标签，直接用 Apache POI API 创建表格行并处理单元格合并
 */
@Slf4j
@Component
public class TableGenerator {

    public static final String TABLE_PLACEHOLDER_PREFIX = "__TABLE_PLACEHOLDER__";

    /**
     * 扫描文档中的表格占位符段落并替换为生成的表格
     */
    public void replaceTablePlaceholders(XWPFDocument doc, Map<String, TableData> tableDataMap) {
        if (tableDataMap == null || tableDataMap.isEmpty()) return;

        List<IBodyElement> bodyElements = doc.getBodyElements();
        // 从后往前遍历，避免删除导致索引偏移
        for (int i = bodyElements.size() - 1; i >= 0; i--) {
            IBodyElement element = bodyElements.get(i);
            if (!(element instanceof XWPFParagraph para)) continue;

            String text = para.getText();
            String key = findTablePlaceholderKey(text, tableDataMap);
            if (key != null) {
                TableData tableData = tableDataMap.get(key);
                if (tableData != null) {
                    insertTableAtParagraph(doc, para, tableData);
                    log.info("表格占位符替换完成: key={}, 行数={}", key,
                            tableData.getRows() != null ? tableData.getRows().size() : 0);
                } else {
                    log.warn("未找到表格数据: key={}", key);
                }
            }
        }
    }

    private String findTablePlaceholderKey(String text, Map<String, TableData> tableDataMap) {
        if (text == null || text.isBlank()) {
            return null;
        }
        for (String key : tableDataMap.keySet()) {
            if (text.contains(TABLE_PLACEHOLDER_PREFIX + key)) {
                return key;
            }
        }

        String trimmed = text.trim();
        if (trimmed.startsWith(TABLE_PLACEHOLDER_PREFIX)) {
            return trimmed.substring(TABLE_PLACEHOLDER_PREFIX.length()).trim();
        }
        return null;
    }

    /**
     * 在指定段落位置插入表格（替换该段落）
     */
    private void insertTableAtParagraph(XWPFDocument doc, XWPFParagraph placeholder, TableData tableData) {
        List<ColumnDef> columns = tableData.getColumns();
        if (columns == null || columns.isEmpty()) {
            log.warn("表格列定义为空，跳过生成");
            doc.removeBodyElement(doc.getPosOfParagraph(placeholder));
            return;
        }

        int colCount = columns.size();

        // 用 XmlCursor 在占位段落前定位，插入新表格
        XmlCursor cursor = placeholder.getCTP().newCursor();
        XWPFTable table;
        try {
            table = doc.insertNewTbl(cursor);
        } finally {
            cursor.dispose();
        }

        // 确保表格有足够的列
        XWPFTableRow firstRow = table.getRow(0);
        while (firstRow.getTableCells().size() < colCount) {
            firstRow.addNewTableCell();
        }

        // 生成表头行
        createHeaderRow(table, columns, colCount);

        // 生成数据行
        List<Map<String, String>> rows = tableData.getRows();
        if (rows != null && !rows.isEmpty()) {
            createDataRows(table, tableData);
        }

        // 应用默认样式
        applyDefaultStyle(table);

        // 处理单元格合并
        applyMergeRules(table, tableData.getMergeRules());

        // 删除原占位段落
        doc.removeBodyElement(doc.getPosOfParagraph(placeholder));
    }

    private void createHeaderRow(XWPFTable table, List<ColumnDef> columns, int colCount) {
        XWPFTableRow headerRow = table.getRow(0);
        while (headerRow.getTableCells().size() < colCount) {
            headerRow.addNewTableCell();
        }
        for (int i = 0; i < colCount; i++) {
            XWPFTableCell cell = headerRow.getCell(i);
            setCellText(cell, columns.get(i).getHeader());
            setBold(cell, true);
        }
    }

    private void createDataRows(XWPFTable table, TableData tableData) {
        List<Map<String, String>> rows = tableData.getRows();
        List<ColumnDef> columns = tableData.getColumns();

        for (Map<String, String> rowData : rows) {
            XWPFTableRow row = table.createRow();
            for (int colIdx = 0; colIdx < columns.size(); colIdx++) {
                String cellText = rowData.getOrDefault(columns.get(colIdx).getKey(), "");
                XWPFTableCell cell = row.getCell(colIdx);
                setCellText(cell, cellText);
            }
        }
    }

    // ==================== 单元格合并 ====================

    private void applyMergeRules(XWPFTable table, List<MergeRule> mergeRules) {
        if (mergeRules == null || mergeRules.isEmpty()) return;

        List<XWPFTableRow> rows = table.getRows();
        if (rows.size() <= 1) return;

        for (MergeRule rule : mergeRules) {
            if (rule.getStrategy() == MergeStrategy.BY_SAME_TEXT) {
                mergeColumnBySameText(table, rule.getColumnIndex());
            }
        }
    }

    private void mergeColumnBySameText(XWPFTable table, int colIdx) {
        List<XWPFTableRow> rows = table.getRows();
        if (rows.size() <= 2) return;

        int startRow = 1;
        String currentText = getCellText(rows.get(startRow).getCell(colIdx));

        for (int i = startRow + 1; i <= rows.size(); i++) {
            String nextText = (i < rows.size()) ? getCellText(rows.get(i).getCell(colIdx)) : null;

            if (i == rows.size() || !currentText.equals(nextText)) {
                if (i - 1 > startRow) {
                    setCellMerge(rows.get(startRow).getCell(colIdx), STMerge.RESTART);
                    for (int j = startRow + 1; j < i; j++) {
                        setCellMerge(rows.get(j).getCell(colIdx), STMerge.CONTINUE);
                        clearCellText(rows.get(j).getCell(colIdx));
                    }
                }
                if (i < rows.size()) {
                    startRow = i;
                    currentText = nextText;
                }
            }
        }
    }

    private void setCellMerge(XWPFTableCell cell, STMerge.Enum mergeType) {
        CTTcPr tcPr = cell.getCTTc().getTcPr();
        if (tcPr == null) tcPr = cell.getCTTc().addNewTcPr();
        CTVMerge vMerge = tcPr.isSetVMerge() ? tcPr.getVMerge() : tcPr.addNewVMerge();
        vMerge.setVal(mergeType);
    }

    private void clearCellText(XWPFTableCell cell) {
        for (int i = cell.getParagraphs().size() - 1; i >= 0; i--) {
            cell.removeParagraph(i);
        }
        cell.addParagraph();
    }

    // ==================== 单元格文本与样式 ====================

    private void setCellText(XWPFTableCell cell, String text) {
        if (cell.getParagraphs().isEmpty()) {
            cell.addParagraph();
        }
        XWPFParagraph para = cell.getParagraphs().get(0);
        if (para.getRuns().isEmpty()) {
            para.createRun();
        }
        para.getRuns().get(0).setText(text != null ? text : "", 0);
    }

    private void setBold(XWPFTableCell cell, boolean bold) {
        for (XWPFParagraph para : cell.getParagraphs()) {
            for (XWPFRun run : para.getRuns()) {
                run.setBold(bold);
            }
        }
    }

    private String getCellText(XWPFTableCell cell) {
        StringBuilder sb = new StringBuilder();
        for (XWPFParagraph para : cell.getParagraphs()) {
            for (XWPFRun run : para.getRuns()) {
                String t = run.getText(0);
                if (t != null) sb.append(t);
            }
        }
        return sb.toString().trim();
    }

    // ==================== 默认样式 ====================

    private void applyDefaultStyle(XWPFTable table) {
        table.setWidth("100%");
        applyTableBorders(table);

        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);

                for (XWPFParagraph para : cell.getParagraphs()) {
                    para.setAlignment(ParagraphAlignment.LEFT);
                    para.setSpacingBefore(0);
                    para.setSpacingAfter(0);

                    for (XWPFRun run : para.getRuns()) {
                        run.setFontSize(10);
                        run.setFontFamily("宋体");
                    }
                }
            }
        }

        // 表头行居中
        if (!table.getRows().isEmpty()) {
            XWPFTableRow headerRow = table.getRow(0);
            for (XWPFTableCell cell : headerRow.getTableCells()) {
                for (XWPFParagraph para : cell.getParagraphs()) {
                    para.setAlignment(ParagraphAlignment.CENTER);
                }
            }
        }
    }

    private void applyTableBorders(XWPFTable table) {
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        if (tblPr == null) tblPr = table.getCTTbl().addNewTblPr();

        CTTblBorders borders = tblPr.isSetTblBorders() ? tblPr.getTblBorders() : tblPr.addNewTblBorders();

        setBorder(borders.isSetTop() ? borders.getTop() : borders.addNewTop());
        setBorder(borders.isSetBottom() ? borders.getBottom() : borders.addNewBottom());
        setBorder(borders.isSetLeft() ? borders.getLeft() : borders.addNewLeft());
        setBorder(borders.isSetRight() ? borders.getRight() : borders.addNewRight());
        setBorder(borders.isSetInsideH() ? borders.getInsideH() : borders.addNewInsideH());
        setBorder(borders.isSetInsideV() ? borders.getInsideV() : borders.addNewInsideV());
    }

    private void setBorder(CTBorder border) {
        border.setVal(STBorder.SINGLE);
        border.setSz(BigInteger.valueOf(4));
        border.setSpace(BigInteger.ZERO);
        border.setColor("auto");
    }
}
