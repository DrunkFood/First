package com.jy.eleaitender.core.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.support.SupMessage;

/**
 * 用户消息服务接口（Core模块）
 */
public interface IUserMessageService {

    /**
     * 分页查询当前用户消息
     */
    Page<SupMessage> getMyMessages(Integer pageNum, Integer pageSize, String messageType, Integer isRead);

    /**
     * 获取未读消息数
     */
    long getUnreadCount();

    /**
     * 标记已读
     */
    void markRead(Long id);

    /**
     * 全部标记已读
     */
    void markAllRead();

    /**
     * 删除消息
     */
    void deleteById(Long id);
}
