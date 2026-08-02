package com.jy.eleaitender.core.controller;

import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.core.dto.response.AiTaskVO;
import com.jy.eleaitender.core.service.IAiTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * AI任务控制器
 * 精简后仅保留3个接口：
 * - GET /{id}        轮询任务状态
 * - POST /{id}/skip  跳过卡住的任务
 * - GET /latest      查询最新任务（页面加载用）
 */
@RestController
@RequestMapping("/api/v1/ai-tasks")
@Tag(name = "AI任务管理")
public class AiTaskController {

    @Autowired
    private IAiTaskService aiTaskService;

    @GetMapping("/{id:\\d+}")
    @RequireLogin
    @Operation(summary = "查询任务状态")
    public Result<AiTaskVO> getTaskStatus(@PathVariable Long id) {
        return Result.success(aiTaskService.getTask(id));
    }

    @PostMapping("/{id:\\d+}/skip")
    @RequireLogin
    @Operation(summary = "跳过任务(降级手动)")
    public Result<Void> skipTask(@PathVariable Long id) {
        aiTaskService.skipTask(id);
        return Result.success();
    }

    @GetMapping("/latest")
    @RequireLogin
    @Operation(summary = "查询业务实体的最新AI任务（含终态）")
    public Result<AiTaskVO> getLatestTask(
            @RequestParam String taskType,
            @RequestParam Long bizId,
            @RequestParam String bizType) {
        return Result.success(aiTaskService.getLatestTask(taskType, bizId, bizType));
    }
}
