package com.jy.eletender.common.interaction.util;

import com.jy.eletender.common.interaction.dto.BidRecordSchemeQueryRequest;
import com.jy.eletender.common.interaction.dto.BidDecryptResultCallbackRequest;
import com.jy.eletender.common.interaction.dto.BidDecryptSubmitRequest;
import com.jy.eletender.common.interaction.dto.BidDocumentPushRequest;
import com.jy.eletender.common.interaction.dto.BidDocumentResultCallbackRequest;
import com.jy.eletender.common.interaction.dto.EnvelopeJoinRequest;
import com.jy.eletender.common.interaction.dto.ExternalTokenRequest;
import com.jy.eletender.common.interaction.dto.ProjectBasicInfoQueryRequest;
import com.jy.eletender.common.interaction.dto.CaKeysInfoQueryRequest;
import com.jy.eletender.common.interaction.dto.TenderEntryContext;
import com.jy.eletender.common.interaction.dto.TenderPackageCallbackRequest;
import com.jy.eletender.common.interaction.dto.TenderPdfCallbackRequest;
import com.jy.eletender.common.interaction.enums.InteractionResponseCode;
import com.jy.eletender.common.interaction.exception.InteractionException;

/**
 * 交互层参数校验工具。
 */
public final class InteractionValidationUtils {

    private InteractionValidationUtils() {
    }

    public static void validateExternalTokenRequest(ExternalTokenRequest request) {
        requireNotNull(request, "请求体不能为空");
        requireText(request.getUserName(), "用户名称不能为空");
        requireText(request.getUserId(), "用户ID不能为空");
        requireText(request.getEnterpriseName(), "企业名称不能为空");
        requireText(request.getEnterpriseId(), "企业ID不能为空");
        requireText(request.getEnterpriseCode(), "企业社会统一信用代码不能为空");
    }

    public static void validateProjectBasicInfoQueryRequest(ProjectBasicInfoQueryRequest request) {
        validateBizContext(request == null ? null : request.getBizType(),
                request == null ? null : request.getBizId(),
                request == null ? null : request.getProjectId(),
                request == null ? null : request.getTenderId());
    }

    public static void validateBidRecordSchemeQueryRequest(BidRecordSchemeQueryRequest request) {
        validateBizContext(request == null ? null : request.getBizType(),
                request == null ? null : request.getBizId(),
                request == null ? null : request.getProjectId(),
                request == null ? null : request.getTenderId());
    }

    public static void validateCaKeysInfoQueryRequest(CaKeysInfoQueryRequest request) {
        requireText(request == null ? null : request.getProjectId(), "项目ID不能为空");
        requireText(request == null ? null : request.getTenderId(), "标段ID不能为空");
    }

    public static void validateTenderPdfCallbackRequest(TenderPdfCallbackRequest request) {
        validateBizContext(request == null ? null : request.getBizType(),
                request == null ? null : request.getBizId(),
                request == null ? null : request.getProjectId(),
                request == null ? null : request.getTenderId());
        requireNotNull(request == null ? null : request.getFileId(), "文件ID不能为空");
        requireText(request == null ? null : request.getFileName(), "文件名称不能为空");
    }

    public static void validateTenderPackageCallbackRequest(TenderPackageCallbackRequest request) {
        validateBizContext(request == null ? null : request.getBizType(),
                request == null ? null : request.getBizId(),
                request == null ? null : request.getProjectId(),
                request == null ? null : request.getTenderId());
        requireNotNull(request == null ? null : request.getFileId(), "文件ID不能为空");
        requireText(request == null ? null : request.getFileName(), "文件名称不能为空");
    }

    public static void validateTenderEntryContext(TenderEntryContext context) {
        validateBizContext(context == null ? null : context.getBizType(),
                context == null ? null : context.getBizId(),
                context == null ? null : context.getProjectId(),
                context == null ? null : context.getTenderId());
        requireText(context == null ? null : context.getToken(), "Token不能为空");
    }

    public static void validateFileId(Long fileId) {
        requireNotNull(fileId, "文件ID不能为空");
        if (fileId.longValue() <= 0L) {
            throw new InteractionException(InteractionResponseCode.PARAM_ERROR, "文件ID必须大于0");
        }
    }

    public static void validateFileUploadParams(byte[] content, String fileName, String bizType) {
        if (content == null || content.length == 0) {
            throw new InteractionException(InteractionResponseCode.PARAM_ERROR, "文件内容不能为空");
        }
        requireText(fileName, "文件名不能为空");
        requireText(bizType, "业务类型不能为空");
    }

    public static void validateBidDocumentPushRequest(BidDocumentPushRequest request) {
        requireNotNull(request, "请求体不能为空");
        validateFileId(request.getFileId());
        requireText(request.getFileSha256(), "文件SHA-256不能为空");
    }

    public static void validateBidDecryptSubmitRequest(BidDecryptSubmitRequest request) {
        requireNotNull(request, "请求体不能为空");
        requireText(request.getProjectId(), "项目ID不能为空");
        requireText(request.getTenderId(), "标段ID不能为空");
        requireText(request.getBidRecordId(), "开标标录ID不能为空");
        requireText(request.getBidderPwdStr(), "投标文件口令不能为空");
        requireText(request.getFileSha256(), "文件SHA-256不能为空");
    }

    public static void validateBidDocumentResultCallbackRequest(BidDocumentResultCallbackRequest request) {
        requireNotNull(request, "请求体不能为空");
        validateFileId(request.getFileId());
        requireText(request.getUploadResult(), "上传结果不能为空");
    }

    public static void validateEnvelopeJoinRequest(EnvelopeJoinRequest request) {
        requireNotNull(request, "请求体不能为空");
        if (request.getHashKeyList() == null || request.getHashKeyList().isEmpty()) {
            throw new InteractionException(InteractionResponseCode.PARAM_ERROR, "hashKeyList不能为空");
        }
        for (EnvelopeJoinRequest.HashKeyItem item : request.getHashKeyList()) {
            requireNotNull(item, "hashKeyList条目不能为空");
            requireText(item.getCaId(), "caId不能为空");
            requireText(item.getHashKeyD(), "hashKeyD不能为空");
        }
    }

    public static void validateBidDecryptResultCallbackRequest(BidDecryptResultCallbackRequest request) {
        requireNotNull(request, "请求体不能为空");
        requireText(request.getBidRecordId(), "开标标录ID不能为空");
        requireText(request.getProjectId(), "项目ID不能为空");
        requireText(request.getTenderId(), "标段ID不能为空");
        requireText(request.getStatus(), "解密状态不能为空");
    }

    private static void validateBizContext(Integer bizType, String bizId, String projectId, String tenderId) {
        requireNotNull(bizType, "业务类型不能为空");
        requireText(bizId, "业务ID不能为空");
        requireText(projectId, "项目ID不能为空");
        requireText(tenderId, "标段ID不能为空");
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
