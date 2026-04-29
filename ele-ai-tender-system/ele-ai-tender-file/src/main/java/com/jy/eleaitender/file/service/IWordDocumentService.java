package com.jy.eleaitender.file.service;

import com.jy.eleaitender.common.dto.FixReplacement;
import com.jy.eleaitender.common.dto.response.WordFixResultVO;
import com.jy.eleaitender.common.dto.response.WordStructureVO;

import java.util.List;
import java.util.Map;

/**
 * Word文档服务接口
 */
public interface IWordDocumentService {

    /**
     * 获取Word文档结构
     *
     * @param fileId 文件ID
     * @return 文档结构
     */
    WordStructureVO getFileStructure(Long fileId);

    /**
     * 基于模板生成文档
     *
     * @param templateFileId 模板文件ID
     * @param fillDataRaw    填充数据
     * @param fileName       生成文件名
     * @return 生成文件ID
     */
    Long generateDocument(Long templateFileId, List<Map<String, Object>> fillDataRaw, String fileName);

    /**
     * 修复Word文档（替换文本）
     *
     * @param fileId       原始文件ID
     * @param replacements 替换列表
     * @return 修复结果
     */
    WordFixResultVO fixDocument(Long fileId, List<FixReplacement> replacements);

    /**
     * 提取Word文档文本+位置索引
     *
     * @param fileId 文件ID
     * @return 提取结果（fullText + segments）
     */
    Map<String, Object> extractText(Long fileId);
}
