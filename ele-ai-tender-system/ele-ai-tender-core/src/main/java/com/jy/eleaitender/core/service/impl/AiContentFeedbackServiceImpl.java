package com.jy.eleaitender.core.service.impl;

import com.jy.eleaitender.common.entity.ai.AiContentFeedback;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.enums.FeedbackScene;
import com.jy.eleaitender.common.enums.FeedbackType;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.common.security.SecurityContextHolder;
import com.jy.eleaitender.core.dto.request.FeedbackSubmitRequest;
import com.jy.eleaitender.core.dto.response.FeedbackVO;
import com.jy.eleaitender.core.mapper.AiContentFeedbackMapper;
import com.jy.eleaitender.core.mapper.AiTaskMapper;
import com.jy.eleaitender.core.service.IAiContentFeedbackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI内容反馈服务实现
 */
@Slf4j
@Service
public class AiContentFeedbackServiceImpl implements IAiContentFeedbackService {

    @Autowired
    private AiContentFeedbackMapper feedbackMapper;

    @Autowired
    private AiTaskMapper aiTaskMapper;

    @Override
    @Transactional
    public FeedbackVO submitFeedback(FeedbackSubmitRequest request) {
        Long userId = SecurityContextHolder.getUserId();

        // 参数校验
        validateRequest(request);

        String chatMessageId = request.getChatMessageId() != null ? request.getChatMessageId() : "";

        // 查询已有反馈
        AiContentFeedback existing = feedbackMapper.selectByUserAndTarget(
                userId, request.getTaskId(), request.getFeedbackScene(), chatMessageId);

        if (existing != null) {
            // 已反馈，不可修改（幂等返回同类型，拒绝不同类型）
            if (existing.getFeedbackType().equals(request.getFeedbackType())) {
                return toVO(existing);
            }
            throw new BusinessException(ResponseCode.FEEDBACK_ALREADY_EXISTS);
        }

        // 新增反馈
        AiContentFeedback feedback = new AiContentFeedback();
        feedback.setTaskId(request.getTaskId());
        feedback.setFeedbackType(request.getFeedbackType());
        feedback.setFeedbackScene(request.getFeedbackScene());
        feedback.setChatMessageId(chatMessageId);
        feedback.setChatContent(request.getChatContent());
        feedback.setReason(request.getReason());
        feedbackMapper.insert(feedback);
        return toVO(feedback);
    }

    @Override
    public FeedbackVO getUserFeedback(Long taskId, String feedbackScene, String chatMessageId) {
        Long userId = SecurityContextHolder.getUserId();
        String chatId = chatMessageId != null ? chatMessageId : "";
        AiContentFeedback feedback = feedbackMapper.selectByUserAndTarget(
                userId, taskId, feedbackScene, chatId);
        return feedback != null ? toVO(feedback) : null;
    }

    private void validateRequest(FeedbackSubmitRequest request) {
        // 校验反馈类型
        try {
            FeedbackType.fromCode(request.getFeedbackType());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "无效的反馈类型: " + request.getFeedbackType());
        }
        // 校验反馈场景
        try {
            FeedbackScene.fromCode(request.getFeedbackScene());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "无效的反馈场景: " + request.getFeedbackScene());
        }
        // 生成内容反馈必须有 taskId
        if (FeedbackScene.GENERATION_CONTENT.getCode().equals(request.getFeedbackScene())
                && request.getTaskId() == null) {
            throw new BusinessException(ResponseCode.FEEDBACK_TARGET_NOT_FOUND, "生成内容反馈必须关联AI任务");
        }
        // 聊天反馈必须有 chatMessageId
        if (FeedbackScene.CHAT_MESSAGE.getCode().equals(request.getFeedbackScene())
                && (request.getChatMessageId() == null || request.getChatMessageId().isBlank())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "聊天反馈必须指定消息标识");
        }
        // 如有 taskId，验证任务存在
        if (request.getTaskId() != null) {
            AiTask task = aiTaskMapper.selectById(request.getTaskId());
            if (task == null) {
                throw new BusinessException(ResponseCode.TASK_NOT_FOUND);
            }
        }
    }

    private FeedbackVO toVO(AiContentFeedback feedback) {
        FeedbackVO vo = new FeedbackVO();
        vo.setId(feedback.getId());
        vo.setFeedbackType(feedback.getFeedbackType());
        vo.setFeedbackScene(feedback.getFeedbackScene());
        vo.setReason(feedback.getReason());
        return vo;
    }
}
