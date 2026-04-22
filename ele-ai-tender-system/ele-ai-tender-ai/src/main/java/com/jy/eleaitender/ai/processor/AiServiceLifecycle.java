package com.jy.eleaitender.ai.processor;

import com.jy.eleaitender.ai.mapper.AiTaskMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * AI服务生命周期管理
 * 启动时清理上次未完成的PROCESSING任务，关闭时标记当前PROCESSING任务为AI_UNAVAILABLE
 */
@Slf4j
@Component
public class AiServiceLifecycle {

    @Autowired
    private AiTaskMapper aiTaskMapper;

    @PostConstruct
    public void onStartup() {
        int count = aiTaskMapper.markAllProcessingAsAiUnavailable("AI服务重启，任务被中断");
        if (count > 0) {
            log.warn("AI服务启动，清理残留PROCESSING任务: {}条", count);
        }
    }

    @PreDestroy
    public void onShutdown() {
        int count = aiTaskMapper.markAllProcessingAsAiUnavailable("AI服务关闭，任务被中断");
        if (count > 0) {
            log.warn("AI服务关闭，标记PROCESSING任务为AI_UNAVAILABLE: {}条", count);
        }
    }
}
