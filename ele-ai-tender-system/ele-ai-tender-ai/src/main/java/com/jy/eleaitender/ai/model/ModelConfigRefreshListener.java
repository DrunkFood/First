package com.jy.eleaitender.ai.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;

/**
 * 模型配置刷新监听器
 * 监听Redis Pub/Sub通道，收到支撑中心发出的刷新通知后清除本地缓存
 */
@Slf4j
@Component
public class ModelConfigRefreshListener {

    public static final String REFRESH_CHANNEL = "ai:config:refresh";

    @Autowired
    private ModelConfigCacheService cacheService;

    @Autowired
    private DynamicChatClientFactory clientFactory;

    /**
     * 配置Redis消息监听容器
     */
    @Bean
    public RedisMessageListenerContainer modelConfigRefreshContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
                new MessageListenerAdapter(this, "onRefreshMessage"),
                new ChannelTopic(REFRESH_CHANNEL)
        );
        return container;
    }

    /**
     * 收到刷新消息时清除所有缓存
     */
    public void onRefreshMessage(String message, String channel) {
        log.info("收到模型配置刷新通知: channel={}, message={}", channel, message);
        cacheService.evictAll();
        clientFactory.evictAll();
    }
}
