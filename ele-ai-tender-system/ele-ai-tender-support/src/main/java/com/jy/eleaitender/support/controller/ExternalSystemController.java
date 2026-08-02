package com.jy.eleaitender.support.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.support.SysAccessSystem;
import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.support.service.IExternalSystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 接入系统控制器
 */
@RestController
@RequestMapping("/api/external-systems")
@Tag(name = "接入系统管理")
public class ExternalSystemController {

    @Autowired
    private IExternalSystemService externalSystemService;

    @GetMapping
    @RequireLogin
    @Operation(summary = "分页查询接入系统")
    public Result<Page<SysAccessSystem>> getList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String systemName,
            @RequestParam(required = false) String appKey,
            @RequestParam(required = false) Integer status) {
        return Result.success(externalSystemService.getSystemPage(pageNum, pageSize, systemName, appKey, status));
    }

    @GetMapping("/{id}")
    @RequireLogin
    @Operation(summary = "获取接入系统详情")
    public Result<SysAccessSystem> getById(@PathVariable Long id) {
        return Result.success(externalSystemService.getSystemById(id));
    }

    @PostMapping
    @RequireLogin
    @Operation(summary = "创建接入系统")
    public Result<SysAccessSystem> create(@RequestBody SysAccessSystem system) {
        return Result.success(externalSystemService.createSystem(system));
    }

    @PutMapping("/{id}")
    @RequireLogin
    @Operation(summary = "更新接入系统")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysAccessSystem system) {
        system.setId(id);
        externalSystemService.updateSystem(system);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireLogin
    @Operation(summary = "删除接入系统")
    public Result<Void> delete(@PathVariable Long id) {
        externalSystemService.deleteSystem(id);
        return Result.success();
    }

    @PostMapping("/{id}/regenerate-secret")
    @RequireLogin
    @Operation(summary = "重新生成密钥")
    public Result<Map<String, String>> regenerateSecret(@PathVariable Long id) {
        String newSecret = externalSystemService.regenerateSecret(id);
        Map<String, String> result = new HashMap<>();
        result.put("appSecret", newSecret);
        return Result.success(result);
    }

    @PutMapping("/{id}/status")
    @RequireLogin
    @Operation(summary = "启用/禁用接入系统")
    public Result<Void> changeStatus(
            @PathVariable Long id,
            @RequestParam(required = false) Integer status,
            @RequestBody(required = false) Map<String, Integer> body) {
        if (status == null && body != null) {
            status = body.get("status");
        }
        externalSystemService.changeStatus(id, status);
        return Result.success();
    }
}
