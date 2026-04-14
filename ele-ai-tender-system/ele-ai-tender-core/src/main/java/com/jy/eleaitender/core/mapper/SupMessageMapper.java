package com.jy.eleaitender.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.support.SupMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 消息Mapper（Core模块读写sup_message表）
 */
@Mapper
public interface SupMessageMapper extends BaseMapper<SupMessage> {

    /**
     * 批量标记用户所有消息为已读
     */
    @Update("UPDATE sup_message SET is_read = 1, read_time = NOW() " +
            "WHERE user_id = #{userId} AND is_read = 0 AND is_delete = 0")
    int markAllRead(@Param("userId") Long userId);
}
