package com.jy.eleaitender.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jy.eleaitender.common.entity.core.TbProjectVersion;
import com.jy.eleaitender.core.mapper.TbProjectVersionMapper;
import com.jy.eleaitender.core.service.IProjectService;
import com.jy.eleaitender.core.service.IProjectVersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 项目版本服务实现
 */
@Service
public class ProjectVersionServiceImpl implements IProjectVersionService {

    @Autowired
    private TbProjectVersionMapper projectVersionMapper;

    @Autowired
    private IProjectService projectService;

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
    public TbProjectVersion createVersion(Long projectId, String contentSnapshot, String changeDescription) {
        // 校验项目归属
        projectService.getById(projectId);
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

        projectVersionMapper.insert(version);
        return version;
    }
}
