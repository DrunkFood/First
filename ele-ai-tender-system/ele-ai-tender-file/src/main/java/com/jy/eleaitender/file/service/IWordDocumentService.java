package com.jy.eleaitender.file.service;

import com.jy.eleaitender.common.dto.response.WordStructureVO;

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
     * @param data           填充数据
     * @param fileName       生成文件名
     * @return 生成文件ID
     */
    Long generateDocument(Long templateFileId, Map<String, Object> data, String fileName);
}
