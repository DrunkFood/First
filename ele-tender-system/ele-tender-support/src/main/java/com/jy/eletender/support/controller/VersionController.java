package com.jy.eletender.support.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eletender.common.response.Result;
import com.jy.eletender.common.entity.support.SysMainVersion;
import com.jy.eletender.common.entity.support.SysPluginVersion;
import com.jy.eletender.common.security.annotation.RequireLogin;
import com.jy.eletender.support.service.IVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 版本控制器
 */
@RestController
@RequestMapping("/api/versions")
@Tag(name = "版本管理")
public class VersionController {

    @Autowired
    private IVersionService versionService;

    // ========== 主版本管理 ==========
    
    @GetMapping
    @RequireLogin
    @Operation(summary = "分页查询版本")
    public Result<Page<SysMainVersion>> getList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String versionNumber,
            @RequestParam(required = false) String versionName,
            @RequestParam(required = false) Integer status) {
        return Result.success(versionService.getVersionPage(pageNum, pageSize, versionNumber, versionName, status));
    }

    @GetMapping("/{id}")
    @RequireLogin
    @Operation(summary = "获取版本详情")
    public Result<SysMainVersion> getById(@PathVariable Long id) {
        return Result.success(versionService.getVersionById(id));
    }

    @PostMapping
    @RequireLogin
    @Operation(summary = "创建版本")
    public Result<SysMainVersion> create(@RequestBody SysMainVersion version) {
        return Result.success(versionService.createVersion(version));
    }

    @PutMapping("/{id}")
    @RequireLogin
    @Operation(summary = "更新版本")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysMainVersion version) {
        version.setId(id);
        versionService.updateVersion(version);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireLogin
    @Operation(summary = "删除版本")
    public Result<Void> delete(@PathVariable Long id) {
        versionService.deleteVersion(id);
        return Result.success();
    }

    @PostMapping("/{id}/publish")
    @RequireLogin
    @Operation(summary = "发布版本")
    public Result<Void> publish(@PathVariable Long id) {
        versionService.publishVersion(id);
        return Result.success();
    }

    @PostMapping("/{id}/deprecate")
    @RequireLogin
    @Operation(summary = "废弃版本")
    public Result<Void> deprecate(@PathVariable Long id) {
        versionService.deprecateVersion(id);
        return Result.success();
    }

    // ========== 插件管理 ==========
    
    @GetMapping("/{versionId}/plugins")
    @RequireLogin
    @Operation(summary = "获取版本的插件列表")
    public Result<List<SysPluginVersion>> getPlugins(@PathVariable Long versionId) {
        return Result.success(versionService.getPluginsByVersionId(versionId));
    }

    @PostMapping("/{versionId}/plugins")
    @RequireLogin
    @Operation(summary = "创建插件")
    public Result<SysPluginVersion> createPlugin(@PathVariable Long versionId, @RequestBody SysPluginVersion plugin) {
        // 设置版本ID
        SysMainVersion version = versionService.getVersionById(versionId);
        if (version != null) {
            plugin.setCompatibleMainVersion(version.getVersionNumber());
        }
        return Result.success(versionService.createPlugin(plugin));
    }

    @PutMapping("/plugins/{id}")
    @RequireLogin
    @Operation(summary = "更新插件")
    public Result<Void> updatePlugin(@PathVariable Long id, @RequestBody SysPluginVersion plugin) {
        plugin.setId(id);
        versionService.updatePlugin(plugin);
        return Result.success();
    }

    @DeleteMapping("/plugins/{id}")
    @RequireLogin
    @Operation(summary = "删除插件")
    public Result<Void> deletePlugin(@PathVariable Long id) {
        versionService.deletePlugin(id);
        return Result.success();
    }
}
