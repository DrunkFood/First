package com.jy.eleaitender.core.service.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eleaitender.common.dto.ai.AiTaskParams;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.entity.support.SysAccessSystem;
import com.jy.eleaitender.common.enums.AiTaskType;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.AiSyncedException;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.common.interaction.dto.AiTaskCreateRequest;
import com.jy.eleaitender.common.interaction.dto.AiTaskCreateResponse;
import com.jy.eleaitender.common.interaction.dto.AiTaskQueryResponse;
import com.jy.eleaitender.common.security.SecurityContextHolder;
import com.jy.eleaitender.core.dto.response.AiTaskVO;
import com.jy.eleaitender.core.mapper.SysAccessSystemQueryMapper;
import com.jy.eleaitender.core.service.IAiTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 外部AI任务适配服务
 * 将外部DTO转换为内部IAiTaskService调用参数
 */
@Slf4j
@Service
public class ExternalAiTaskService {

    @Autowired
    private IAiTaskService aiTaskService;

    @Autowired
    private SysAccessSystemQueryMapper accessSystemQueryMapper;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 创建AI任务
     */
    public AiTaskCreateResponse createTask(AiTaskCreateRequest request) {
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

        // 4. 查询外部系统信息
        SysAccessSystem system = accessSystemQueryMapper.selectById(SecurityContextHolder.getSystemId());
        if (system == null || !StringUtils.hasText(system.getSystemUrl())) {
            throw new AiSyncedException("外部系统不存在或未配置system_url");
        }

        // 5. 创建任务（外部任务无项目上下文，projectId传null）
        AiTask task = aiTaskService.createExternalTask(taskType, system.getId(), null, request.getBizId(), request.getBizType(),
                params, request.getFileIds());

        // 6. 构建响应
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
    public AiTaskQueryResponse getTask(Long taskId) {
        AiTaskVO vo = aiTaskService.getTaskStatus(taskId);
        return toQueryResponse(vo);
    }

    /**
     * 查询任务状态
     */
    public AiTaskQueryResponse getTaskStatus(Long taskId) {
        return getTask(taskId);
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
        response.setStartedAt(formatDate(vo.getStartedAt()));
        response.setCompletedAt(formatDate(vo.getCompletedAt()));
        response.setCreateTime(formatDate(vo.getCreateTime()));
        return response;
    }

    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }
}
