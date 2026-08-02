package com.jy.eleaitender.support.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jy.eleaitender.common.entity.support.SupMessage;

/**
 * 消息中心服务接口
 */
public interface IMessageService extends IService<SupMessage> {

    /**
     * 分页查询当前用户消息
     *
     * @param pageNum     页码
     * @param pageSize    每页条数
     * @param messageType 消息类型（可选）
     * @param isRead      是否已读（可选）
     * @return 分页结果
     */
    Page<SupMessage> getMyMessages(Integer pageNum, Integer pageSize,
                                   String messageType, Integer isRead);

    /**
     * 获取当前用户未读消息数量
     *
     * @return 未读数量
     */
    long getUnreadCount();

    /**
     * 标记单条消息为已读
     *
     * @param id 消息ID
     */
    void markRead(Long id);

    /**
     * 标记当前用户所有消息为已读
     */
    void markAllRead();

    /**
     * 删除消息（逻辑删除）
     *
     * @param id 消息ID
     */
    void deleteById(Long id);

    /**
     * 发送系统消息（供其他模块调用）
     *
     * @param message 消息实体
     */
    void send(SupMessage message);
}
