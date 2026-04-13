package com.jy.eleaitender.support.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.ai.AiModelConfig;
import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.support.service.IModelConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/model-configs")
@Tag(name = "AI模型配置")
public class ModelConfigController {

    @Autowired
    private IModelConfigService modelConfigService;

    @GetMapping
    @Operation(summary = "查询模型配置列表")
    @RequireLogin
    public Result<Page<AiModelConfig>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String modelType,
            @RequestParam(required = false) String usageScenario) {
        return Result.success(modelConfigService.getPage(pageNum, pageSize, modelType, usageScenario));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取模型配置详情")
    @RequireLogin
    public Result<AiModelConfig> getById(@PathVariable Long id) {
        return Result.success(modelConfigService.getById(id));
    }

    @PostMapping
    @Operation(summary = "创建模型配置")
    @RequireLogin
    public Result<AiModelConfig> create(@RequestBody AiModelConfig config) {
        return Result.success(modelConfigService.create(config));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新模型配置")
    @RequireLogin
    public Result<Void> update(@PathVariable Long id, @RequestBody AiModelConfig config) {
        config.setId(id);
        modelConfigService.update(config);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除模型配置")
    @RequireLogin
    public Result<Void> delete(@PathVariable Long id) {
        modelConfigService.deleteById(id);
        return Result.success();
    }

    @PutMapping("/{id}/active")
    @Operation(summary = "激活/停用模型配置")
    @RequireLogin
    public Result<Void> setActive(@PathVariable Long id, @RequestParam Integer isActive) {
        modelConfigService.setActive(id, isActive);
        return Result.success();
    }
}
