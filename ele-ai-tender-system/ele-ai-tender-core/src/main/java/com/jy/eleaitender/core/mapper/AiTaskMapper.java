package com.jy.eleaitender.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.datascope.DataScope;
import com.jy.eleaitender.common.entity.ai.AiTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * AI任务Mapper（Core模块）
 */
@Mapper
public interface AiTaskMapper extends BaseMapper<AiTask> {

    /**
     * CAS更新任务状态（防止并发重复处理）
     * 跳过数据隔离：后台任务调度无用户上下文
     */
    @DataScope(skip = true)
    @Update("UPDATE ai_task SET status = #{newStatus}, started_at = NOW() " +
            "WHERE id = #{id} AND status = #{oldStatus} AND is_delete = 0")
    int casUpdateStatus(@Param("id") Long id,
                        @Param("oldStatus") String oldStatus,
                        @Param("newStatus") String newStatus);

    /**
     * 查询待处理任务
     * 跳过数据隔离：后台任务调度无用户上下文
     */
    @DataScope(skip = true)
    @Select("SELECT * FROM ai_task WHERE status = 'PENDING' AND is_delete = 0 " +
            "ORDER BY create_time ASC LIMIT #{limit}")
    List<AiTask> selectPendingTasks(@Param("limit") int limit);

    /**
     * 标记超时任务为AI_UNAVAILABLE
     * 跳过数据隔离：后台定时任务无用户上下文
     */
    @DataScope(skip = true)
    @Update("UPDATE ai_task SET status = 'AI_UNAVAILABLE' " +
            "WHERE status = 'PENDING' AND is_delete = 0 " +
            "AND TIMESTAMPDIFF(MINUTE, create_time, NOW()) > timeout_minutes")
    int markTimeoutTasks();
}
