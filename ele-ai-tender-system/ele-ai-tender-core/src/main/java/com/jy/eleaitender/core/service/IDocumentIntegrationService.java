package com.jy.eleaitender.core.service;

import com.jy.eleaitender.core.dto.response.DocumentPreviewVO;

/**
 * 文档集成服务接口
 */
public interface IDocumentIntegrationService {

    /**
     * 执行文档集成（生成预览用HTML）
     */
    DocumentPreviewVO integrate(Long projectId);

    /**
     * 获取集成预览
     */
    DocumentPreviewVO getPreview(Long projectId);

    /**
     * 导出Word文档
     *
     * @return 生成的文件字节数组
     */
    byte[] exportWord(Long projectId);

    /**
     * 编辑集成后的文档内容
     */
    void editContent(Long projectId, String markdownContent);
}
