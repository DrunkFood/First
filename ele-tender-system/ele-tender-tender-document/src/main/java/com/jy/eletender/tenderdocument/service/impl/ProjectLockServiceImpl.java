package com.jy.eletender.tenderdocument.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jy.eletender.common.exception.BusinessException;
import com.jy.eletender.tenderdocument.entity.ProjectLock;
import com.jy.eletender.tenderdocument.mapper.ProjectLockMapper;
import com.jy.eletender.tenderdocument.service.IProjectLockService;
import com.jy.eletender.tenderdocument.support.TenderDocumentUserContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;

/**
 * 项目级首创人锁服务实现。
 * 职责：在招标文件编制全流程中统一校验并维护项目锁，确保同一项目仅由锁定人继续操作。
 */
@Service
public class ProjectLockServiceImpl implements IProjectLockService {

    private final ProjectLockMapper projectLockMapper;

    public ProjectLockServiceImpl(ProjectLockMapper projectLockMapper) {
        this.projectLockMapper = projectLockMapper;
    }

    /**
     * 校验项目级首创人锁；未加锁时由当前用户占有，已加锁时只允许锁定人继续操作。
     */
    @Override
    public ProjectLock verifyOrCreateLock(String projectId, TenderDocumentUserContext userContext) {
        ProjectLock existingLock = projectLockMapper.selectOne(Wrappers.<ProjectLock>lambdaQuery()
                .eq(ProjectLock::getProjectId, projectId)
                .last("limit 1"));
        if (existingLock == null) {
            ProjectLock projectLock = new ProjectLock();
            projectLock.setProjectId(projectId);
            projectLock.setOwnerUserId(userContext.getUserId());
            projectLock.setOwnerUserName(userContext.getUserName());
            projectLock.setOwnerEnterpriseId(userContext.getEnterpriseId());
            projectLock.setOwnerEnterpriseName(userContext.getEnterpriseName());
            projectLock.setOwnerEnterpriseCode(userContext.getEnterpriseCode());
            projectLock.setOwnerAppKey(userContext.getAppKey());
            projectLock.setLockTime(new Date());
            projectLockMapper.insert(projectLock);
            return projectLock;
        }

        // 锁的判断维度固定为 appKey + enterpriseCode + userId，避免同项目被其他系统或其他企业串用。
        if (StringUtils.equals(existingLock.getOwnerAppKey(), userContext.getAppKey())
                && StringUtils.equals(existingLock.getOwnerEnterpriseCode(), userContext.getEnterpriseCode())
                && Objects.equals(existingLock.getOwnerUserId(), userContext.getUserId())) {
            return existingLock;
        }

        throw new BusinessException("当前项目已由首创人锁定，仅首创人可查看和编辑");
    }
}
