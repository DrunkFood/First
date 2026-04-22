package com.jy.eleaitender.file.engine;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Markdown模板引擎
 * 使用 flexmark-java 解析 Markdown 为 HTML，支持变量占位符替换
 */
@Slf4j
@Component
public class MarkdownTemplateEngine {

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownTemplateEngine() {
        MutableDataSet options = new MutableDataSet();
        this.parser = Parser.builder(options).build();
        this.renderer = HtmlRenderer.builder(options).build();
    }

    /**
     * 将Markdown转换为HTML
     */
    public String markdownToHtml(String markdown) {
        if (!StringUtils.hasText(markdown)) {
            return "";
        }
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }

    /**
     * 替换模板中的变量占位符 {{key}}
     */
    public String renderTemplate(String template, Map<String, Object> context) {
        if (!StringUtils.hasText(template) || context == null) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? String.valueOf(entry.getValue()) : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }

    /**
     * 根据Markdown模板和上下文生成HTML文档内容
     *
     * @param templateContent Markdown模板内容
     * @param context         变量上下文
     * @return 生成的HTML内容
     */
    public String generateHtml(String templateContent, Map<String, Object> context) {
        log.info("开始文档生成，模板长度: {}, 上下文参数数量: {}",
                templateContent != null ? templateContent.length() : 0,
                context != null ? context.size() : 0);

        // 1. 替换变量占位符
        String rendered = renderTemplate(templateContent, context);
        // 2. Markdown → HTML
        return markdownToHtml(rendered);
    }

    /**
     * 保留原接口兼容性
     */
    public String generateWord(String templateContent, Map<String, Object> context) {
        return generateHtml(templateContent, context);
    }
}
