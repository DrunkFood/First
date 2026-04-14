package com.jy.eleaitender.core.helper;

import com.jy.eleaitender.common.entity.support.SupMessage;
import com.jy.eleaitender.common.enums.MessageBizType;
import com.jy.eleaitender.common.enums.MessageType;
import com.jy.eleaitender.core.mapper.SupMessageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 消息发送辅助类
 * 供其他Service调用，发送系统通知
 */
@Slf4j
@Component
public class MessageHelper {

    @Autowired
    private SupMessageMapper supMessageMapper;

    /**
     * 发送项目通知
     */
    public void sendProjectNotice(Long userId, String title, String content, Long projectId) {
        send(userId, title, content, MessageType.SYSTEM, MessageBizType.PROJECT, projectId);
    }

    /**
     * 发送检测通知
     */
    public void sendDetectionNotice(Long userId, String title, String content, Long projectId) {
        send(userId, title, content, MessageType.DETECTION, MessageBizType.DETECTION, projectId);
    }

    /**
     * 发送警告通知
     */
    public void sendWarningNotice(Long userId, String title, String content, Long bizId) {
        send(userId, title, content, MessageType.WARNING, MessageBizType.PROJECT, bizId);
    }

    private void send(Long userId, String title, String content,
                      MessageType messageType, MessageBizType bizType, Long bizId) {
        try {
            SupMessage msg = new SupMessage();
            msg.setUserId(userId);
            msg.setTitle(title);
            msg.setContent(content);
            msg.setMessageType(messageType.getCode());
            msg.setBizType(bizType.getCode());
            msg.setBizId(bizId);
            msg.setIsRead(0);
            supMessageMapper.insert(msg);
            log.info("发送消息: userId={}, title={}", userId, title);
        } catch (Exception e) {
            log.error("发送消息失败: userId={}, title={}", userId, title, e);
        }
    }
}
