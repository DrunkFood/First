package com.jy.eleaitender.core.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.core.dto.response.ProjectPhaseVO;
import com.jy.eleaitender.core.entity.AiProject;
import com.jy.eleaitender.core.entity.AiProjectVersion;
import com.jy.eleaitender.core.service.IProjectService;
import com.jy.eleaitender.core.service.IProjectVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 项目管理控制器
 */
@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "项目管理")
public class ProjectController {

    @Autowired
    private IProjectService projectService;

    @Autowired
    private IProjectVersionService projectVersionService;

    @GetMapping
    @RequireLogin
    @Operation(summary = "分页查询项目列表")
    public Result<Page<AiProject>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String projectCategory) {
        return Result.success(projectService.getPage(pageNum, pageSize, projectName, status, projectCategory));
    }

    @GetMapping("/{id}")
    @RequireLogin
    @Operation(summary = "获取项目详情")
    public Result<AiProject> getById(@PathVariable Long id) {
        return Result.success(projectService.getById(id));
    }

    @PostMapping
    @RequireLogin
    @Operation(summary = "创建项目")
    public Result<AiProject> create(@RequestBody AiProject project) {
        return Result.success(projectService.create(project));
    }

    @PutMapping("/{id}")
    @RequireLogin
    @Operation(summary = "更新项目")
    public Result<Void> update(@PathVariable Long id, @RequestBody AiProject project) {
        projectService.update(id, project);
        return Result.success();
    }

    @DeleteMapping
    @RequireLogin
    @Operation(summary = "批量删除项目")
    public Result<Void> deleteByIds(@RequestBody List<Long> ids) {
        projectService.deleteByIds(ids);
        return Result.success();
    }

    @GetMapping("/{id}/versions")
    @RequireLogin
    @Operation(summary = "获取项目版本历史")
    public Result<List<AiProjectVersion>> getVersions(@PathVariable Long id) {
        return Result.success(projectVersionService.getByProjectId(id));
    }

    @PostMapping("/{id}/export")
    @RequireLogin
    @Operation(summary = "导出项目招标文件")
    public Result<Void> export(@PathVariable Long id) {
        // TODO: 实现导出Word文档逻辑（T07实现）
        return Result.success();
    }

    @GetMapping("/{id}/phase")
    @RequireLogin
    @Operation(summary = "获取项目当前阶段信息")
    public Result<ProjectPhaseVO> getPhase(@PathVariable Long id) {
        return Result.success(projectService.getPhase(id));
    }

    @PutMapping("/{id}/phase")
    @RequireLogin
    @Operation(summary = "手动推进阶段")
    public Result<Void> advancePhase(@PathVariable Long id, @RequestParam Integer targetPhase) {
        projectService.advancePhase(id, targetPhase);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @RequireLogin
    @Operation(summary = "变更项目状态")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam String targetStatus) {
        projectService.changeStatus(id, targetStatus);
        return Result.success();
    }

    @PostMapping("/{id}/cancel")
    @RequireLogin
    @Operation(summary = "取消项目")
    public Result<Void> cancel(@PathVariable Long id) {
        projectService.cancelProject(id);
        return Result.success();
    }

    @PostMapping("/{id}/publish")
    @RequireLogin
    @Operation(summary = "发布项目")
    public Result<Void> publish(@PathVariable Long id) {
        projectService.publishProject(id);
        return Result.success();
    }

    @PostMapping("/{id}/archive")
    @RequireLogin
    @Operation(summary = "归档项目")
    public Result<Void> archive(@PathVariable Long id) {
        projectService.archiveProject(id);
        return Result.success();
    }
}
