package com.jy.eleaitender.file.service.impl;

import com.deepoove.poi.data.Pictures;
import com.jy.eleaitender.common.dto.FillData;
import com.jy.eleaitender.common.dto.FixReplacement;
import com.jy.eleaitender.common.dto.ImageData;
import com.jy.eleaitender.common.dto.TableData;
import com.jy.eleaitender.common.dto.response.FileUploadResponse;
import com.jy.eleaitender.common.dto.response.WordFixResultVO;
import com.jy.eleaitender.common.dto.response.WordStructureVO;
import com.jy.eleaitender.file.engine.*;
import com.jy.eleaitender.file.service.IFileStorageService;
import com.jy.eleaitender.file.service.IWordDocumentService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlCursor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Word文档服务实现
 * 两阶段渲染：poi-tl 渲染文本/图片 → POI 编程生成表格
 */
@Slf4j
@Service
public class WordDocumentServiceImpl implements IWordDocumentService {

    private static final String AI_DISCLAIMER_TITLE = "【AI辅助生成·仅供参考】";
    private static final String AI_DISCLAIMER_CONTENT = "本文档由AI工具辅助生成，仅供使用者参考、编辑与格式借鉴，不构成我们提供的任何形式的专业法律、技术或商业建议，不构成可直接提交的最终招标文件，亦不代表我们对招标项目内容、数据的任何承诺、审查或保证。使用者必须结合具体项目需求、法律法规及招标文件要求，对本文档的全部内容进行独立审查、修正和核实，并自行承担使用本文档产生的全部风险与责任。因未履行上述审核义务而直接使用本文档所造成的任何损失，我们均不承担任何责任。";
    private static final String DISCLAIMER_BACKGROUND_COLOR = "FFFF00";

    @Autowired
    private IFileStorageService fileStorageService;

    @Autowired
    private WordStructureParser structureParser;

    @Autowired
    private WordTemplateEngine templateEngine;

    @Autowired
    private WordDocumentFixEngine fixEngine;

    @Autowired
    private TableGenerator tableGenerator;

    @Autowired
    private MarkdownToDocumentConverter markdownConverter;

    @Override
    public WordStructureVO getFileStructure(Long fileId) {
        String filePath = fileStorageService.getFilePath(fileId);
        try (InputStream is = new FileInputStream(filePath)) {
            return structureParser.parse(is);
        } catch (Exception e) {
            throw new RuntimeException("获取Word文档结构失败: " + e.getMessage(), e);
        }
    }

    /**
     * 基于模板和 FillData 列表生成文档（新接口）
     */
    @Override
    public Long generateDocument(Long templateFileId, List<Map<String, Object>> fillDataRaw, String fileName) {
        List<FillData> fillDataList = deserializeFillDataList(fillDataRaw);
        return generateWithFillData(templateFileId, fillDataList, fileName);
    }

    /**
     * 两阶段渲染：poi-tl 渲染文本/图片 → POI 编程生成表格
     */
    private Long generateWithFillData(Long templateFileId, List<FillData> fillDataList, String fileName) {
        String templatePath = fileStorageService.getFilePath(templateFileId);
        try (InputStream templateStream = new FileInputStream(templatePath)) {

            // ====== 阶段一：分离数据 → poi-tl 渲染文本和图片 ======

            Map<String, Object> poiData = new LinkedHashMap<>();
            Map<String, TableData> tableDataMap = new LinkedHashMap<>();
            Set<String> markdownKeys = new HashSet<>();

            for (FillData fd : fillDataList) {
                switch (fd.getType()) {
                    case TEXT -> poiData.put(fd.getKey(), fd.getValue());
                    case IMAGE -> poiData.put(fd.getKey(), toPictureRenderData((ImageData) fd.getValue()));
                    case MARKDOWN -> {
                        poiData.put(fd.getKey(), markdownConverter.convert((String) fd.getValue()));
                        markdownKeys.add(fd.getKey());
                    }
                    case TABLE -> {
                        poiData.put(fd.getKey(), TableGenerator.TABLE_PLACEHOLDER_PREFIX + fd.getKey());
                        tableDataMap.put(fd.getKey(), (TableData) fd.getValue());
                    }
                }
            }

            // poi-tl 渲染（文本+图片+TABLE标记文本）
            byte[] rendered = templateEngine.render(templateStream, poiData, markdownKeys);

            // ====== 阶段二：POI 编程生成表格 ======

            try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(rendered))) {
                addAiDisclaimerToTop(doc);
                if (!tableDataMap.isEmpty()) {
                    tableGenerator.replaceTablePlaceholders(doc, tableDataMap);
                }
                byte[] finalBytes = writeDocument(doc);
                return uploadGeneratedFile(finalBytes, fileName);
            }
        } catch (Exception e) {
            throw new RuntimeException("基于模板生成文档失败: " + e.getMessage(), e);
        }
    }

    // ==================== 辅助方法 ====================

    private Object toPictureRenderData(ImageData imageData) {
        if (imageData.getBase64() != null) {
            byte[] bytes = Base64.getDecoder().decode(imageData.getBase64());
            if (imageData.getWidth() > 0 && imageData.getHeight() > 0) {
                return Pictures.ofBytes(bytes).size(imageData.getWidth(), imageData.getHeight()).create();
            }
            return Pictures.ofBytes(bytes).create();
        }
        // URL 模式暂不支持，返回占位文本
        log.warn("图片URL模式暂不支持: {}", imageData.getUrl());
        return "[图片]";
    }

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    private List<FillData> deserializeFillDataList(List<Map<String, Object>> rawList) {
        if (rawList == null) return List.of();
        return rawList.stream().map(raw -> {
            try {
                FillData fd = MAPPER.convertValue(raw, FillData.class);
                // JSON反序列化后 value 是 LinkedHashMap，需按 type 转为具体类型
                if (fd.getValue() != null && fd.getType() != null) {
                    fd.setValue(switch (fd.getType()) {
                        case TABLE -> MAPPER.convertValue(fd.getValue(), TableData.class);
                        case IMAGE -> MAPPER.convertValue(fd.getValue(), ImageData.class);
                        case TEXT, MARKDOWN -> fd.getValue() instanceof String s ? s : String.valueOf(fd.getValue());
                    });
                }
                return fd;
            } catch (Exception e) {
                log.warn("FillData 反序列化失败: {}", raw, e);
                return null;
            }
        }).filter(Objects::nonNull).toList();
    }

    private byte[] writeDocument(XWPFDocument doc) throws Exception {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        doc.write(out);
        return out.toByteArray();
    }

    private void addAiDisclaimerToTop(XWPFDocument doc) {
        XWPFParagraph contentParagraph = insertParagraphAtDocumentStart(doc);
        applyDisclaimerParagraphStyle(contentParagraph, 200);
        XWPFRun contentRun = contentParagraph.createRun();
        contentRun.setText(AI_DISCLAIMER_CONTENT);
        contentRun.setFontSize(12);
        contentRun.setFontFamily("宋体");

        XWPFParagraph titleParagraph = insertParagraphAtDocumentStart(doc);
        applyDisclaimerParagraphStyle(titleParagraph, 0);
        XWPFRun titleRun = titleParagraph.createRun();
        titleRun.setText(AI_DISCLAIMER_TITLE);
        titleRun.setBold(true);
        titleRun.setFontSize(14);
        titleRun.setFontFamily("宋体");
    }

    private XWPFParagraph insertParagraphAtDocumentStart(XWPFDocument doc) {
        List<IBodyElement> bodyElements = doc.getBodyElements();
        if (bodyElements.isEmpty()) {
            return doc.createParagraph();
        }

        IBodyElement firstElement = bodyElements.get(0);
        XmlCursor cursor = null;
        if (firstElement instanceof XWPFParagraph paragraph) {
            cursor = paragraph.getCTP().newCursor();
        } else if (firstElement instanceof XWPFTable table) {
            cursor = table.getCTTbl().newCursor();
        }

        if (cursor == null) {
            return doc.createParagraph();
        }

        try {
            return doc.insertNewParagraph(cursor);
        } finally {
            cursor.dispose();
        }
    }

    private void applyDisclaimerParagraphStyle(XWPFParagraph paragraph, int spacingAfter) {
        paragraph.setSpacingBefore(0);
        paragraph.setSpacingAfter(spacingAfter);
        paragraph.setIndentationLeft(0);
        setParagraphShading(paragraph, DISCLAIMER_BACKGROUND_COLOR);
    }

    private void setParagraphShading(XWPFParagraph paragraph, String colorHex) {
        try {
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr paragraphProperties =
                    paragraph.getCTP().isSetPPr() ? paragraph.getCTP().getPPr() : paragraph.getCTP().addNewPPr();
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd shading =
                    paragraphProperties.isSetShd() ? paragraphProperties.getShd() : paragraphProperties.addNewShd();
            shading.setFill(colorHex);
        } catch (Exception e) {
            log.debug("设置免责声明背景色失败（可忽略）", e);
        }
    }

    private Long uploadGeneratedFile(byte[] docBytes, String fileName) {
        FileUploadResponse response = fileStorageService.uploadFromBytes(
                new ByteArrayInputStream(docBytes), fileName, "generated");
        return response.getFileId();
    }

    @Override
    public WordFixResultVO fixDocument(Long fileId, List<FixReplacement> replacements) {
        String filePath = fileStorageService.getFilePath(fileId);
        try {
            byte[] docxBytes = Files.readAllBytes(Paths.get(filePath));
            WordDocumentFixEngine.FixResult result = fixEngine.fix(docxBytes, replacements);

            // 生成文件名：保留原始文件名，去除已有的 fixed_时间戳_ 前缀后重新拼接
            String originalName = fileStorageService.getById(fileId).getFileName();
            String baseName = originalName.replaceFirst("^fixed_\\d+_", "");
            String generatedName = "fixed_" + System.currentTimeMillis() + "_" + baseName;
            FileUploadResponse uploadResp = fileStorageService.uploadFromBytes(
                    new ByteArrayInputStream(result.getDocumentBytes()), generatedName, "detection-fixed");

            WordFixResultVO vo = new WordFixResultVO();
            vo.setFileId(uploadResp.getFileId());
            vo.setFixedCount(result.getFixedCount());
            vo.setFailedCount(result.getFailedCount());
            return vo;
        } catch (Exception e) {
            throw new RuntimeException("修复Word文档失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> extractText(Long fileId) {
        String filePath = fileStorageService.getFilePath(fileId);
        try {
            byte[] docBytes = Files.readAllBytes(Paths.get(filePath));
            try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docBytes))) {
                WordTextExtractor.ExtractResult result = WordTextExtractor.extract(doc);

                Map<String, Object> response = new HashMap<>();
                response.put("fullText", result.getFullText());

                List<Map<String, Object>> segmentList = new ArrayList<>();
                for (WordTextExtractor.TextSegment seg : result.getSegments()) {
                    Map<String, Object> segMap = new HashMap<>();
                    segMap.put("type", seg.getType());
                    segMap.put("elementIndex", seg.getElementIndex());
                    segMap.put("text", seg.getText());
                    segMap.put("fullTextOffset", seg.getFullTextOffset());
                    if (seg.getTableIndex() != null) {
                        segMap.put("tableIndex", seg.getTableIndex());
                        segMap.put("rowIndex", seg.getRowIndex());
                        segMap.put("cellIndex", seg.getCellIndex());
                    }
                    segmentList.add(segMap);
                }
                response.put("segments", segmentList);

                return response;
            }
        } catch (Exception e) {
            throw new RuntimeException("提取文档文本失败: " + e.getMessage(), e);
        }
    }
}
