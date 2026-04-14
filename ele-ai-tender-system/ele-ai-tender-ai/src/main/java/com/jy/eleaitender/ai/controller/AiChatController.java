package com.jy.eleaitender.ai.controller;

import com.jy.eleaitender.ai.dto.request.ChatRequest;
import com.jy.eleaitender.ai.dto.request.OptimizeRequest;
import com.jy.eleaitender.ai.service.IAiChatService;
import com.jy.eleaitender.common.security.annotation.RequireLogin;
import com.jy.eleaitender.common.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI对话和文本优化控制器（SSE流式响应）
 */
@Tag(name = "AI助手", description = "AI对话、文本优化、AI建议")
@RestController
@RequestMapping("/api/v1/ai")
public class AiChatController {

    @Autowired
    private IAiChatService aiChatService;

    @Operation(summary = "AI对话（SSE流式响应）")
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequireLogin
    public SseEmitter chat(@RequestBody @Valid ChatRequest request) {
        SseEmitter emitter = new SseEmitter(60_000L);
        emitter.onTimeout(emitter::complete);
        aiChatService.streamChat(request, emitter);
        return emitter;
    }

    @Operation(summary = "文本优化（SSE流式响应）")
    @PostMapping(value = "/optimize", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequireLogin
    public SseEmitter optimize(@RequestBody @Valid OptimizeRequest request) {
        SseEmitter emitter = new SseEmitter(60_000L);
        emitter.onTimeout(emitter::complete);
        aiChatService.streamOptimize(request, emitter);
        return emitter;
    }

    @Operation(summary = "获取AI建议（同步响应）")
    @PostMapping("/suggest")
    @RequireLogin
    public Result<String> suggest(@RequestBody @Valid ChatRequest request) {
        return Result.success(aiChatService.suggest(request));
    }
}
