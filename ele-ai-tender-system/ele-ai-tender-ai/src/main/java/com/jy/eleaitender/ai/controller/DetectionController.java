package com.jy.eleaitender.ai.controller;

import com.jy.eleaitender.ai.dto.request.DetectionRequest;
import com.jy.eleaitender.ai.dto.response.DetectionResultVO;
import com.jy.eleaitender.ai.entity.AiDetectionRecord;
import com.jy.eleaitender.ai.service.IDetectionService;
import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/detection")
@Tag(name = "智能检测")
public class DetectionController {

    @Autowired
    private IDetectionService detectionService;

    @PostMapping("/start")
    @Operation(summary = "启动检测")
    @RequireLogin
    public Result<AiDetectionRecord> start(@RequestBody @jakarta.validation.Valid DetectionRequest request) {
        return Result.success(detectionService.startDetection(request));
    }

    @GetMapping("/{id}/result")
    @Operation(summary = "获取检测结果")
    @RequireLogin
    public Result<DetectionResultVO> getResult(@PathVariable Long id) {
        return Result.success(detectionService.getResult(id));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "确认检测结果")
    @RequireLogin
    public Result<Void> confirm(@PathVariable Long id) {
        detectionService.confirmResult(id);
        return Result.success();
    }
}
