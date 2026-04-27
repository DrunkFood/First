package com.jy.eleaitender.file.service.impl;

import com.jy.eleaitender.common.dto.FixReplacement;
import com.jy.eleaitender.common.dto.response.FileUploadResponse;
import com.jy.eleaitender.common.dto.response.WordFixResultVO;
import com.jy.eleaitender.common.dto.response.WordStructureVO;
import com.jy.eleaitender.file.engine.WordDocumentFixEngine;
import com.jy.eleaitender.file.engine.WordStructureParser;
import com.jy.eleaitender.file.engine.WordTemplateEngine;
import com.jy.eleaitender.file.service.IFileStorageService;
import com.jy.eleaitender.file.service.IWordDocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Word文档服务实现
 */
@Slf4j
@Service
public class WordDocumentServiceImpl implements IWordDocumentService {

    @Autowired
    private IFileStorageService fileStorageService;

    @Autowired
    private WordStructureParser structureParser;

    @Autowired
    private WordTemplateEngine templateEngine;

    @Autowired
    private WordDocumentFixEngine fixEngine;

    @Override
    public WordStructureVO getFileStructure(Long fileId) {
        String filePath = fileStorageService.getFilePath(fileId);
        try (InputStream is = new FileInputStream(filePath)) {
            return structureParser.parse(is);
        } catch (Exception e) {
            throw new RuntimeException("获取Word文档结构失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Long generateDocument(Long templateFileId, Map<String, Object> data, String fileName) {
        String templatePath = fileStorageService.getFilePath(templateFileId);
        try (InputStream templateStream = new FileInputStream(templatePath)) {
            byte[] rendered = templateEngine.render(templateStream, data);
            // 上传生成结果到文件服务
            String bizType = "generated";
            FileUploadResponse response = fileStorageService.uploadFromBytes(
                    new ByteArrayInputStream(rendered), fileName, bizType);
            return response.getFileId();
        } catch (Exception e) {
            throw new RuntimeException("基于模板生成文档失败: " + e.getMessage(), e);
        }
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
            String fileName = "fixed_" + System.currentTimeMillis() + "_" + baseName;
            FileUploadResponse uploadResp = fileStorageService.uploadFromBytes(
                    new ByteArrayInputStream(result.getDocumentBytes()), fileName, "detection-fixed");

            WordFixResultVO vo = new WordFixResultVO();
            vo.setFileId(uploadResp.getFileId());
            vo.setFixedCount(result.getFixedCount());
            vo.setFailedCount(result.getFailedCount());
            return vo;
        } catch (Exception e) {
            throw new RuntimeException("修复Word文档失败: " + e.getMessage(), e);
        }
    }
}
