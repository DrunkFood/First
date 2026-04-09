package com.jy.eletender.crypto.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jy.eletender.common.entity.crypto.BdcDecryptArtifact;
import com.jy.eletender.common.enums.DecryptArtifactStatus;
import com.jy.eletender.crypto.config.CryptoProperties;
import com.jy.eletender.crypto.mapper.BdcDecryptArtifactMapper;
import com.jy.eletender.crypto.service.IBidDecryptRequestService;
import com.jy.eletender.crypto.service.IDecryptArtifactService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
/**
 * 解密超时恢复任务。
 * 当 Stream 消息已 ack 但工件长期停留在 PROCESSING 时，依赖 `bdc_decrypt_artifact`
 * 的持久化状态做重试或失败收口，而不是走 Redis pending-list reclaim。
 */
public class DecryptTimeoutRecoveryScheduler {

    private final BdcDecryptArtifactMapper artifactMapper;
    private final IDecryptArtifactService artifactService;
    private final IBidDecryptRequestService decryptRequestService;
    private final StringRedisTemplate stringRedisTemplate;
    private final CryptoProperties cryptoProperties;

    public DecryptTimeoutRecoveryScheduler(BdcDecryptArtifactMapper artifactMapper,
                                           IDecryptArtifactService artifactService,
                                           IBidDecryptRequestService decryptRequestService,
                                           StringRedisTemplate stringRedisTemplate,
                                           CryptoProperties cryptoProperties) {
        this.artifactMapper = artifactMapper;
        this.artifactService = artifactService;
        this.decryptRequestService = decryptRequestService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.cryptoProperties = cryptoProperties;
    }

    @Scheduled(fixedDelayString = "#{@cryptoProperties.timeoutScanIntervalSeconds * 1000}")
    public void recoverTimeoutTasks() {
        Date timeoutBefore = new Date(System.currentTimeMillis() - cryptoProperties.getTaskTimeoutSeconds() * 1000L);
        // Stream messages are acked by the consumer, so timeout recovery is driven by persisted
        // artifact state rather than Redis pending entries.
        List<BdcDecryptArtifact> timedOutArtifacts = artifactMapper.selectList(new LambdaQueryWrapper<BdcDecryptArtifact>()
                .eq(BdcDecryptArtifact::getStatus, DecryptArtifactStatus.PROCESSING.name())
                .lt(BdcDecryptArtifact::getProcessingStartedAt, timeoutBefore));

        for (BdcDecryptArtifact artifact : timedOutArtifacts) {
            try {
                int retryCount = artifact.getRetryCount() == null ? 0 : artifact.getRetryCount();
                if (retryCount < cryptoProperties.getMaxRetryCount()) {
                    // Reset first so the next consumer run observes a clean retry state.
                    artifactService.resetForRetry(artifact.getId());
                    pushTask(artifact.getId());
                } else {
                    artifactService.markFailed(artifact.getId(), "解密任务超时且超过最大重试次数");
                    decryptRequestService.handleArtifactCompletion(artifact.getId());
                }
            } catch (Exception ex) {
                log.error("处理超时任务失败 artifactId={} message={}", artifact.getId(), ex.getMessage(), ex);
            }
        }
    }

    /**
     * 重新投递工件任务，让消费者再次执行解密。
     */
    private void pushTask(Long artifactId) {
        stringRedisTemplate.opsForStream().add(StreamRecords.newRecord()
                .in(cryptoProperties.getStream().getKey())
                .ofMap(Map.of("artifactId", String.valueOf(artifactId))));
    }
}
