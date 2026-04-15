package com.jy.eleaitender.support.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.logging.OperationLog;
import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.support.dto.ModelConfigCreateDTO;
import com.jy.eleaitender.support.dto.ModelConfigUpdateDTO;
import com.jy.eleaitender.support.service.IModelConfigService;
import com.jy.eleaitender.support.vo.ModelConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/model-configs")
@Tag(name = "AI模型配置")
public class ModelConfigController {

    @Autowired
    private IModelConfigService modelConfigService;

    @GetMapping
    @Operation(summary = "查询模型配置列表")
    @RequireLogin
    public Result<Page<ModelConfigVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String modelType,
            @RequestParam(required = false) String usageScenario,
            @RequestParam(required = false) String modelName,
            @RequestParam(required = false) Integer isActive) {
        return Result.success(modelConfigService.getPage(pageNum, pageSize, modelType, usageScenario, modelName, isActive));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取模型配置详情")
    @RequireLogin
    public Result<ModelConfigVO> getById(@PathVariable Long id) {
        return Result.success(modelConfigService.getDetailById(id));
    }

    @PostMapping
    @Operation(summary = "创建模型配置")
    @RequireLogin
    @OperationLog("创建模型配置")
    public Result<ModelConfigVO> create(@Valid @RequestBody ModelConfigCreateDTO dto) {
        return Result.success(modelConfigService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新模型配置")
    @RequireLogin
    @OperationLog("更新模型配置")
    public Result<Void> update(@PathVariable Long id, @RequestBody ModelConfigUpdateDTO dto) {
        modelConfigService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除模型配置")
    @RequireLogin
    @OperationLog("删除模型配置")
    public Result<Void> delete(@PathVariable Long id) {
        modelConfigService.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除模型配置")
    @RequireLogin
    @OperationLog("批量删除模型配置")
    public Result<Void> deleteByIds(@RequestBody List<Long> ids) {
        modelConfigService.deleteByIds(ids);
        return Result.success();
    }

    @PutMapping("/{id}/active")
    @Operation(summary = "激活/停用模型配置")
    @RequireLogin
    @OperationLog("变更模型配置状态")
    public Result<Void> setActive(@PathVariable Long id, @RequestParam Integer isActive) {
        modelConfigService.setActive(id, isActive);
        return Result.success();
    }
}
