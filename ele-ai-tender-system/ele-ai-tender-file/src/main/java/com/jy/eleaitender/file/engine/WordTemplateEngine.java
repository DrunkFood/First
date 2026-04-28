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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Word模板引擎
 * 使用poi-tl填充Word模板占位符，生成新文档
 */
@Slf4j
@Component
public class WordTemplateEngine {

    private static final String W_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final Pattern TEMPLATE_TAG_PATTERN = Pattern.compile("\\{\\{.*?}}");

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
            byte[] preprocessed = preprocessTemplate(templateStream);
            try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                 InputStream processedStream = new ByteArrayInputStream(preprocessed)) {
                XWPFTemplate template = XWPFTemplate.compile(processedStream, config).render(data);
                template.writeAndClose(out);
                return out.toByteArray();
            }
        } catch (Exception e) {
            log.error("Word模板填充失败", e);
            throw new RuntimeException("Word模板填充失败: " + e.getMessage(), e);
        }
    }

    /**
     * 模板预处理：清除修订标记 + 拆分合并的模板标签Run
     */
    private byte[] preprocessTemplate(InputStream templateStream) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(templateStream)) {
            Node bodyNode = doc.getDocument().getBody().getDomNode();
            acceptAllRevisions(bodyNode);
            splitMergedRuns(bodyNode);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 等效于Word"接受所有修订"：
     * 解包<w:ins>（保留子节点）、删除<w:del>（含子节点）、删除属性变更标记。
     * Word/WPS编辑模板时产生的修订标记会将<w:r>包裹在<w:ins>中，
     * 导致<w:r>不再是<w:p>的直接子元素，poi-tl的refactorRun合并Run时removeRun索引不同步。
     */
    private void acceptAllRevisions(Node bodyNode) {
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
    }

    /**
     * 拆分包含多个模板标签的合并Run。
     * Word/WPS编辑模板时，可能将相邻的 {{tag1}}{{tag2}} 合并到同一个 &lt;w:r&gt; 中，
     * 导致poi-tl无法正确识别标签边界。此方法将包含多个 {{...}} 的 Run 拆分为独立的 Run 元素。
     */
    private void splitMergedRuns(Node root) {
        List<Element> runs = collectElements(root, W_NS, "r");
        for (Element run : runs) {
            splitRunIfNeeded(run);
        }
    }

    private void splitRunIfNeeded(Element run) {
        Element textEl = getDirectChild(run, W_NS, "t");
        if (textEl == null) return;

        String text = textEl.getTextContent();
        if (text == null || !text.contains("{{")) return;

        List<String> segments = splitTemplateText(text);
        if (segments.size() <= 1) return;

        Node parent = run.getParentNode();
        Node nextSibling = run.getNextSibling();

        for (int i = segments.size() - 1; i >= 0; i--) {
            Element newRun = (Element) run.cloneNode(true);
            Element newT = getDirectChild(newRun, W_NS, "t");
            String segment = segments.get(i);
            newT.setTextContent(segment);
            if (segment.startsWith(" ") || segment.endsWith(" ") || segment.contains("{{")) {
                newT.setAttributeNS("http://www.w3.org/XML/1998/namespace", "xml:space", "preserve");
            }
            if (nextSibling != null) {
                parent.insertBefore(newRun, nextSibling);
            } else {
                parent.appendChild(newRun);
            }
        }

        parent.removeChild(run);
    }

    /**
     * 将包含多个 {{...}} 标签的文本拆分为独立片段，每个 {{...}} 标签和中间文本各为一段
     * 例: "{{?items}}{{name}} text" → ["{{?items}}", "{{name}}", " text"]
     */
    private List<String> splitTemplateText(String text) {
        List<String> segments = new ArrayList<>();
        Matcher matcher = TEMPLATE_TAG_PATTERN.matcher(text);
        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                segments.add(text.substring(lastEnd, matcher.start()));
            }
            segments.add(matcher.group());
            lastEnd = matcher.end();
        }
        if (lastEnd < text.length()) {
            segments.add(text.substring(lastEnd));
        }
        return segments;
    }

    private Element getDirectChild(Element parent, String nsUri, String localName) {
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child instanceof Element el) {
                if (nsUri.equals(el.getNamespaceURI()) && localName.equals(el.getLocalName())) {
                    return el;
                }
            }
            child = child.getNextSibling();
        }
        return null;
    }

    private List<Element> collectElements(Node root, String nsUri, String localName) {
        NodeList nodeList = ((Element) root).getElementsByTagNameNS(nsUri, localName);
        List<Element> result = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            result.add((Element) nodeList.item(i));
        }
        return result;
    }

    /** 解包：将元素的子节点移到元素前面，然后删除该元素 */
    private void unwrapElements(Node root, String nsUri, String localName) {
        List<Element> toProcess = collectElements(root, nsUri, localName);
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
        List<Element> toRemove = collectElements(root, nsUri, localName);
        for (Element el : toRemove) {
            el.getParentNode().removeChild(el);
        }
    }
}
