package com.jy.eleaitender.core.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.core.entity.AiRequirement;
import com.jy.eleaitender.core.service.IRequirementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 业务需求控制器
 */
@RestController
@RequestMapping("/api/v1/requirements")
@Tag(name = "业务需求管理")
public class RequirementController {

    @Autowired
    private IRequirementService requirementService;

    @GetMapping
    @RequireLogin
    @Operation(summary = "分页查询需求列表")
    public Result<Page<AiRequirement>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String requirementName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long projectId) {
        return Result.success(requirementService.getPage(pageNum, pageSize, requirementName, status, projectId));
    }

    @GetMapping("/{id}")
    @RequireLogin
    @Operation(summary = "获取需求详情")
    public Result<AiRequirement> getById(@PathVariable Long id) {
        return Result.success(requirementService.getById(id));
    }

    @PostMapping
    @RequireLogin
    @Operation(summary = "创建需求")
    public Result<AiRequirement> create(@RequestBody AiRequirement requirement) {
        return Result.success(requirementService.create(requirement));
    }

    @PutMapping("/{id}")
    @RequireLogin
    @Operation(summary = "更新需求")
    public Result<Void> update(@PathVariable Long id, @RequestBody AiRequirement requirement) {
        requirementService.update(id, requirement);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequireLogin
    @Operation(summary = "删除需求")
    public Result<Void> deleteById(@PathVariable Long id) {
        requirementService.deleteById(id);
        return Result.success();
    }

    @PostMapping("/{id}/match")
    @RequireLogin
    @Operation(summary = "匹配历史模板")
    public Result<AiRequirement> match(
            @PathVariable Long id,
            @RequestParam Long matchedFileId,
            @RequestParam(defaultValue = "MANUAL_SELECT") String matchMode) {
        return Result.success(requirementService.matchTemplate(id, matchedFileId, matchMode));
    }
}
