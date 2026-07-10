package com.jy.eleaitender.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.ai.AiTaskExternalCallback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * AI任务外部回调记录Mapper
 */
@Mapper
public interface AiTaskExternalCallbackMapper extends BaseMapper<AiTaskExternalCallback> {

    /**
     * 查询待回调记录
     */
    @Select("SELECT * FROM ai_task_external_callback WHERE callback_status = 'PENDING' AND is_delete = 0 ORDER BY create_time ASC LIMIT #{limit}")
    List<AiTaskExternalCallback> selectPendingCallbacks(@Param("limit") int limit);

    /**
     * CAS更新状态为PROCESSING，防止多实例重复处理
     * @return 影响行数，1=成功获取，0=已被其他实例获取
     */
    @Update("UPDATE ai_task_external_callback SET callback_status='PROCESSING' WHERE id=#{id} AND callback_status='PENDING' AND is_delete=0")
    int casUpdateToProcessing(@Param("id") Long id);
}
