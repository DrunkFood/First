package com.jy.eleaitender.file.engine;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

/**
 * Word模板引擎
 * 使用poi-tl填充Word模板占位符，生成新文档
 */
@Slf4j
@Component
public class WordTemplateEngine {

    /**
     * 渲染Word模板
     *
     * @param templateStream 模板文件输入流
     * @param data           填充数据
     * @return 生成文档的字节数组
     */
    public byte[] render(InputStream templateStream, Map<String, Object> data) {
        Configure config = Configure.builder().useSpringEL().build();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XWPFTemplate template = XWPFTemplate.compile(templateStream, config).render(data);
            template.writeAndClose(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Word模板填充失败", e);
            throw new RuntimeException("Word模板填充失败: " + e.getMessage(), e);
        }
    }
}
