package com.jy.eleaitender.support.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jy.eleaitender.common.entity.support.SupMessage;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.common.security.SecurityContextHolder;
import com.jy.eleaitender.support.mapper.MessageMapper;
import com.jy.eleaitender.support.service.IMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * 消息中心服务实现
 */
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, SupMessage> implements IMessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Override
    public Page<SupMessage> getMyMessages(Integer pageNum, Integer pageSize,
                                          String messageType, Integer isRead) {
        Long userId = SecurityContextHolder.getUserId();
        Page<SupMessage> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SupMessage> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(SupMessage::getUserId, userId);
        if (StringUtils.hasText(messageType)) {
            wrapper.eq(SupMessage::getMessageType, messageType);
        }
        if (isRead != null) {
            wrapper.eq(SupMessage::getIsRead, isRead);
        }
        wrapper.orderByDesc(SupMessage::getCreateTime);

        return messageMapper.selectPage(page, wrapper);
    }

    @Override
    public long getUnreadCount() {
        Long userId = SecurityContextHolder.getUserId();
        LambdaQueryWrapper<SupMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupMessage::getUserId, userId)
               .eq(SupMessage::getIsRead, 0);
        return messageMapper.selectCount(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long id) {
        SupMessage message = messageMapper.selectById(id);
        if (message == null) {
            throw new BusinessException(ResponseCode.MESSAGE_NOT_FOUND);
        }
        if (message.getIsRead() == 0) {
            message.setIsRead(1);
            message.setReadTime(new Date());
            messageMapper.updateById(message);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead() {
        Long userId = SecurityContextHolder.getUserId();
        messageMapper.markAllRead(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        messageMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void send(SupMessage message) {
        if (message.getIsRead() == null) {
            message.setIsRead(0);
        }
        messageMapper.insert(message);
    }
}
