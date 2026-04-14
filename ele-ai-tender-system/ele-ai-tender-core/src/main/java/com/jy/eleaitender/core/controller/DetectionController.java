package com.jy.eleaitender.core.controller;

import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.core.dto.request.DetectionSubmitRequest;
import com.jy.eleaitender.core.dto.response.DetectionProgressVO;
import com.jy.eleaitender.core.dto.response.DetectionReportVO;
import com.jy.eleaitender.core.service.IDetectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 检测流程控制器
 */
@RestController
@RequestMapping("/api/v1/detection")
@Tag(name = "智能检测")
public class DetectionController {

    @Autowired
    private IDetectionService detectionService;

    @PostMapping("/submit/{projectId}")
    @RequireLogin
    @Operation(summary = "提交最终文档检测")
    public Result<Map<String, Long>> submit(@PathVariable Long projectId,
                                            @RequestBody(required = false) DetectionSubmitRequest request) {
        return Result.success(detectionService.submit(projectId, request));
    }

    @GetMapping("/progress/{projectId}")
    @RequireLogin
    @Operation(summary = "获取检测进度")
    public Result<DetectionProgressVO> getProgress(@PathVariable Long projectId) {
        return Result.success(detectionService.getProgress(projectId));
    }

    @GetMapping("/report/{projectId}")
    @RequireLogin
    @Operation(summary = "获取检测报告")
    public Result<DetectionReportVO> getReport(@PathVariable Long projectId) {
        return Result.success(detectionService.getReport(projectId));
    }

    @PostMapping("/{recordId}/accept")
    @RequireLogin
    @Operation(summary = "接受检测建议")
    public Result<Void> acceptIssue(@PathVariable Long recordId) {
        detectionService.acceptIssue(recordId);
        return Result.success();
    }

    @PostMapping("/{recordId}/reject")
    @RequireLogin
    @Operation(summary = "拒绝检测建议")
    public Result<Void> rejectIssue(@PathVariable Long recordId) {
        detectionService.rejectIssue(recordId);
        return Result.success();
    }

    @PostMapping("/accept-all/{projectId}")
    @RequireLogin
    @Operation(summary = "一键接受所有建议")
    public Result<Void> acceptAll(@PathVariable Long projectId) {
        detectionService.acceptAll(projectId);
        return Result.success();
    }

    @PostMapping("/skip/{projectId}")
    @RequireLogin
    @Operation(summary = "跳过检测")
    public Result<Void> skip(@PathVariable Long projectId) {
        detectionService.skip(projectId);
        return Result.success();
    }

    @PostMapping("/retry/{projectId}")
    @RequireLogin
    @Operation(summary = "重新检测")
    public Result<Map<String, Long>> retry(@PathVariable Long projectId) {
        return Result.success(detectionService.retry(projectId));
    }
}
