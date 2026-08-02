package com.jy.eleaitender.common.interaction.util;

import com.jy.eleaitender.common.interaction.dto.AiTaskResultCallbackRequest;
import com.jy.eleaitender.common.interaction.dto.ExternalTokenRequest;
import com.jy.eleaitender.common.interaction.enums.InteractionResponseCode;
import com.jy.eleaitender.common.interaction.exception.InteractionException;

/**
 * 交互层参数校验工具。
 */
public final class InteractionValidationUtils {

    private InteractionValidationUtils() {
    }

    public static void validateReceiveAiTaskResult(AiTaskResultCallbackRequest request) {
        requireNotNull(request, "请求体不能为空");
        requireNotNull(request.getTaskId(), "任务ID不能为空");
        requireText(request.getTaskType(), "任务类型不能为空");
        requireText(request.getStatus(), "任务状态不能为空");
    }

    public static void validateExternalTokenRequest(ExternalTokenRequest request) {
        requireNotNull(request, "请求体不能为空");
        requireText(request.getUserName(), "用户名称不能为空");
        requireText(request.getUserId(), "用户ID不能为空");
        requireText(request.getEnterpriseName(), "企业名称不能为空");
        requireText(request.getEnterpriseId(), "企业ID不能为空");
        requireText(request.getEnterpriseCode(), "企业社会统一信用代码不能为空");
    }

    public static void validateFileId(Long fileId) {
        requireNotNull(fileId, "文件ID不能为空");
        if (fileId <= 0L) {
            throw new InteractionException(InteractionResponseCode.PARAM_ERROR, "文件ID必须大于0");
        }
    }

    public static void validateFileSha256(String sha256) {
        requireText(sha256, "文件SHA256不能为空");
    }

    public static void validateFileUploadParams(byte[] content, String fileName, String bizType) {
        if (content == null || content.length == 0) {
            throw new InteractionException(InteractionResponseCode.PARAM_ERROR, "文件内容不能为空");
        }
        requireText(fileName, "文件名不能为空");
        requireText(bizType, "业务类型不能为空");
    }

    private static void requireNotNull(Object value, String message) {
        if (value == null) {
            throw new InteractionException(InteractionResponseCode.PARAM_ERROR, message);
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new InteractionException(InteractionResponseCode.PARAM_ERROR, message);
        }
    }
}
