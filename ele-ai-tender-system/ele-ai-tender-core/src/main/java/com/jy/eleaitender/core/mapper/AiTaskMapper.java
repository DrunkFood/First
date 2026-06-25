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

    /**
     * 查询业务实体的活跃任务（PENDING/PROCESSING），用于防重复提交
     */
    @DataScope(skip = true)
    @Select("SELECT * FROM ai_task WHERE task_type = #{taskType} AND biz_id = #{bizId} " +
            "AND biz_type = #{bizType} AND status IN ('PENDING','PROCESSING') AND is_delete = 0 " +
            "LIMIT 1")
    AiTask selectActiveTask(@Param("taskType") String taskType,
                            @Param("bizId") Long bizId,
                            @Param("bizType") String bizType);

    /**
     * 查询业务实体的最新任务（不限状态），用于页面加载时展示上次任务状态
     */
    @DataScope(skip = true)
    @Select("SELECT * FROM ai_task WHERE task_type = #{taskType} AND biz_id = #{bizId} " +
            "AND biz_type = #{bizType} AND is_delete = 0 " +
            "ORDER BY create_time DESC LIMIT 1")
    AiTask selectLatestTask(@Param("taskType") String taskType,
                            @Param("bizId") Long bizId,
                            @Param("bizType") String bizType);

    /**
     * 查询已完成但未同步结果的AI任务
     * 跳过数据隔离：后台定时任务无用户上下文
     */
    @DataScope(skip = true)
    @Select("SELECT * FROM ai_task WHERE status IN ('COMPLETED','FAILED','AI_UNAVAILABLE','SKIPPED') " +
            "AND result_synced = 0 AND is_delete = 0 ORDER BY completed_at ASC LIMIT #{limit}")
    List<AiTask> selectUnsyncedTasks(@Param("limit") int limit);

    /**
     * 查询项目下是否存在活跃的AI任务（PENDING/PROCESSING）
     */
    @DataScope(skip = true)
    @Select("SELECT COUNT(*) FROM ai_task WHERE project_id = #{projectId} " +
            "AND status IN ('PENDING','PROCESSING') AND is_delete = 0")
    int countActiveTasksByProjectId(@Param("projectId") Long projectId);

    /**
     * 标记AI任务结果同步状态
     * 跳过数据隔离：后台定时任务无用户上下文
     */
    @DataScope(skip = true)
    @Update("UPDATE ai_task SET result_synced = #{synced} WHERE id = #{id} AND is_delete = 0")
    int markSynced(@Param("id") Long id, @Param("synced") int synced);

}
