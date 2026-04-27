package com.jy.eleaitender.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eleaitender.common.entity.core.TbProject;
import com.jy.eleaitender.common.entity.core.TbProjectVersion;
import com.jy.eleaitender.common.enums.ProjectStatus;
import com.jy.eleaitender.core.mapper.TbProjectVersionMapper;
import com.jy.eleaitender.core.service.IProjectService;
import com.jy.eleaitender.core.service.IProjectVersionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 项目版本服务实现
 */
@Slf4j
@Service
public class ProjectVersionServiceImpl implements IProjectVersionService {

    @Autowired
    private TbProjectVersionMapper projectVersionMapper;

    @Autowired
    private IProjectService projectService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<TbProjectVersion> getByProjectId(Long projectId) {
        // 校验项目归属
        projectService.getById(projectId);
        LambdaQueryWrapper<TbProjectVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TbProjectVersion::getProjectId, projectId);
        wrapper.orderByDesc(TbProjectVersion::getVersionNo);
        return projectVersionMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TbProjectVersion createVersion(Long projectId, String changeDescription) throws JsonProcessingException {
        // 校验项目归属
        TbProject project = projectService.getById(projectId);
        String contentSnapshot = objectMapper.writeValueAsString(Map.of(
                "generatedFileId", project.getGeneratedFileId() != null ? project.getGeneratedFileId() : 0,
                "status", project.getStatus()
        ));
        // 查询当前最大版本号
        LambdaQueryWrapper<TbProjectVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TbProjectVersion::getProjectId, projectId);
        wrapper.orderByDesc(TbProjectVersion::getVersionNo);
        wrapper.last("LIMIT 1");
        TbProjectVersion latest = projectVersionMapper.selectOne(wrapper);

        int nextVersion = (latest != null) ? latest.getVersionNo() + 1 : 1;

        TbProjectVersion version = new TbProjectVersion();
        version.setProjectId(projectId);
        version.setVersionNo(nextVersion);
        version.setContentSnapshot(contentSnapshot);
        if (StringUtils.hasText(changeDescription)) {
            version.setChangeDescription(changeDescription);
        } else {
            version.setChangeDescription("第" + nextVersion + "版");
        }
        version.setCreateId(project.getCreateId());
        version.setCreateName(project.getCreateName());

        projectVersionMapper.insert(version);
        log.info("检测完成版本快照创建成功: projectId={}", projectId);
        return version;
    }
}
