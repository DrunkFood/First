package com.jy.eleaitender.file.service.impl;

import com.jy.eleaitender.common.dto.response.FileUploadResponse;
import com.jy.eleaitender.common.dto.response.WordStructureVO;
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
}
