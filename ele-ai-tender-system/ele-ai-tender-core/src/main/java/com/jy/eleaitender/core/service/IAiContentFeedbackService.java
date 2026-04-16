package com.jy.eleaitender.core.service;

import com.jy.eleaitender.core.dto.request.FeedbackSubmitRequest;
import com.jy.eleaitender.core.dto.response.FeedbackVO;

/**
 * AI内容反馈 Service 接口
 */
public interface IAiContentFeedbackService {

    /**
     * 提交或更新反馈
     * 同一用户同一目标只保留一条反馈，不可撤销，但可切换类型
     */
    FeedbackVO submitFeedback(FeedbackSubmitRequest request);

    /**
     * 查询当前用户对某个目标的反馈状态
     */
    FeedbackVO getUserFeedback(Long taskId, String feedbackScene, String chatMessageId);
}
