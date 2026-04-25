package com.jy.eleaitender.core.service;

import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.core.dto.response.DocumentPreviewVO;

/**
 * 文档集成服务接口
 */
public interface IDocumentIntegrationService {

    /**
     * 执行文档集成
     */
    AiTask integrate(Long projectId);

    /**
     * 获取集成预览
     */
    DocumentPreviewVO getPreview(Long projectId);

    /**
     * 编辑集成后的文档内容
     */
    void editContent(Long projectId, String markdownContent);
}
