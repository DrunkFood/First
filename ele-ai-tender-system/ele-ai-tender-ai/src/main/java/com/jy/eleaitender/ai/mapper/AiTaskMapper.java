package com.jy.eleaitender.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.ai.AiTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * AI任务Mapper（AI模块）
 */
@Mapper
public interface AiTaskMapper extends BaseMapper<AiTask> {

    /**
     * 查询待处理任务
     */
    @Select("SELECT * FROM ai_task WHERE status = 'PENDING' AND is_delete = 0 " +
            "ORDER BY create_time ASC LIMIT #{limit}")
    List<AiTask> selectPendingTasks(@Param("limit") int limit);

    /**
     * CAS更新任务状态
     */
    @Update("UPDATE ai_task SET status = #{newStatus}, started_at = NOW() " +
            "WHERE id = #{id} AND status = #{oldStatus} AND is_delete = 0")
    int casUpdateStatus(@Param("id") Long id,
                        @Param("oldStatus") String oldStatus,
                        @Param("newStatus") String newStatus);

    /**
     * 标记任务完成
     */
    @Update("UPDATE ai_task SET status = 'COMPLETED', result = #{result}, completed_at = NOW() " +
            "WHERE id = #{id} AND is_delete = 0")
    int markCompleted(@Param("id") Long id, @Param("result") String result);

    /**
     * 标记任务失败
     */
    @Update("UPDATE ai_task SET status = 'FAILED', error_msg = #{errorMsg}, completed_at = NOW() " +
            "WHERE id = #{id} AND is_delete = 0")
    int markFailed(@Param("id") Long id, @Param("errorMsg") String errorMsg);

    /**
     * 标记AI服务不可用
     */
    @Update("UPDATE ai_task SET status = 'AI_UNAVAILABLE', error_msg = #{errorMsg}, completed_at = NOW() " +
            "WHERE id = #{id} AND is_delete = 0")
    int markAiUnavailable(@Param("id") Long id, @Param("errorMsg") String errorMsg);
}
