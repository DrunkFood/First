package com.jy.eleaitender.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.enums.AiTaskStatus;
import com.jy.eleaitender.common.enums.AiTaskType;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.core.dto.response.AiTaskVO;
import com.jy.eleaitender.core.mapper.AiTaskMapper;
import com.jy.eleaitender.core.service.IAiTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * AI任务服务实现
 */
@Slf4j
@Service
public class AiTaskServiceImpl implements IAiTaskService {

    @Autowired
    private AiTaskMapper aiTaskMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional
    public AiTask createTask(AiTaskType type, Long projectId, Long bizId, String bizType,
                             Map<String, Object> requestParams, String fileIds) {
        // 防重复提交：同一业务同一类型不能有活跃任务
        AiTask activeTask = aiTaskMapper.selectActiveTask(type.getCode(), bizId, bizType);
        if (activeTask != null) {
            throw new BusinessException(ResponseCode.TASK_ALREADY_PROCESSING);
        }

        AiTask task = new AiTask();
        task.setTaskType(type.getCode());
        task.setProjectId(projectId);
        task.setBizId(bizId);
        task.setBizType(bizType);
        task.setFileIds(fileIds);
        task.setStatus(AiTaskStatus.PENDING.getCode());
        task.setRetryCount(0);
        task.setMaxRetry(3);
        task.setTimeoutMinutes(10);

        try {
            task.setRequestParams(objectMapper.writeValueAsString(requestParams));
        } catch (Exception e) {
            log.error("序列化请求参数失败", e);
            task.setRequestParams("{}");
        }

        aiTaskMapper.insert(task);
        log.info("创建AI任务: id={}, type={}, projectId={}, bizId={}", task.getId(), type.getCode(), projectId, bizId);
        return task;
    }

    @Override
    public AiTaskVO getTaskStatus(Long taskId) {
        AiTask task = aiTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ResponseCode.TASK_NOT_FOUND);
        }
        return toVO(task);
    }

    @Override
    @Transactional
    public void skipTask(Long taskId) {
        AiTask task = aiTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ResponseCode.TASK_NOT_FOUND);
        }

        AiTaskStatus currentStatus = AiTaskStatus.fromCode(task.getStatus());
        if (currentStatus.isTerminal() && currentStatus != AiTaskStatus.FAILED && currentStatus != AiTaskStatus.AI_UNAVAILABLE) {
            throw new BusinessException(ResponseCode.TASK_STATUS_ERROR,
                    "当前状态[" + currentStatus.getLabel() + "]不支持跳过");
        }

        task.setStatus(AiTaskStatus.SKIPPED.getCode());
        aiTaskMapper.updateById(task);
        log.info("跳过AI任务: id={}", taskId);
    }

    @Override
    public int markTimeoutTasks() {
        int count = aiTaskMapper.markTimeoutTasks();
        if (count > 0) {
            log.warn("标记{}个超时AI任务为AI_UNAVAILABLE", count);
        }
        return count;
    }

    @Override
    public AiTaskVO getLatestTask(String taskType, Long bizId, String bizType) {
        AiTask task = aiTaskMapper.selectLatestTask(taskType, bizId, bizType);
        return task != null ? toVO(task) : null;
    }

    private AiTaskVO toVO(AiTask task) {
        AiTaskVO vo = new AiTaskVO();
        vo.setId(task.getId());
        vo.setTaskType(task.getTaskType());
        vo.setProjectId(task.getProjectId());
        vo.setBizId(task.getBizId());
        vo.setBizType(task.getBizType());
        vo.setStatus(task.getStatus());
        vo.setResult(task.getResult());
        vo.setErrorMsg(task.getErrorMsg());
        vo.setRetryCount(task.getRetryCount());
        vo.setMaxRetry(task.getMaxRetry());
        vo.setStartedAt(task.getStartedAt());
        vo.setCompletedAt(task.getCompletedAt());
        vo.setCreateTime(task.getCreateTime());
        vo.setResultSynced(task.getResultSynced());

        try {
            AiTaskType taskType = AiTaskType.fromCode(task.getTaskType());
            vo.setTaskTypeName(taskType.getLabel());
        } catch (Exception e) {
            vo.setTaskTypeName(task.getTaskType());
        }

        try {
            AiTaskStatus taskStatus = AiTaskStatus.fromCode(task.getStatus());
            vo.setStatusName(taskStatus.getLabel());
        } catch (Exception e) {
            vo.setStatusName(task.getStatus());
        }

        return vo;
    }
}
