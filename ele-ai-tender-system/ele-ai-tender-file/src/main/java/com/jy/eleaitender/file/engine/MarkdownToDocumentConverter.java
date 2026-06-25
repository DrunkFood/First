package com.jy.eleaitender.file.engine;

import com.deepoove.poi.data.*;
import com.deepoove.poi.data.style.Style;
import com.vladsch.flexmark.ast.*;
import com.vladsch.flexmark.ext.tables.*;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Markdown → DocumentRenderData 转换器
 * <p>
 * 使用 flexmark 解析 Markdown AST，遍历节点树，
 * 将标题、段落、加粗/斜体、列表、引用等元素映射为 poi-tl 的 DocumentRenderData 结构。
 * <p>
 * 核心转换映射：
 * <pre>
 *   Heading         → ParagraphRenderData (大号加粗字体)
 *   Paragraph       → ParagraphRenderData (含行内格式: TextRenderData + Style)
 *   BulletList      → NumberingRenderData (BULLET 格式)
 *   OrderedList     → NumberingRenderData (DECIMAL 格式)
 *   BlockQuote      → ParagraphRenderData (前缀 "> ")
 *   FencedCodeBlock → ParagraphRenderData (等宽字体)
 *   TableBlock      → TableRenderData (表头加粗，单元格保留行内格式)
 * </pre>
 */
@Slf4j
@Component
public class MarkdownToDocumentConverter {

    /**
     * 默认正文字号
     */
    private static final double BODY_FONT_SIZE = 10.5;
    /**
     * 默认字体
     */
    private static final String DEFAULT_FONT = "宋体";
    /**
     * 代码块字体
     */
    private static final String CODE_FONT = "Courier New";
    /**
     * 引用前缀
     */
    private static final String BLOCKQUOTE_PREFIX = "> ";

    /**
     * 各级标题字号映射 (h1=22pt, h2=18pt, h3=15pt, h4=13pt, h5=12pt, h6=11pt)
     */
    private static final double[] HEADING_FONT_SIZES = {22, 18, 15, 13, 12, 11};

    private final Parser parser;

    public MarkdownToDocumentConverter() {
        MutableDataSet options = new MutableDataSet();
        // 启用表格扩展，使 flexmark 识别 Markdown 表格语法 (| A | B |)
        options.set(Parser.EXTENSIONS, List.of(TablesExtension.create()));
        this.parser = Parser.builder(options).build();
    }

    /**
     * 将 Markdown 文本转换为 DocumentRenderData
     *
     * @param markdown Markdown 文本
     * @return DocumentRenderData 对象，可直接用于 DocumentRenderPolicy 渲染
     */
    public DocumentRenderData convert(String markdown) {
        if (!StringUtils.hasText(markdown)) {
            return new DocumentRenderData();
        }

        Node document = parser.parse(markdown);
        Documents.DocumentBuilder builder = Documents.of();

        for (Node child : document.getChildren()) {
            processBlockNode(child, builder);
        }

        return builder.create();
    }

    // ==================== 块级节点处理 ====================

    private void processBlockNode(Node node, Documents.DocumentBuilder builder) {
        switch (node) {
            // 标题
            case Heading heading -> builder.addParagraph(convertHeading(heading));
            // 段落
            case Paragraph paragraph -> builder.addParagraph(convertParagraph(paragraph));
            // 无序列表
            case BulletList bulletList -> builder.addNumbering(convertBulletList(bulletList));
            // 有序列表
            case OrderedList orderedList -> builder.addNumbering(convertOrderedList(orderedList));
            // 代码块
            case FencedCodeBlock codeBlock -> builder.addParagraph(convertFencedCodeBlock(codeBlock));
            // 引用
            case BlockQuote blockQuote -> processBlockQuote(blockQuote, builder);
            // 表格
            case TableBlock tableBlock -> convertTable(tableBlock, builder);
            // 其他块级节点：尝试提取纯文本作为段落
            default -> {
                String text = getNodeText(node);
                if (StringUtils.hasText(text)) {
                    builder.addParagraph(Paragraphs.of(text).create());
                }
            }
        }
    }

    // ==================== 标题 ====================

    private ParagraphRenderData convertHeading(Heading heading) {
        int level = heading.getLevel();
        double fontSize = getHeadingFontSize(level);

        // 收集行内文本片段
        List<TextRenderData> textParts = collectInlineText(heading, null, false);

        Paragraphs.ParagraphBuilder paraBuilder = Paragraphs.of();
        for (TextRenderData part : textParts) {
            // 标题统一加粗 + 对应字号
            applyHeadingStyle(part, fontSize);
            paraBuilder.addText(part);
        }
        return paraBuilder.create();
    }

    private double getHeadingFontSize(int level) {
        int idx = Math.clamp(level - 1, 0, HEADING_FONT_SIZES.length - 1);
        return HEADING_FONT_SIZES[idx];
    }

    private void applyHeadingStyle(TextRenderData textData, double fontSize) {
        Style style = textData.getStyle();
        if (style == null) {
            style = new Style(DEFAULT_FONT, fontSize);
            style.setBold(true);
            textData.setStyle(style);
        } else {
            style.setFontFamily(DEFAULT_FONT);
            style.setFontSize(fontSize);
            style.setBold(true);
        }
    }

    // ==================== 段落（含行内格式） ====================

    private ParagraphRenderData convertParagraph(Paragraph paragraph) {
        return convertParagraph(paragraph, null);
    }

    private ParagraphRenderData convertParagraph(Paragraph paragraph, String prefix) {
        List<TextRenderData> textParts = collectInlineText(paragraph, null, false);

        Paragraphs.ParagraphBuilder paraBuilder = Paragraphs.of();

        // 引用前缀
        if (prefix != null) {
            paraBuilder.addText(new TextRenderData(prefix));
        }

        for (TextRenderData part : textParts) {
            paraBuilder.addText(part);
        }
        return paraBuilder.create();
    }

    // ==================== 行内节点收集 ====================

    /**
     * 收集节点内所有行内文本，按格式拆分为多个 TextRenderData
     *
     * @param node    行内节点（Paragraph, Heading 等）
     * @param inherit 继承的样式（如父级加粗/斜体）
     * @param inCode  是否在代码环境中
     * @return 文本片段列表
     */
    private List<TextRenderData> collectInlineText(Node node, Style inherit, boolean inCode) {
        List<TextRenderData> parts = new ArrayList<>();

        for (Node child : node.getChildren()) {
            switch (child) {
                // 文本节点
                case Text textNode -> {
                    String content = textNode.getChars().toString();
                    parts.add(createTextData(content, inherit, inCode));
                }
                // **加粗**
                case StrongEmphasis strong -> {
                    Style boldStyle = mergeStyle(inherit, true, null, inCode);
                    parts.addAll(collectInlineText(strong, boldStyle, inCode));
                }
                // *斜体*
                case Emphasis em -> {
                    Style italicStyle = mergeStyle(inherit, null, true, inCode);
                    parts.addAll(collectInlineText(em, italicStyle, inCode));
                }
                // `行内代码`
                case Code code -> {
                    String content = getNodeText(code);
                    parts.add(createTextData(content, inherit, true));
                }
                // 软换行 → 空格
                case SoftLineBreak ignored -> parts.add(createTextData(" ", inherit, inCode));
                // 硬换行 → 换行符（Word 段落内换行）
                case HardLineBreak ignored -> parts.add(createTextData("\n", inherit, inCode));
                // 其他行内节点（如链接等），递归提取文本
                default -> {
                    String content = getNodeText(child);
                    if (StringUtils.hasText(content)) {
                        parts.add(createTextData(content, inherit, inCode));
                    }
                }
            }
        }

        // 空段落兜底：确保至少有一个空 TextRenderData，避免空段落丢失
        if (parts.isEmpty() && node instanceof Paragraph) {
            parts.add(createTextData("", inherit, inCode));
        }

        return parts;
    }

    private TextRenderData createTextData(String text, Style inherit, boolean inCode) {
        Style style;
        if (inCode) {
            style = new Style(CODE_FONT, BODY_FONT_SIZE);
        } else if (inherit != null) {
            style = new Style();
            copyStyle(inherit, style);
        } else {
            style = new Style(DEFAULT_FONT, BODY_FONT_SIZE);
        }
        return new TextRenderData(text, style);
    }

    private Style mergeStyle(Style inherit, Boolean bold, Boolean italic, boolean inCode) {
        Style style = new Style();
        if (inherit != null) {
            copyStyle(inherit, style);
        }
        if (bold != null && bold) {
            style.setBold(true);
        }
        if (italic != null && italic) {
            style.setItalic(true);
        }
        if (inCode) {
            style.setFontFamily(CODE_FONT);
        }
        if (style.getFontFamily() == null) {
            style.setFontFamily(DEFAULT_FONT);
        }
        if (style.getFontSize() == 0) {
            style.setFontSize(BODY_FONT_SIZE);
        }
        return style;
    }

    private void copyStyle(Style src, Style dst) {
        if (src.getFontFamily() != null) dst.setFontFamily(src.getFontFamily());
        if (src.getFontSize() != 0) dst.setFontSize(src.getFontSize());
        if (src.isBold() != null) dst.setBold(src.isBold());
        if (src.isItalic() != null) dst.setItalic(src.isItalic());
        if (src.getColor() != null) dst.setColor(src.getColor());
    }

    // ==================== 列表 ====================

    private NumberingRenderData convertBulletList(BulletList bulletList) {
        Numberings.NumberingBuilder builder = Numberings.ofBullet();
        for (Node child : bulletList.getChildren()) {
            if (child instanceof ListItem listItem) {
                builder.addItem(convertListItem(listItem));
            }
        }
        return builder.create();
    }

    private NumberingRenderData convertOrderedList(OrderedList orderedList) {
        Numberings.NumberingBuilder builder = Numberings.ofDecimal();
        for (Node child : orderedList.getChildren()) {
            if (child instanceof ListItem listItem) {
                builder.addItem(convertListItem(listItem));
            }
        }
        return builder.create();
    }

    private ParagraphRenderData convertListItem(ListItem listItem) {
        List<TextRenderData> parts = new ArrayList<>();

        for (Node child : listItem.getChildren()) {
            if (child instanceof Paragraph para) {
                parts.addAll(collectInlineText(para, null, false));
            } else {
                // 列表项中的非段落内容（如嵌套列表已在块级处理），提取文本
                String text = getNodeText(child);
                if (StringUtils.hasText(text)) {
                    parts.add(new TextRenderData(text, new Style(DEFAULT_FONT, BODY_FONT_SIZE)));
                }
            }
        }

        Paragraphs.ParagraphBuilder paraBuilder = Paragraphs.of();
        for (TextRenderData part : parts) {
            paraBuilder.addText(part);
        }
        return paraBuilder.create();
    }

    // ==================== 引用 ====================

    private void processBlockQuote(BlockQuote blockQuote, Documents.DocumentBuilder builder) {
        for (Node child : blockQuote.getChildren()) {
            if (child instanceof Paragraph para) {
                builder.addParagraph(convertParagraph(para, BLOCKQUOTE_PREFIX));
            } else if (child instanceof Heading heading) {
                // 引用内的标题也加前缀
                ParagraphRenderData headingPara = convertHeading(heading);
                // 在标题段落的行内文本前插入引用前缀
                List<TextRenderData> contents = headingPara.getContents() != null
                        ? new ArrayList<>(headingPara.getContents().stream()
                        .filter(r -> r instanceof TextRenderData)
                        .map(r -> (TextRenderData) r)
                        .toList())
                        : new ArrayList<>();
                Paragraphs.ParagraphBuilder pb = Paragraphs.of();
                pb.addText(new TextRenderData(BLOCKQUOTE_PREFIX));
                for (TextRenderData td : contents) {
                    pb.addText(td);
                }
                builder.addParagraph(pb.create());
            } else {
                // 其他块级内容提取纯文本
                String text = getNodeText(child);
                if (StringUtils.hasText(text)) {
                    builder.addParagraph(Paragraphs.of(BLOCKQUOTE_PREFIX + text).create());
                }
            }
        }
    }

    // ==================== 代码块 ====================

    private ParagraphRenderData convertFencedCodeBlock(FencedCodeBlock codeBlock) {
        // FencedCodeBlock 的 getChars() 包含围栏标记行，需要去掉首尾的围栏
        String raw = codeBlock.getChars().toString();
        String content = stripFencedCodeBlock(raw);

        Style codeStyle = new Style(CODE_FONT, BODY_FONT_SIZE);
        Paragraphs.ParagraphBuilder paraBuilder = Paragraphs.of();
        paraBuilder.addText(new TextRenderData(content, codeStyle));
        return paraBuilder.create();
    }

    /**
     * 去掉 FencedCodeBlock 的围栏标记行（首行 ``` 和末行 ```）
     */
    private String stripFencedCodeBlock(String raw) {
        String[] lines = raw.split("\n");
        if (lines.length <= 2) {
            return "";
        }
        // 去掉首行（围栏开始）和末行（围栏结束），保留中间内容
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < lines.length - 1; i++) {
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append(lines[i]);
        }
        return sb.toString();
    }

    // ==================== 表格 ====================

    /**
     * 将 Markdown 表格转换为 poi-tl TableRenderData。
     * <p>
     * 按结构遍历 TableBlock → TableHead/TableBody → TableRow，仅取直接子行。
     * 显式跳过 |---| 分隔行：flexmark 中 TableSeparator 是 TableBlock 的直接子节点，
     * 其内部还嵌套了内容为 "---" 的 TableRow，需整个跳过避免被当成数据行。
     * TableHead 内的行视为表头并加粗，单元格内行内格式（加粗/斜体/代码）通过 collectInlineText 保留。
     */
    private void convertTable(TableBlock tableBlock, Documents.DocumentBuilder builder) {
        Tables.TableBuilder tableBuilder = Tables.of();
        for (Node section : tableBlock.getChildren()) {
            // 跳过分隔行节点（|---|）：flexmark 中 TableSeparator 是 TableBlock 的直接子节点，
            // 其内部还嵌套了内容为 "---" 的 TableRow，必须整个跳过，否则会把分隔行渲染成数据行。
            if (section instanceof TableSeparator) {
                continue;
            }
            boolean headerSection = section instanceof TableHead;
            for (Node rowNode : section.getChildren()) {
                if (!(rowNode instanceof TableRow row)) {
                    continue;
                }
                Rows.RowBuilder rowBuilder = Rows.of();
                for (Node cellNode : row.getChildren()) {
                    if (!(cellNode instanceof TableCell cell)) {
                        continue;
                    }
                    rowBuilder.addCell(buildTableCell(cell));
                }
                if (headerSection) {
                    rowBuilder.textBold();
                }
                tableBuilder.addRow(rowBuilder.create());
            }
        }
        builder.addTable(tableBuilder.create());
    }

    /**
     * 构建表格单元格：将单元格内行内文本片段组装为单段落，再包成 CellRenderData。
     * 一个单元格内的多个片段（如 "加粗 普通"）合并到同一单元格，避免被错拆为多列。
     */
    private CellRenderData buildTableCell(TableCell cell) {
        List<TextRenderData> parts = collectInlineText(cell, null, false);
        if (parts.isEmpty()) {
            parts.add(createTextData("", null, false));
        }
        Paragraphs.ParagraphBuilder paraBuilder = Paragraphs.of();
        for (TextRenderData part : parts) {
            paraBuilder.addText(part);
        }
        return Cells.of().addParagraph(paraBuilder.create()).create();
    }

    // ==================== 通用工具 ====================

    /**
     * 递归获取节点的纯文本内容
     */
    private String getNodeText(Node node) {
        StringBuilder sb = new StringBuilder();
        for (Node child : node.getChildren()) {
            if (child instanceof Text textNode) {
                sb.append(textNode.getChars());
            } else {
                sb.append(getNodeText(child));
            }
        }
        return sb.toString();
    }
}
