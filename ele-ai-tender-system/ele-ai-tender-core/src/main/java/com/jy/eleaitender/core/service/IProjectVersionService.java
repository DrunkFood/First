package com.jy.eleaitender.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jy.eleaitender.common.entity.core.TbProjectVersion;

import java.util.List;

/**
 * 项目版本服务接口
 */
public interface IProjectVersionService {

    /**
     * 根据项目ID获取版本列表
     */
    List<TbProjectVersion> getByProjectId(Long projectId);

    /**
     * 创建版本快照
     */
    TbProjectVersion createVersion(Long projectId, String changeDescription) throws JsonProcessingException;
}
