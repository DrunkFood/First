package com.jy.eleaitender.ai.processor;

import com.jy.eleaitender.ai.checker.DetectionEngine;
import com.jy.eleaitender.ai.generator.RequirementGenerator;
import com.jy.eleaitender.ai.generator.ReviewItemGenerator;
import com.jy.eleaitender.ai.generator.TextOptimizer;
import com.jy.eleaitender.ai.mapper.AiTaskMapper;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.enums.AiTaskType;
import com.jy.eleaitender.common.exception.AiErrorContentException;
import com.jy.eleaitender.common.exception.AiUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI任务轮询处理器（定时任务）
 * 负责从ai_task表拉取PENDING任务并分发执行
 */
@Slf4j
@Component
public class AiTaskProcessor {

    @Autowired
    private AiTaskMapper aiTaskMapper;

    @Autowired
    private RequirementGenerator requirementGenerator;

    @Autowired
    private ReviewItemGenerator reviewItemGenerator;

    @Autowired
    private DetectionEngine detectionEngine;

    @Autowired
    private TextOptimizer textOptimizer;

    /**
     * 每5秒轮询待处理任务
     */
    @Scheduled(fixedDelay = 5000)
    public void processPendingTasks() {
        List<AiTask> tasks = aiTaskMapper.selectPendingTasks(10);
        log.info("待处理AI任务: {}", tasks.size());
        for (AiTask task : tasks) {
            // CAS更新状态为PROCESSING，防止并发
            int updated = aiTaskMapper.casUpdateStatus(task.getId(), "PENDING", "PROCESSING");
            if (updated == 0) {
                continue; // 已被其他实例抢占
            }
            log.info("开始处理AI任务: id={}, type={}", task.getId(), task.getTaskType());

            try {
                String result = dispatch(task);
                aiTaskMapper.markCompleted(task.getId(), result);
                log.info("AI任务处理完成: id={}, type={}", task.getId(), task.getTaskType());
            } catch (AiUnavailableException e) {
                log.error("AI服务不可用: id={}, type={}", task.getId(), task.getTaskType(), e);
                aiTaskMapper.markAiUnavailable(task.getId(), e.getMessage());
            } catch (AiErrorContentException e) {
                log.error("AI任务内容异常: id={}, type={}", task.getId(), task.getTaskType(), e);
                aiTaskMapper.markFailed(task.getId(), e.getContent(), truncateErrorMsg(e.getMessage()));
            } catch (Exception e) {
                log.error("AI任务处理失败: id={}, type={}", task.getId(), task.getTaskType(), e);
                aiTaskMapper.markFailed(task.getId(), truncateErrorMsg(e.getMessage()));
            }
        }
    }

    /**
     * 按任务类型分发到实际处理器
     */
    private String dispatch(AiTask task) {
        AiTaskType taskType = AiTaskType.fromCode(task.getTaskType());
        return switch (taskType) {
            case REQUIREMENT_GENERATE,
                 PROJECT_REQUIREMENT_GENERATE -> requirementGenerator.generate(task);
            case REVIEW_ITEM_GENERATE -> reviewItemGenerator.generate(task);
            case DETECTION_SENSITIVE_WORD,
                 DETECTION_TYPO,
                 DETECTION_POLICY_REVIEW,
                 DETECTION_FORMAT_CHECK -> detectionEngine.detect(task);
            case TEXT_OPTIMIZE -> textOptimizer.optimize(task);
        };
    }

    /**
     * 截断错误信息，防止超长文本写入数据库
     */
    private String truncateErrorMsg(String msg) {
        if (msg == null) return "未知错误";
        return msg.length() > 500 ? msg.substring(0, 500) : msg;
    }
}
