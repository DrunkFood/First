package com.jy.eletender.support.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eletender.common.entity.support.SysMainVersion;
import com.jy.eletender.common.entity.support.SysPluginVersion;
import com.jy.eletender.support.mapper.SysMainVersionMapper;
import com.jy.eletender.support.mapper.SysPluginVersionMapper;
import com.jy.eletender.support.service.IVersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 版本服务实现
 */
@Service
public class VersionServiceImpl implements IVersionService {

    @Autowired
    private SysMainVersionMapper mainVersionMapper;
    
    @Autowired
    private SysPluginVersionMapper pluginVersionMapper;

    @Override
    public Page<SysMainVersion> getVersionPage(Integer pageNum, Integer pageSize, String versionNumber, String versionName, Integer status) {
        Page<SysMainVersion> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysMainVersion> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(versionNumber)) {
            wrapper.like(SysMainVersion::getVersionNumber, versionNumber);
        }
        if (StringUtils.hasText(versionName)) {
            wrapper.like(SysMainVersion::getSystemName, versionName);
        }
        if (status != null) {
            wrapper.eq(SysMainVersion::getStatus, status);
        }
        
        wrapper.eq(SysMainVersion::getIsDelete, 0);
        wrapper.orderByDesc(SysMainVersion::getCreateTime);
        
        return mainVersionMapper.selectPage(page, wrapper);
    }

    @Override
    public SysMainVersion getVersionById(Long id) {
        return mainVersionMapper.selectById(id);
    }

    @Override
    public SysMainVersion createVersion(SysMainVersion version) {
        version.setStatus(0); // 草稿状态
        mainVersionMapper.insert(version);
        return version;
    }

    @Override
    public void updateVersion(SysMainVersion version) {
        mainVersionMapper.updateById(version);
    }

    @Override
    public void deleteVersion(Long id) {
        mainVersionMapper.deleteById(id);
    }

    @Override
    public void publishVersion(Long id) {
        SysMainVersion version = new SysMainVersion();
        version.setId(id);
        version.setStatus(1); // 已发布
        mainVersionMapper.updateById(version);
    }

    @Override
    public void deprecateVersion(Long id) {
        SysMainVersion version = new SysMainVersion();
        version.setId(id);
        version.setStatus(2); // 已下线
        mainVersionMapper.updateById(version);
    }

    @Override
    public List<SysPluginVersion> getPluginsByVersionId(Long versionId) {
        SysMainVersion mainVersion = mainVersionMapper.selectById(versionId);
        if (mainVersion == null) {
            return List.of();
        }
        
        LambdaQueryWrapper<SysPluginVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPluginVersion::getCompatibleMainVersion, mainVersion.getVersionNumber());
        wrapper.eq(SysPluginVersion::getIsDelete, 0);
        wrapper.orderByDesc(SysPluginVersion::getCreateTime);
        return pluginVersionMapper.selectList(wrapper);
    }

    @Override
    public SysPluginVersion createPlugin(SysPluginVersion plugin) {
        plugin.setStatus(0); // 草稿状态
        pluginVersionMapper.insert(plugin);
        return plugin;
    }

    @Override
    public void updatePlugin(SysPluginVersion plugin) {
        pluginVersionMapper.updateById(plugin);
    }

    @Override
    public void deletePlugin(Long id) {
        pluginVersionMapper.deleteById(id);
    }

    @Override
    public void publishPlugin(Long id) {
        SysPluginVersion plugin = new SysPluginVersion();
        plugin.setId(id);
        plugin.setStatus(1); // 已发布
        pluginVersionMapper.updateById(plugin);
    }

    @Override
    public void deprecatePlugin(Long id) {
        SysPluginVersion plugin = new SysPluginVersion();
        plugin.setId(id);
        plugin.setStatus(2); // 已下线
        pluginVersionMapper.updateById(plugin);
    }
}
