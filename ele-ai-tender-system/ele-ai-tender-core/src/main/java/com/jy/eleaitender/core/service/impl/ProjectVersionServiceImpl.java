package com.jy.eleaitender.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jy.eleaitender.common.entity.core.AiProjectVersion;
import com.jy.eleaitender.core.mapper.AiProjectVersionMapper;
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
    private AiProjectVersionMapper projectVersionMapper;

    @Override
    public List<AiProjectVersion> getByProjectId(Long projectId) {
        LambdaQueryWrapper<AiProjectVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiProjectVersion::getProjectId, projectId);
        wrapper.orderByDesc(AiProjectVersion::getVersionNo);
        return projectVersionMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public AiProjectVersion createVersion(Long projectId, String contentSnapshot, String changeDescription) {
        // 查询当前最大版本号
        LambdaQueryWrapper<AiProjectVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiProjectVersion::getProjectId, projectId);
        wrapper.orderByDesc(AiProjectVersion::getVersionNo);
        wrapper.last("LIMIT 1");
        AiProjectVersion latest = projectVersionMapper.selectOne(wrapper);

        int nextVersion = (latest != null) ? latest.getVersionNo() + 1 : 1;

        AiProjectVersion version = new AiProjectVersion();
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
