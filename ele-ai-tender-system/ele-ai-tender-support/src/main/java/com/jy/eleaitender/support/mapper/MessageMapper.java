package com.jy.eleaitender.support.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jy.eleaitender.common.entity.support.SupMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 系统消息Mapper
 */
@Mapper
public interface MessageMapper extends BaseMapper<SupMessage> {

    /**
     * 批量标记用户所有消息为已读
     *
     * @param userId 用户ID
     * @return 更新行数
     */
    @Update("UPDATE sup_message SET is_read = 1, read_time = NOW() " +
            "WHERE user_id = #{userId} AND is_read = 0 AND is_delete = 0")
    int markAllRead(@Param("userId") Long userId);
}
