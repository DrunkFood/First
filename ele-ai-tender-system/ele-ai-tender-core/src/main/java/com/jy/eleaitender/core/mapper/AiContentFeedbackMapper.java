package com.jy.eleaitender.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.ai.AiContentFeedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * AI内容反馈 Mapper
 */
@Mapper
public interface AiContentFeedbackMapper extends BaseMapper<AiContentFeedback> {

    /**
     * 查询用户对某个目标的已有反馈
     */
    @Select("SELECT * FROM ai_content_feedback " +
            "WHERE create_id = #{userId} " +
            "AND (#{taskId} IS NULL AND task_id IS NULL OR task_id = #{taskId}) " +
            "AND feedback_scene = #{feedbackScene} " +
            "AND chat_message_id = #{chatMessageId} " +
            "AND is_delete = 0 LIMIT 1")
    AiContentFeedback selectByUserAndTarget(
            @Param("userId") Long userId,
            @Param("taskId") Long taskId,
            @Param("feedbackScene") String feedbackScene,
            @Param("chatMessageId") String chatMessageId);
}
