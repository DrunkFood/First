package com.jy.eleaitender.ai.processor;

import com.jy.eleaitender.ai.mapper.AiTaskMapper;
import com.jy.eleaitender.ai.processor.checker.DetectionEngine;
import com.jy.eleaitender.ai.processor.generator.DocumentIntegration;
import com.jy.eleaitender.ai.processor.generator.RequirementGenerator;
import com.jy.eleaitender.ai.processor.generator.ReviewItemGenerator;
import com.jy.eleaitender.ai.processor.generator.TextOptimizer;
import com.jy.eleaitender.ai.threadpool.DynamicThreadPoolManager;
import com.jy.eleaitender.ai.threadpool.UserConcurrencyManager;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.enums.AiTaskType;
import com.jy.eleaitender.common.exception.AiErrorContentException;
import com.jy.eleaitender.common.exception.AiUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * AI任务轮询处理器（定时任务）
 * 负责从ai_task表拉取PENDING任务，经并发控制后提交到动态线程池执行
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
    private DocumentIntegration documentIntegration;

    @Autowired
    private TextOptimizer textOptimizer;

    @Autowired
    private DynamicThreadPoolManager threadPoolManager;

    @Autowired
    private UserConcurrencyManager concurrencyManager;

    /**
     * 每5秒轮询待处理任务
     */
    @Scheduled(fixedDelay = 5000)
    public void processPendingTasks() {
        List<AiTask> tasks = aiTaskMapper.selectPendingTasks(10);
        if (tasks.isEmpty()) {
            return;
        }
        log.debug("待处理AI任务: {}", tasks.size());

        for (AiTask task : tasks) {
            Long userId = task.getCreateId();
            if (userId == null || userId == 0) {
                int updated = aiTaskMapper.casUpdateStatus(task.getId(), "PENDING", "PROCESSING");
                if (updated == 0) {
                    return;
                }
                submitDirectly(task);
                continue;
            }

            // 检查发起数上限
            //if (!concurrencyManager.tryReserve(userId)) {
            //    log.warn("任务{}发起数超限，拒绝: userId={}", task.getId(), userId);
            //    continue;
            //}

            // 先获取并发许可，再CAS改状态，避免PROCESSING→PENDING弹跳
            if (!concurrencyManager.tryAcquire(userId)) {
                log.warn("任务{}并发数已满，排队等待: userId={}", task.getId(), userId);
                continue;
            }

            int updated = aiTaskMapper.casUpdateStatus(task.getId(), "PENDING", "PROCESSING");
            if (updated == 0) {
                // CAS失败（被其他实例抢占），归还许可
                concurrencyManager.release(userId);
                continue;
            }

            // 获得许可，提交到线程池执行
            submitWithConcurrencyControl(task);
        }
    }

    /**
     * 无并发控制的直接提交（兼容无用户信息的任务）
     */
    private void submitDirectly(AiTask task) {
        log.info("开始处理AI任务(直接): id={}, type={}", task.getId(), task.getTaskType());

        threadPoolManager.execute(() -> {
            try {
                String result = dispatch(task);
                aiTaskMapper.markCompleted(task.getId(), result);
                log.info("AI任务处理完成: id={}, type={}", task.getId(), task.getTaskType());
            } catch (AiUnavailableException e) {
                log.error("AI服务不可用: id={}, type={}", task.getId(), task.getTaskType(), e);
                aiTaskMapper.markAiUnavailable(task.getId(), e.getMessage());
            } catch (AiErrorContentException e) {
                log.error("AI任务内容异常: id={}, type={}", task.getId(), task.getTaskType(), e);
                aiTaskMapper.markAiErrorContent(task.getId(), e.getContent(), truncateErrorMsg(e.getMessage()));
            } catch (Exception e) {
                log.error("AI任务处理失败: id={}, type={}", task.getId(), task.getTaskType(), e);
                aiTaskMapper.markFailed(task.getId(), truncateErrorMsg(e.getMessage()));
            }
        });
    }

    /**
     * 带并发控制和超时的任务提交
     */
    private void submitWithConcurrencyControl(AiTask task) {
        Long userId = task.getCreateId();
        int timeout = threadPoolManager.getProperties().getTaskTimeoutMinutes();
        if (task.getTimeoutMinutes() != null && task.getTimeoutMinutes() > 0) {
            timeout = task.getTimeoutMinutes();
        }
        final int timeoutMinutes = timeout;

        log.info("开始处理AI任务: id={}, type={}, userId={}, timeout={}min", task.getId(), task.getTaskType(), userId, timeoutMinutes);

        Future<String> future = threadPoolManager.submit(() -> dispatch(task));

        threadPoolManager.execute(() -> {
            try {
                String result = future.get(timeoutMinutes, TimeUnit.MINUTES);
                aiTaskMapper.markCompleted(task.getId(), result);
                log.info("AI任务处理完成: id={}, type={}", task.getId(), task.getTaskType());
            } catch (TimeoutException e) {
                future.cancel(true);
                log.error("AI任务执行超时: id={}, type={}, timeout={}min", task.getId(), task.getTaskType(), timeoutMinutes);
                aiTaskMapper.markFailed(task.getId(), "任务执行超时(" + timeoutMinutes + "分钟)");
            } catch (Exception e) {
                Throwable cause = e.getCause();
                if (cause instanceof AiUnavailableException aue) {
                    log.error("AI服务不可用: id={}, type={}", task.getId(), task.getTaskType(), aue);
                    aiTaskMapper.markAiUnavailable(task.getId(), aue.getMessage());
                } else if (cause instanceof AiErrorContentException aece) {
                    log.error("AI任务内容异常: id={}, type={}", task.getId(), task.getTaskType(), aece);
                    aiTaskMapper.markAiErrorContent(task.getId(), aece.getContent(), truncateErrorMsg(aece.getMessage()));
                } else {
                    log.error("AI任务处理失败: id={}, type={}", task.getId(), task.getTaskType(), e);
                    aiTaskMapper.markFailed(task.getId(), truncateErrorMsg(e.getMessage()));
                }
            } finally {
                concurrencyManager.release(userId);
            }
        });
    }

    /**
     * 按任务类型分发到实际处理器
     */
    private String dispatch(AiTask task) {
        AiTaskType taskType = AiTaskType.fromCode(task.getTaskType());
        return switch (taskType) {
            // 文本优化
            case TEXT_OPTIMIZE -> textOptimizer.optimize(task);
            // 需求生成
            case REQUIREMENT_GENERATE,
                 PROJECT_REQUIREMENT_GENERATE -> requirementGenerator.generate(task);
            case REVIEW_ITEM_GENERATE -> reviewItemGenerator.generate(task);
            // 文档集成
            case DOCUMENT_INTEGRATION -> documentIntegration.integration(task);
            // 检测文档
            case DETECTION_SENSITIVE_WORD,
                 DETECTION_TYPO,
                 DETECTION_POLICY_REVIEW,
                 DETECTION_FORMAT_CHECK -> detectionEngine.detect(task);
        };
    }

    private String truncateErrorMsg(String msg) {
        if (msg == null) return "未知错误";
        return msg.length() > 500 ? msg.substring(0, 500) : msg;
    }
}
