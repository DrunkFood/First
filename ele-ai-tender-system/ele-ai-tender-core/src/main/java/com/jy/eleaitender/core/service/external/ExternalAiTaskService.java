package com.jy.eleaitender.core.service.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eleaitender.common.dto.ai.AiTaskParams;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.entity.support.SysAccessSystem;
import com.jy.eleaitender.common.enums.AiTaskType;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.common.interaction.dto.AiTaskCreateRequest;
import com.jy.eleaitender.common.interaction.dto.AiTaskCreateResponse;
import com.jy.eleaitender.common.interaction.dto.AiTaskQueryResponse;
import com.jy.eleaitender.common.entity.ai.AiTaskExternalCallback;
import com.jy.eleaitender.core.mapper.AiTaskExternalCallbackMapper;
import com.jy.eleaitender.core.mapper.SysAccessSystemQueryMapper;
import com.jy.eleaitender.core.service.IAiTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 外部AI任务适配服务
 * 将外部DTO转换为内部IAiTaskService调用参数
 */
@Slf4j
@Service
public class ExternalAiTaskService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private IAiTaskService aiTaskService;

    @Autowired
    private AiTaskExternalCallbackMapper callbackMapper;

    @Autowired
    private SysAccessSystemQueryMapper accessSystemQueryMapper;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 创建AI任务
     */
    public AiTaskCreateResponse createTask(String appKey, AiTaskCreateRequest request) {
        // 0. 参数校验
        if (request == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请求体不能为空");
        }
        if (!StringUtils.hasText(request.getTaskType())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "任务类型不能为空");
        }

        // 1. 转换任务类型
        AiTaskType taskType;
        try {
            taskType = AiTaskType.fromCode(request.getTaskType());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "未知的任务类型: " + request.getTaskType());
        }

        // 2. 反序列化请求参数
        AiTaskParams params = null;
        if (StringUtils.hasText(request.getRequestParams())) {
            try {
                params = (AiTaskParams) objectMapper.readValue(request.getRequestParams(), taskType.getClazz());
            } catch (Exception e) {
                throw new BusinessException(ResponseCode.PARAM_ERROR, "请求参数解析失败: " + e.getMessage());
            }
        }

        // 3. 创建任务（外部任务无项目上下文，projectId传null）
        AiTask task = aiTaskService.createTask(taskType, null, request.getBizId(), request.getBizType(),
                params, request.getFileIds());

        // 4. 始终创建回调记录（用于数据隔离和回调追踪）
        SysAccessSystem system = accessSystemQueryMapper.selectByAppKey(appKey);
        AiTaskExternalCallback callback = new AiTaskExternalCallback();
        callback.setTaskId(task.getId());
        callback.setAppKey(appKey);
        if (system != null && StringUtils.hasText(system.getSystemUrl())) {
            callback.setCallbackStatus("PENDING");
        } else {
            callback.setCallbackStatus("SUCCESS"); // 无回调URL，标记已完成，仅用于数据隔离
        }
        callback.setRetryCount(0);
        callbackMapper.insert(callback);
        log.info("创建外部回调记录: taskId={}, appKey={}, callbackStatus={}",
                task.getId(), appKey, callback.getCallbackStatus());

        // 5. 构建响应
        AiTaskCreateResponse response = new AiTaskCreateResponse();
        response.setTaskId(task.getId());
        response.setTaskType(task.getTaskType());
        response.setStatus(task.getStatus());
        response.setCreateTime(formatDate(task.getCreateTime()));
        return response;
    }

    /**
     * 查询任务详情
     */
    public AiTaskQueryResponse getTask(String appKey, Long taskId) {
        verifyOwnership(appKey, taskId);
        com.jy.eleaitender.core.dto.response.AiTaskVO vo = aiTaskService.getTaskStatus(taskId);
        return toQueryResponse(vo);
    }

    /**
     * 查询任务状态
     */
    public AiTaskQueryResponse getTaskStatus(String appKey, Long taskId) {
        return getTask(appKey, taskId);
    }

    /**
     * 校验数据隔离：确认任务属于指定外部系统
     */
    private void verifyOwnership(String appKey, Long taskId) {
        long count = callbackMapper.selectCount(
                new LambdaQueryWrapper<AiTaskExternalCallback>()
                        .eq(AiTaskExternalCallback::getTaskId, taskId)
                        .eq(AiTaskExternalCallback::getAppKey, appKey)
        );
        if (count == 0) {
            throw new BusinessException(ResponseCode.FORBIDDEN, "无权查询此任务");
        }
    }

    private AiTaskQueryResponse toQueryResponse(com.jy.eleaitender.core.dto.response.AiTaskVO vo) {
        AiTaskQueryResponse response = new AiTaskQueryResponse();
        response.setTaskId(vo.getId());
        response.setTaskType(vo.getTaskType());
        response.setBizId(vo.getBizId() != null ? String.valueOf(vo.getBizId()) : null);
        response.setBizType(vo.getBizType());
        response.setStatus(vo.getStatus());
        response.setStatusName(vo.getStatusName());
        response.setResult(vo.getResult());
        response.setErrorMsg(vo.getErrorMsg());
        response.setRetryCount(vo.getRetryCount());
        response.setMaxRetry(vo.getMaxRetry());
        response.setStartedAt(formatLocalDateTime(vo.getStartedAt()));
        response.setCompletedAt(formatLocalDateTime(vo.getCompletedAt()));
        response.setCreateTime(formatDate(vo.getCreateTime()));
        return response;
    }

    private String formatLocalDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_FORMATTER) : null;
    }

    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }
}
