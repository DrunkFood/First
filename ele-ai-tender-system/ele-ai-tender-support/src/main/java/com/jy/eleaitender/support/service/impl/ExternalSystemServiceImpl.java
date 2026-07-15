package com.jy.eleaitender.support.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jy.eleaitender.common.entity.support.SysAccessSystem;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.support.mapper.SysAccessSystemMapper;
import com.jy.eleaitender.support.service.IExternalSystemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * 接入系统服务实现
 */
@Slf4j
@Service
public class ExternalSystemServiceImpl extends ServiceImpl<SysAccessSystemMapper, SysAccessSystem> implements IExternalSystemService {

    @Autowired
    private SysAccessSystemMapper accessSystemMapper;

    @Override
    public Page<SysAccessSystem> getSystemPage(Integer pageNum, Integer pageSize, String systemName, String appKey, Integer status) {
        Page<SysAccessSystem> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysAccessSystem> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(systemName)) {
            wrapper.like(SysAccessSystem::getSystemName, systemName);
        }
        if (StringUtils.hasText(appKey)) {
            wrapper.like(SysAccessSystem::getAppKey, appKey);
        }
        if (status != null) {
            wrapper.eq(SysAccessSystem::getStatus, status);
        }

        wrapper.eq(SysAccessSystem::getIsDelete, 0);
        wrapper.orderByDesc(SysAccessSystem::getCreateTime);

        return accessSystemMapper.selectPage(page, wrapper);
    }

    @Override
    public SysAccessSystem getSystemById(Long id) {
        return accessSystemMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysAccessSystem createSystem(SysAccessSystem system) {
        // 生成 AppKey 和 AppSecret
        system.setAppKey(generateAppKey());
        system.setAppSecret(generateAppSecret());
        system.setStatus(1);
        accessSystemMapper.insert(system);

        return system;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSystem(SysAccessSystem system) {
        // 不更新密钥
        system.setAppKey(null);
        system.setAppSecret(null);
        accessSystemMapper.updateById(system);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSystem(Long id) {
        SysAccessSystem system = accessSystemMapper.selectById(id);
        if (system == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "接入系统不存在");
        }
        accessSystemMapper.deleteById(id);
    }

    @Override
    public String regenerateSecret(Long id) {
        // 先查询系统信息获取appKey
        SysAccessSystem system = accessSystemMapper.selectById(id);
        if (system == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "接入系统不存在");
        }
        // 更新密钥
        system.setAppSecret(generateAppSecret());
        accessSystemMapper.updateById(system);
        return system.getAppSecret();
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        SysAccessSystem system = new SysAccessSystem();
        system.setId(id);
        system.setStatus(status);
        accessSystemMapper.updateById(system);
    }

    private String generateAppKey() {
        return "AK" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private String generateAppSecret() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
