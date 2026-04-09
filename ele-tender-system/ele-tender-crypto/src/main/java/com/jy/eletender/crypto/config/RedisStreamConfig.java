package com.jy.eletender.crypto.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;

import jakarta.annotation.PostConstruct;
import java.util.Map;

@Slf4j
@Configuration
/**
 * Redis Stream 初始化配置。
 * 负责确保解密任务 Stream 与 consumer group 存在，避免首次投递时因组不存在而失败。
 */
public class RedisStreamConfig {

    private final StringRedisTemplate stringRedisTemplate;
    private final CryptoProperties cryptoProperties;

    public RedisStreamConfig(StringRedisTemplate stringRedisTemplate, CryptoProperties cryptoProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.cryptoProperties = cryptoProperties;
    }

    @PostConstruct
    public void init() {
        String key = cryptoProperties.getStream().getKey();
        String group = cryptoProperties.getStream().getGroup();

        try {
            // Stream 不存在时先写入一条占位消息，确保后续可以创建 consumer group。
            if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(key))) {
                stringRedisTemplate.opsForStream().add(StreamRecords.newRecord()
                        .in(key)
                        .ofMap(Map.of("bootstrap", "1")));
            }
            stringRedisTemplate.opsForStream().createGroup(key, ReadOffset.latest(), group);
        } catch (Exception ex) {
            if (ex.getMessage() == null || !ex.getMessage().contains("BUSYGROUP")) {
                log.warn("初始化Redis Stream失败 key={} group={} message={}", key, group, ex.getMessage());
            }
        }
    }
}
