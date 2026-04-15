package com.jy.eleaitender.core.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.common.entity.core.AiTemplate;
import com.jy.eleaitender.core.service.ITemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 模板管理控制器
 */
@RestController
@RequestMapping("/api/v1/templates")
@Tag(name = "模板管理")
public class TemplateController {

    @Autowired
    private ITemplateService templateService;

    @GetMapping
    @RequireLogin
    @Operation(summary = "分页查询模板列表")
    public Result<Page<AiTemplate>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) String projectCategory,
            @RequestParam(required = false) String projectType) {
        return Result.success(templateService.getPage(pageNum, pageSize, templateName, projectCategory, projectType));
    }

    @GetMapping("/{id}")
    @RequireLogin
    @Operation(summary = "获取模板详情")
    public Result<AiTemplate> getById(@PathVariable Long id) {
        return Result.success(templateService.getById(id));
    }

    @GetMapping("/default")
    @RequireLogin
    @Operation(summary = "获取默认模板")
    public Result<AiTemplate> getDefault(
            @RequestParam(required = false) String projectCategory,
            @RequestParam(required = false) String projectType) {
        return Result.success(templateService.getDefault(projectCategory, projectType));
    }
}
