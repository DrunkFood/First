package com.jy.eleaitender.core.service;

import com.jy.eleaitender.common.entity.core.AiProjectVersion;

import java.util.List;

/**
 * 项目版本服务接口
 */
public interface IProjectVersionService {

    /**
     * 根据项目ID获取版本列表
     */
    List<AiProjectVersion> getByProjectId(Long projectId);

    /**
     * 创建版本快照
     */
    AiProjectVersion createVersion(Long projectId, String contentSnapshot, String changeDescription);
}
