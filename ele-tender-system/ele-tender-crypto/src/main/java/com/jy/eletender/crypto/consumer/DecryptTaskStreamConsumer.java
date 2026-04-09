package com.jy.eletender.crypto.consumer;

import com.jy.eletender.crypto.config.CryptoProperties;
import com.jy.eletender.crypto.service.IDecryptExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
/**
 * 解密任务 Stream 消费者。
 * 负责把 Redis Stream 中的 artifactId 交给执行器处理，真正的幂等与终态判断
 * 仍由 artifact 状态和请求投影逻辑兜底。
 */
public class DecryptTaskStreamConsumer {

    private final StringRedisTemplate stringRedisTemplate;
    private final IDecryptExecutor decryptExecutor;
    private final CryptoProperties cryptoProperties;

    public DecryptTaskStreamConsumer(StringRedisTemplate stringRedisTemplate,
                                     IDecryptExecutor decryptExecutor,
                                     CryptoProperties cryptoProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.decryptExecutor = decryptExecutor;
        this.cryptoProperties = cryptoProperties;
    }

    @Scheduled(fixedDelayString = "${crypto.stream-poll-interval-ms:1000}")
    public void consume() {
        String key = cryptoProperties.getStream().getKey();
        String group = cryptoProperties.getStream().getGroup();
        String consumerName = cryptoProperties.getStream().getConsumerName();

        // The consumer group gives us at-least-once delivery. Idempotency is handled downstream
        // by artifact status checks in the executor.
        List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                Consumer.from(group, consumerName),
                StreamReadOptions.empty()
                        .count(cryptoProperties.getStreamPollBatchSize())
                        .block(Duration.ofMillis(cryptoProperties.getStreamPollBlockMillis())),
                StreamOffset.create(key, ReadOffset.lastConsumed())
        );

        if (records == null || records.isEmpty()) {
            return;
        }

        for (MapRecord<String, Object, Object> record : records) {
            try {
                Object artifactIdValue = record.getValue().get("artifactId");
                if (artifactIdValue == null) {
                    // 非法消息直接跳过，避免阻塞后续正常任务。
                    continue;
                }
                decryptExecutor.execute(Long.parseLong(String.valueOf(artifactIdValue)));
            } catch (Exception ex) {
                log.error("消费解密任务失败 recordId={} message={}", record.getId().getValue(), ex.getMessage(), ex);
            } finally {
                // Ack is unconditional here: timeout recovery is handled by the scheduler against
                // PROCESSING artifacts instead of relying on pending-list reclaim logic.
                stringRedisTemplate.opsForStream().acknowledge(key, group, record.getId());
            }
        }
    }
}
