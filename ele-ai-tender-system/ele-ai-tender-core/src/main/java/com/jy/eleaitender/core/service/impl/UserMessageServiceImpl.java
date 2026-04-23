package com.jy.eleaitender.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.support.SupMessage;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.common.security.SecurityContextHolder;
import com.jy.eleaitender.core.mapper.SupMessageMapper;
import com.jy.eleaitender.core.service.IUserMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * 用户消息服务实现
 */
@Service
public class UserMessageServiceImpl implements IUserMessageService {

    @Autowired
    private SupMessageMapper supMessageMapper;

    @Override
    public Page<SupMessage> getMyMessages(Integer pageNum, Integer pageSize, String messageType, Integer isRead) {
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
        return supMessageMapper.selectPage(page, wrapper);
    }

    @Override
    public long getUnreadCount() {
        Long userId = SecurityContextHolder.getUserId();
        LambdaQueryWrapper<SupMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupMessage::getUserId, userId)
                .eq(SupMessage::getIsRead, 0);
        return supMessageMapper.selectCount(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long id) {
        SupMessage msg = supMessageMapper.selectById(id);
        if (msg == null) {
            throw new BusinessException(ResponseCode.MESSAGE_NOT_FOUND);
        }
        msg.setIsRead(1);
        msg.setReadTime(new Date());
        supMessageMapper.updateById(msg);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead() {
        Long userId = SecurityContextHolder.getUserId();
        supMessageMapper.markAllRead(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        supMessageMapper.deleteById(id);
    }
}
