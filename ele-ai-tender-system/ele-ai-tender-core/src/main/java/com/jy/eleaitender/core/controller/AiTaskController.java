package com.jy.eleaitender.core.controller;

import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.core.dto.response.AiTaskVO;
import com.jy.eleaitender.core.service.IAiTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI任务控制器
 */
@RestController
@RequestMapping("/api/v1/ai-tasks")
@Tag(name = "AI任务管理")
public class AiTaskController {

    @Autowired
    private IAiTaskService aiTaskService;

    @GetMapping("/{id}")
    @RequireLogin
    @Operation(summary = "查询任务状态")
    public Result<AiTaskVO> getTaskStatus(@PathVariable Long id) {
        return Result.success(aiTaskService.getTaskStatus(id));
    }

    @GetMapping("/project/{projectId}")
    @RequireLogin
    @Operation(summary = "查询项目所有任务")
    public Result<List<AiTaskVO>> getTasksByProject(@PathVariable Long projectId) {
        return Result.success(aiTaskService.getTasksByProject(projectId));
    }

    @PostMapping("/{id}/retry")
    @RequireLogin
    @Operation(summary = "重试任务")
    public Result<Void> retryTask(@PathVariable Long id) {
        aiTaskService.retryTask(id);
        return Result.success();
    }

    @PostMapping("/{id}/skip")
    @RequireLogin
    @Operation(summary = "跳过任务(降级手动)")
    public Result<Void> skipTask(@PathVariable Long id) {
        aiTaskService.skipTask(id);
        return Result.success();
    }

    @GetMapping("/active")
    @RequireLogin
    @Operation(summary = "查询业务实体的活跃AI任务")
    public Result<AiTaskVO> getActiveTask(
            @RequestParam String taskType,
            @RequestParam Long bizId,
            @RequestParam String bizType) {
        return Result.success(aiTaskService.getActiveTask(taskType, bizId, bizType));
    }

    @GetMapping("/project/{projectId}/active")
    @RequireLogin
    @Operation(summary = "查询项目的活跃AI任务列表")
    public Result<List<AiTaskVO>> getActiveTasksByProject(@PathVariable Long projectId) {
        return Result.success(aiTaskService.getActiveTasksByProject(projectId));
    }
}
