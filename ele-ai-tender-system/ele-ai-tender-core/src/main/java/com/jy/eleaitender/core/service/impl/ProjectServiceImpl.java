package com.jy.eleaitender.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.core.entity.AiProject;
import com.jy.eleaitender.core.mapper.AiProjectMapper;
import com.jy.eleaitender.core.service.IProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 项目服务实现
 */
@Service
public class ProjectServiceImpl implements IProjectService {

    @Autowired
    private AiProjectMapper projectMapper;

    @Override
    public Page<AiProject> getPage(Integer pageNum, Integer pageSize, String projectName, String status, String projectCategory) {
        Page<AiProject> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AiProject> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(projectName)) {
            wrapper.like(AiProject::getProjectName, projectName);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(AiProject::getStatus, status);
        }
        if (StringUtils.hasText(projectCategory)) {
            wrapper.eq(AiProject::getProjectCategory, projectCategory);
        }

        wrapper.orderByDesc(AiProject::getCreateTime);

        return projectMapper.selectPage(page, wrapper);
    }

    @Override
    public AiProject getById(Long id) {
        AiProject project = projectMapper.selectById(id);
        if (project == null) {
            throw new BusinessException(ResponseCode.PROJECT_NOT_FOUND);
        }
        return project;
    }

    @Override
    @Transactional
    public AiProject create(AiProject project) {
        // 生成项目编号
        project.setProjectCode(generateProjectCode());
        // 初始化状态为草稿
        if (!StringUtils.hasText(project.getStatus())) {
            project.setStatus("DRAFT");
        }
        projectMapper.insert(project);
        return project;
    }

    @Override
    @Transactional
    public void update(Long id, AiProject project) {
        AiProject existing = getById(id);
        project.setId(id);
        // 不允许修改项目编号
        project.setProjectCode(existing.getProjectCode());
        projectMapper.updateById(project);
    }

    @Override
    @Transactional
    public void deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择要删除的项目");
        }
        for (Long id : ids) {
            projectMapper.deleteById(id);
        }
    }

    @Override
    public String generateProjectCode() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomDigits = ThreadLocalRandom.current().nextInt(100000, 999999);
        return "AI-" + timestamp + "-" + randomDigits;
    }
}
