package com.jy.eleaitender.core.service;

import com.jy.eleaitender.common.dto.ai.AiTaskParams;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.enums.AiTaskType;
import com.jy.eleaitender.core.dto.response.AiTaskVO;

/**
 * AI任务服务接口
 */
public interface IAiTaskService {

    /**
     * 创建AI任务
     */
    AiTask createTask(AiTaskType type, Long projectId, Long bizId, String bizType,
                      AiTaskParams requestParams, String fileIds);

    /**
     * 创建AI任务
     */
    AiTask createTask(AiTaskType type, Long projectId, String bizId, String bizType,
                      AiTaskParams requestParams, String fileIds);

    /**
     * 查询任务状态
     */
    AiTaskVO getTaskStatus(Long taskId);

    /**
     * 用户跳过任务（降级为手动模式）
     */
    void skipTask(Long taskId);

    /**
     * 标记超时任务为AI_UNAVAILABLE（定时任务调用）
     */
    int markTimeoutTasks();

    /**
     * 查询业务实体的最新任务（不限状态），用于页面加载时展示上次任务状态
     */
    AiTaskVO getLatestTask(String taskType, Long bizId, String bizType);

    /**
     * 检查项目下是否存在活跃的AI任务（PENDING/PROCESSING）
     */
    boolean hasActiveTasks(Long projectId);
}
