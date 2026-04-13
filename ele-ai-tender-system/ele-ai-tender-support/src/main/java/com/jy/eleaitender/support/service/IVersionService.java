package com.jy.eleaitender.support.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.support.SysMainVersion;
import com.jy.eleaitender.common.entity.support.SysPluginVersion;

import java.util.List;

/**
 * 版本服务接口
 */
public interface IVersionService {
    
    // ========== 主版本管理 ==========
    Page<SysMainVersion> getVersionPage(Integer pageNum, Integer pageSize, String versionNumber, String versionName, Integer status);
    
    SysMainVersion getVersionById(Long id);
    
    SysMainVersion createVersion(SysMainVersion version);
    
    void updateVersion(SysMainVersion version);
    
    void deleteVersion(Long id);
    
    void publishVersion(Long id);
    
    void deprecateVersion(Long id);
    
    // ========== 插件管理 ==========
    List<SysPluginVersion> getPluginsByVersionId(Long versionId);
    
    SysPluginVersion createPlugin(SysPluginVersion plugin);
    
    void updatePlugin(SysPluginVersion plugin);
    
    void deletePlugin(Long id);
    
    void publishPlugin(Long id);
    
    void deprecatePlugin(Long id);
}
