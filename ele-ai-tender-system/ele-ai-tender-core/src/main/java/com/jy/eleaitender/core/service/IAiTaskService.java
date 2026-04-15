package com.jy.eleaitender.core.service;

import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.enums.AiTaskType;
import com.jy.eleaitender.core.dto.response.AiTaskVO;

import java.util.List;
import java.util.Map;

/**
 * AI任务服务接口
 */
public interface IAiTaskService {

    /**
     * 创建AI任务
     */
    AiTask createTask(AiTaskType type, Long projectId, Long bizId, String bizType,
                      Map<String, Object> requestParams, String fileIds);

    /**
     * 查询任务状态
     */
    AiTaskVO getTaskStatus(Long taskId);

    /**
     * 查询项目的所有任务
     */
    List<AiTaskVO> getTasksByProject(Long projectId);

    /**
     * 用户重试任务
     */
    void retryTask(Long taskId);

    /**
     * 用户跳过任务（降级为手动模式）
     */
    void skipTask(Long taskId);

    /**
     * 标记超时任务为AI_UNAVAILABLE（定时任务调用）
     */
    int markTimeoutTasks();

    /**
     * 查询业务实体的活跃任务（PENDING/PROCESSING），用于防重复提交和前端状态联动
     */
    AiTaskVO getActiveTask(String taskType, Long bizId, String bizType);

    /**
     * 查询项目的活跃任务（按项目维度，PENDING/PROCESSING）
     */
    List<AiTaskVO> getActiveTasksByProject(Long projectId);
}
