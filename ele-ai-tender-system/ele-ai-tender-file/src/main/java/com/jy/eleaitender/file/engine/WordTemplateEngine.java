package com.jy.eleaitender.file.engine;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Word模板引擎
 * 使用poi-tl填充Word模板占位符，生成新文档
 */
@Slf4j
@Component
public class WordTemplateEngine {

    private static final String W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

    /**
     * 渲染Word模板
     *
     * @param templateStream 模板文件输入流
     * @param data           填充数据
     * @return 生成文档的字节数组
     */
    public byte[] render(InputStream templateStream, Map<String, Object> data) {
        Configure config = Configure.builder().build();
        try {
            // 先清除修订标记，防止poi-tl因Run被<w:ins>包裹而抛IndexOutOfBoundsException
            byte[] cleaned = acceptAllRevisions(templateStream);
            try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                 InputStream cleanStream = new ByteArrayInputStream(cleaned)) {
                XWPFTemplate template = XWPFTemplate.compile(cleanStream, config).render(data);
                template.writeAndClose(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            log.error("Word模板填充失败", e);
            throw new RuntimeException("Word模板填充失败: " + e.getMessage(), e);
        }
    }

    /**
     * 等效于Word"接受所有修订"：
     * 解包<w:ins>（保留子节点）、删除<w:del>（含子节点）、删除属性变更标记。
     * Word/WPS编辑模板时产生的修订标记会将<w:r>包裹在<w:ins>中，
     * 导致<w:r>不再是<w:p>的直接子元素，poi-tl的refactorRun合并Run时removeRun索引不同步。
     */
    private byte[] acceptAllRevisions(InputStream templateStream) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(templateStream)) {
            Node bodyNode = doc.getDocument().getBody().getDomNode();
            unwrapElements(bodyNode, W_NS, "ins");
            unwrapElements(bodyNode, W_NS, "moveTo");
            removeElements(bodyNode, W_NS, "del");
            removeElements(bodyNode, W_NS, "moveFrom");
            removeElements(bodyNode, W_NS, "rPrChange");
            removeElements(bodyNode, W_NS, "pPrChange");
            removeElements(bodyNode, W_NS, "sectPrChange");
            removeElements(bodyNode, W_NS, "tblPrChange");
            removeElements(bodyNode, W_NS, "tcPrChange");
            removeElements(bodyNode, W_NS, "trPrChange");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();
        }
    }

    /** 解包：将元素的子节点移到元素前面，然后删除该元素 */
    private void unwrapElements(Node root, String nsUri, String localName) {
        NodeList elements = ((Element) root).getElementsByTagNameNS(nsUri, localName);
        List<Element> toProcess = new ArrayList<>();
        for (int i = 0; i < elements.getLength(); i++) {
            toProcess.add((Element) elements.item(i));
        }
        // 反序处理避免后续元素位置偏移
        for (int i = toProcess.size() - 1; i >= 0; i--) {
            Element el = toProcess.get(i);
            Node parent = el.getParentNode();
            while (el.hasChildNodes()) {
                parent.insertBefore(el.getFirstChild(), el);
            }
            parent.removeChild(el);
        }
    }

    /** 删除：连同子节点一起移除 */
    private void removeElements(Node root, String nsUri, String localName) {
        NodeList elements = ((Element) root).getElementsByTagNameNS(nsUri, localName);
        List<Element> toRemove = new ArrayList<>();
        for (int i = 0; i < elements.getLength(); i++) {
            toRemove.add((Element) elements.item(i));
        }
        for (Element el : toRemove) {
            el.getParentNode().removeChild(el);
        }
    }
}
