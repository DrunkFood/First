package com.jy.eleaitender.core.controller;

import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.core.dto.request.FeedbackSubmitRequest;
import com.jy.eleaitender.core.dto.response.FeedbackVO;
import com.jy.eleaitender.core.service.IAiContentFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * AI内容反馈控制器
 */
@RestController
@RequestMapping("/api/v1/feedback")
@Tag(name = "AI内容反馈")
public class AiContentFeedbackController {

    @Autowired
    private IAiContentFeedbackService feedbackService;

    @PostMapping
    @RequireLogin
    @Operation(summary = "提交或更新反馈")
    public Result<FeedbackVO> submitFeedback(@RequestBody @Valid FeedbackSubmitRequest request) {
        return Result.success(feedbackService.submitFeedback(request));
    }

    @GetMapping
    @RequireLogin
    @Operation(summary = "查询当前用户对某个目标的反馈状态")
    public Result<FeedbackVO> getUserFeedback(
            @RequestParam(required = false) Long taskId,
            @RequestParam String feedbackScene,
            @RequestParam(required = false, defaultValue = "") String chatMessageId) {
        return Result.success(feedbackService.getUserFeedback(taskId, feedbackScene, chatMessageId));
    }
}
