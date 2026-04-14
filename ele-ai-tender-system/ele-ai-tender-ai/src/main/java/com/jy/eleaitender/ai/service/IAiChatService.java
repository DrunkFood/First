package com.jy.eleaitender.ai.service;

import com.jy.eleaitender.ai.dto.request.ChatRequest;
import com.jy.eleaitender.ai.dto.request.OptimizeRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI对话服务接口
 */
public interface IAiChatService {

    /**
     * 流式AI对话
     */
    void streamChat(ChatRequest request, SseEmitter emitter);

    /**
     * 流式文本优化
     */
    void streamOptimize(OptimizeRequest request, SseEmitter emitter);

    /**
     * 同步获取AI建议
     */
    String suggest(ChatRequest request);
}
