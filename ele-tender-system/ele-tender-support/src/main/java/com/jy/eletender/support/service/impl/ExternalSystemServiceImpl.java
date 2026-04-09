package com.jy.eletender.support.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eletender.common.constant.FileConstants;
import com.jy.eletender.common.constant.RedisKeyConstant;
import com.jy.eletender.common.entity.support.SysAccessSystem;
import com.jy.eletender.support.mapper.SysAccessSystemMapper;
import com.jy.eletender.support.service.IExternalSystemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 接入系统服务实现
 */
@Slf4j
@Service
public class ExternalSystemServiceImpl implements IExternalSystemService {

    @Autowired
    private SysAccessSystemMapper accessSystemMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

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
        validateSuffixes(system);
        // 生成 AppKey 和 AppSecret
        system.setAppKey(generateAppKey());
        system.setAppSecret(generateAppSecret());
        system.setStatus(1);
        accessSystemMapper.insert(system);
        registerAfterCommitSuffixRefresh();
        return system;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSystem(SysAccessSystem system) {
        validateSuffixes(system);
        // 不更新密钥
        system.setAppKey(null);
        system.setAppSecret(null);
        accessSystemMapper.updateById(system);
        registerAfterCommitSuffixRefresh();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSystem(Long id) {
        accessSystemMapper.deleteById(id);
        registerAfterCommitSuffixRefresh();
    }

    @Override
    public String regenerateSecret(Long id) {
        String newSecret = generateAppSecret();
        SysAccessSystem system = new SysAccessSystem();
        system.setId(id);
        system.setAppSecret(newSecret);
        accessSystemMapper.updateById(system);
        return newSecret;
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        SysAccessSystem system = new SysAccessSystem();
        system.setId(id);
        system.setStatus(status);
        accessSystemMapper.updateById(system);
    }

    private void validateSuffixes(SysAccessSystem system) {
        FileConstants.validateDocumentSuffix(system.getTenderDocumentSuffix());
        FileConstants.validateDocumentSuffix(system.getBidDocumentSuffix());
    }

    private void registerAfterCommitSuffixRefresh() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            safeRefreshRegisteredSuffixes();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        safeRefreshRegisteredSuffixes();
                    }
                });
    }

    private void safeRefreshRegisteredSuffixes() {
        try {
            refreshRegisteredSuffixes();
        } catch (Exception e) {
            log.error("DB 已提交但 Redis 后缀刷新失败，将在下次操作时自愈", e);
        }
    }

    private void refreshRegisteredSuffixes() {
        LambdaQueryWrapper<SysAccessSystem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysAccessSystem::getIsDelete, 0);
        wrapper.and(w -> w.isNotNull(SysAccessSystem::getTenderDocumentSuffix)
                .or()
                .isNotNull(SysAccessSystem::getBidDocumentSuffix));
        List<SysAccessSystem> systems = accessSystemMapper.selectList(wrapper);

        Set<String> suffixes = new LinkedHashSet<>();
        for (SysAccessSystem sys : systems) {
            if (StringUtils.hasText(sys.getTenderDocumentSuffix())) {
                suffixes.add(sys.getTenderDocumentSuffix());
            }
            if (StringUtils.hasText(sys.getBidDocumentSuffix())) {
                suffixes.add(sys.getBidDocumentSuffix());
            }
        }

        String key = RedisKeyConstant.REGISTERED_SUFFIXES;
        if (suffixes.isEmpty()) {
            redisTemplate.delete(key);
        } else {
            String tmpKey = key + ":tmp:" + UUID.randomUUID();
            try {
                redisTemplate.opsForSet().add(tmpKey, suffixes.toArray(String[]::new));
                redisTemplate.rename(tmpKey, key);
            } catch (Exception e) {
                redisTemplate.delete(tmpKey);
                throw e;
            }
        }
        log.info("已刷新注册后缀到 Redis: {}", suffixes);
    }

    private String generateAppKey() {
        return "AK" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private String generateAppSecret() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
