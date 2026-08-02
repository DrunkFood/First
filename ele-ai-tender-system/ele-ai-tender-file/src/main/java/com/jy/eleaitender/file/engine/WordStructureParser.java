package com.jy.eleaitender.file.engine;

import com.jy.eleaitender.common.dto.response.WordStructureVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBookmark;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Word文档结构解析器
 * 解析Word文档中的章节、占位符、书签
 */
@Slf4j
@Component
public class WordStructureParser {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([\\w\\u4e00-\\u9fa5]+)}}");

    /**
     * 解析Word文档结构
     *
     * @param inputStream 文档输入流
     * @return 文档结构信息
     */
    public WordStructureVO parse(InputStream inputStream) {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            WordStructureVO structure = new WordStructureVO();
            List<WordStructureVO.Chapter> chapters = new ArrayList<>();
            Set<String> placeholders = new LinkedHashSet<>();
            Set<String> bookmarks = new LinkedHashSet<>();

            for (Object element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph para) {
                    extractFromParagraph(para, chapters, placeholders, bookmarks);
                } else if (element instanceof XWPFTable table) {
                    extractFromTable(table, placeholders, bookmarks);
                }
            }

            structure.setChapters(chapters);
            structure.setPlaceholders(new ArrayList<>(placeholders));
            structure.setBookmarks(new ArrayList<>(bookmarks));
            return structure;
        } catch (Exception e) {
            throw new RuntimeException("Word文档结构解析失败: " + e.getMessage(), e);
        }
    }

    private void extractFromParagraph(XWPFParagraph para,
                                      List<WordStructureVO.Chapter> chapters,
                                      Set<String> placeholders,
                                      Set<String> bookmarks) {
        // 提取章节（Heading样式）
        String style = para.getStyle();
        if (style != null && style.startsWith("Heading")) {
            try {
                int level = Integer.parseInt(style.substring("Heading".length()).trim());
                WordStructureVO.Chapter chapter = new WordStructureVO.Chapter();
                chapter.setLevel(level);
                chapter.setTitle(para.getText());
                // 提取章节内书签
                chapter.setBookmarks(extractBookmarksFromParagraph(para));
                chapters.add(chapter);
            } catch (NumberFormatException e) {
                log.warn("无法解析Heading级别: {}", style);
            }
        }

        // 提取占位符
        extractPlaceholders(para.getText(), placeholders);

        // 提取书签
        for (CTBookmark bm : para.getCTP().getBookmarkStartList()) {
            String name = bm.getName();
            if (name != null && !name.startsWith("_")) {
                bookmarks.add(name);
            }
        }
    }

    private void extractFromTable(XWPFTable table,
                                  Set<String> placeholders,
                                  Set<String> bookmarks) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                for (XWPFParagraph para : cell.getParagraphs()) {
                    extractPlaceholders(para.getText(), placeholders);
                    for (CTBookmark bm : para.getCTP().getBookmarkStartList()) {
                        String name = bm.getName();
                        if (name != null && !name.startsWith("_")) {
                            bookmarks.add(name);
                        }
                    }
                }
            }
        }
    }

    private void extractPlaceholders(String text, Set<String> placeholders) {
        if (text == null) {
            return;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        while (matcher.find()) {
            placeholders.add(matcher.group(1));
        }
    }

    private List<String> extractBookmarksFromParagraph(XWPFParagraph para) {
        List<String> result = new ArrayList<>();
        for (CTBookmark bm : para.getCTP().getBookmarkStartList()) {
            String name = bm.getName();
            if (name != null && !name.startsWith("_")) {
                result.add(name);
            }
        }
        return result;
    }
}
