package com.jy.eleaitender.core.controller.external;

import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.common.interaction.dto.AiTaskCreateRequest;
import com.jy.eleaitender.common.interaction.dto.AiTaskCreateResponse;
import com.jy.eleaitender.common.interaction.dto.AiTaskQueryResponse;
import com.jy.eleaitender.common.response.Result;
import com.jy.eleaitender.core.service.external.ExternalAiTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 外部AI任务API控制器
 * 供外部系统通过签名认证调用
 */
@RestController
@RequestMapping(value = "/api/external/ai-tasks", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "外部AI任务API")
public class ExternalAiTaskController {

    @Autowired
    private ExternalAiTaskService externalAiTaskService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "创建AI任务")
    public Result<AiTaskCreateResponse> createTask(@RequestBody AiTaskCreateRequest request) {
        return Result.success(externalAiTaskService.createTask(request));
    }

    @GetMapping("/{taskId}")
    @Operation(summary = "查询AI任务详情")
    public Result<AiTaskQueryResponse> getTask(@PathVariable Long taskId) {
        validateTaskId(taskId);
        return Result.success(externalAiTaskService.getTask(taskId));
    }

    @GetMapping("/{taskId}/status")
    @Operation(summary = "查询AI任务状态")
    public Result<AiTaskQueryResponse> getTaskStatus(@PathVariable Long taskId) {
        validateTaskId(taskId);
        return Result.success(externalAiTaskService.getTaskStatus(taskId));
    }

    private void validateTaskId(Long taskId) {
        if (taskId == null || taskId <= 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "任务ID必须大于0");
        }
    }
}
