package com.jy.eleaitender.support.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.ai.AiTemplate;
import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.support.service.ITemplateConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/template-configs")
@Tag(name = "模板管理")
public class TemplateConfigController {

    @Autowired
    private ITemplateConfigService templateConfigService;

    @GetMapping
    @Operation(summary = "查询模板列表")
    @RequireLogin
    public Result<Page<AiTemplate>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) String projectCategory,
            @RequestParam(required = false) String projectType) {
        return Result.success(templateConfigService.getPage(pageNum, pageSize, templateName, projectCategory, projectType));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取模板详情")
    @RequireLogin
    public Result<AiTemplate> getById(@PathVariable Long id) {
        return Result.success(templateConfigService.getById(id));
    }

    @PostMapping
    @Operation(summary = "创建模板")
    @RequireLogin
    public Result<AiTemplate> create(@RequestBody AiTemplate template) {
        return Result.success(templateConfigService.create(template));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新模板")
    @RequireLogin
    public Result<Void> update(@PathVariable Long id, @RequestBody AiTemplate template) {
        template.setId(id);
        templateConfigService.update(template);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除模板")
    @RequireLogin
    public Result<Void> delete(@PathVariable Long id) {
        templateConfigService.deleteById(id);
        return Result.success();
    }

    @PostMapping("/{id}/set-default")
    @Operation(summary = "设为默认模板")
    @RequireLogin
    public Result<Void> setDefault(@PathVariable Long id) {
        templateConfigService.setDefault(id);
        return Result.success();
    }
}
