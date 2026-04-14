package com.jy.eleaitender.core.engine;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.data.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Word文档生成器
 * 使用 poi-tl 将数据填充到 Word 模板，生成 .docx 文件
 */
@Slf4j
@Component
public class WordDocumentGenerator {

    @Autowired
    private MarkdownTemplateEngine markdownTemplateEngine;

    /**
     * 根据数据模型生成Word文档字节数组
     *
     * @param documentData 文档数据（由DocumentDataAssembler组装）
     * @param htmlContent  已生成的HTML内容（用于嵌入文档正文）
     * @return .docx 文件字节数组
     */
    public byte[] generate(Map<String, Object> documentData, String htmlContent) {
        log.info("开始生成Word文档，数据字段数: {}", documentData.size());

        // 构建 poi-tl 数据模型
        Map<String, Object> model = new HashMap<>(documentData);
        model.put("htmlContent", htmlContent);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // 使用内存中动态构建的简易模板
            XWPFTemplate template = XWPFTemplate.compile(buildDefaultTemplate()).render(model);
            template.writeAndClose(out);
            byte[] result = out.toByteArray();
            log.info("Word文档生成完成，文件大小: {} bytes", result.length);
            return result;
        } catch (IOException e) {
            log.error("Word文档生成失败", e);
            throw new RuntimeException("Word文档生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建默认Word模板的输入流
     * 在没有自定义模板文件时，使用内置的简易模板
     */
    private java.io.InputStream buildDefaultTemplate() {
        // 使用 poi-tl 提供的简易文本模板
        // 实际生产环境应使用文件系统中的 .docx 模板文件
        String templatePath = "/templates/tender-document-template.docx";
        java.io.InputStream is = getClass().getResourceAsStream(templatePath);
        if (is != null) {
            return is;
        }
        // 如果模板不存在，创建一个最小化的空白 docx 作为回退
        log.warn("未找到Word模板文件: {}，使用空白模板", templatePath);
        return createMinimalTemplate();
    }

    /**
     * 创建最小化空白Word模板
     */
    private java.io.InputStream createMinimalTemplate() {
        try {
            org.apache.poi.xwpf.usermodel.XWPFDocument doc = new org.apache.poi.xwpf.usermodel.XWPFDocument();
            // 添加标题占位符
            org.apache.poi.xwpf.usermodel.XWPFParagraph title = doc.createParagraph();
            title.createRun().setText("{{projectName}}");
            // 添加正文占位符
            org.apache.poi.xwpf.usermodel.XWPFParagraph body = doc.createParagraph();
            body.createRun().setText("{{htmlContent}}");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            doc.close();
            return new java.io.ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("创建空白Word模板失败", e);
        }
    }
}
