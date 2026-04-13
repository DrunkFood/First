package com.jy.eleaitender.interaction.autoconfigure.logging;

import com.jy.eleaitender.common.interaction.dto.InteractionResult;
import com.jy.eleaitender.common.interaction.spi.InteractionEventLogger;
import com.jy.eleaitender.interaction.core.support.InteractionLogContext;
import com.jy.eleaitender.interaction.core.support.InteractionLogContextExtractor;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认交互事件日志实现。
 */
@Slf4j
public class DefaultInteractionEventLogger implements InteractionEventLogger {

    @Override
    /**
     * 记录电子标系统调业务系统的入站交互日志。
     */
    public void logInbound(String apiName, Object request, Object response, Throwable error) {
        InteractionLogContext context = InteractionLogContextExtractor.extract(apiName, request, response);
        log("IN", context, response, error);
    }

    @Override
    /**
     * 记录业务系统调电子标系统的出站交互日志。
     */
    public void logOutbound(String apiName, Object request, Object response, Throwable error) {
        InteractionLogContext context = InteractionLogContextExtractor.extract(apiName, request, response);
        log("OUT", context, response, error);
    }

    /**
     * 统一按固定字段输出日志，方便日志平台基于方向、业务键、文件键做检索。
     */
    private void log(String direction, InteractionLogContext context, Object response, Throwable error) {
        boolean success = error == null && isResponseSuccessful(response);
        String errorType = error == null ? "-" : error.getClass().getSimpleName();
        String errorMessage = error == null ? "-" : safe(error.getMessage());
        String responseCode = resolveResponseCode(response);
        String pattern = "INTERACTION {} traceId={} api={} success={} responseCode={} bizType={} bizId={} projectId={} tenderId={} fileId={} fileName={} userId={} userName={} enterpriseId={} enterpriseName={} appKey={} requestType={} responseType={} errorType={} errorMessage={}";
        if (error == null) {
            log.info(pattern,
                    direction,
                    safe(context.getTraceId()),
                    safe(context.getApiName()),
                    success,
                    responseCode,
                    safe(context.getBizType()),
                    safe(context.getBizId()),
                    safe(context.getProjectId()),
                    safe(context.getTenderId()),
                    safe(context.getFileId()),
                    safe(context.getFileName()),
                    safe(context.getUserId()),
                    safe(context.getUserName()),
                    safe(context.getEnterpriseId()),
                    safe(context.getEnterpriseName()),
                    safe(context.getAppKey()),
                    safe(context.getRequestType()),
                    safe(context.getResponseType()),
                    errorType,
                    errorMessage);
            return;
        }
        log.warn(pattern,
                direction,
                safe(context.getTraceId()),
                safe(context.getApiName()),
                success,
                responseCode,
                safe(context.getBizType()),
                safe(context.getBizId()),
                safe(context.getProjectId()),
                safe(context.getTenderId()),
                safe(context.getFileId()),
                safe(context.getFileName()),
                safe(context.getUserId()),
                safe(context.getUserName()),
                safe(context.getEnterpriseId()),
                safe(context.getEnterpriseName()),
                safe(context.getAppKey()),
                safe(context.getRequestType()),
                safe(context.getResponseType()),
                errorType,
                errorMessage);
    }

    /**
     * 支持整数 HTTP 状态码和 InteractionResult 两种响应形态。
     */
    private boolean isResponseSuccessful(Object response) {
        if (response == null) {
            return true;
        }
        if (response instanceof Integer) {
            return ((Integer) response).intValue() < 400;
        }
        if (response instanceof InteractionResult) {
            return ((InteractionResult<?>) response).isSuccess();
        }
        return true;
    }

    /**
     * 统一提取可打印的响应码，避免日志里出现多种格式。
     */
    private String resolveResponseCode(Object response) {
        if (response instanceof Integer) {
            return String.valueOf(response);
        }
        if (response instanceof InteractionResult) {
            return String.valueOf(((InteractionResult<?>) response).getCode());
        }
        return "-";
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }
}
