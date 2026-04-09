package com.jy.eletender.crypto.scheduler;

import com.jy.eletender.common.entity.crypto.BdcDecryptArtifact;
import com.jy.eletender.common.enums.DecryptArtifactStatus;
import com.jy.eletender.crypto.config.CryptoProperties;
import com.jy.eletender.crypto.mapper.BdcDecryptArtifactMapper;
import com.jy.eletender.crypto.service.IBidDecryptRequestService;
import com.jy.eletender.crypto.service.IDecryptArtifactService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DecryptTimeoutRecoverySchedulerTest {

    @Mock
    private BdcDecryptArtifactMapper artifactMapper;
    @Mock
    private IDecryptArtifactService artifactService;
    @Mock
    private IBidDecryptRequestService decryptRequestService;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @Test
    void shouldResetAndRetryWhenTimeoutAndRetryNotExhausted() {
        CryptoProperties properties = new CryptoProperties();
        properties.setTaskTimeoutSeconds(30);
        properties.setMaxRetryCount(3);
        properties.getStream().setKey("bdc:decrypt:tasks");

        DecryptTimeoutRecoveryScheduler scheduler = new DecryptTimeoutRecoveryScheduler(
                artifactMapper,
                artifactService,
                decryptRequestService,
                stringRedisTemplate,
                properties
        );

        BdcDecryptArtifact artifact = new BdcDecryptArtifact();
        artifact.setId(11L);
        artifact.setStatus(DecryptArtifactStatus.PROCESSING.name());
        artifact.setRetryCount(1);
        artifact.setProcessingStartedAt(new Date(System.currentTimeMillis() - 60_000L));

        when(artifactMapper.selectList(any())).thenReturn(List.of(artifact));
        when(stringRedisTemplate.opsForStream()).thenReturn(streamOperations);

        scheduler.recoverTimeoutTasks();

        verify(artifactService).resetForRetry(11L);
        verify(artifactService, never()).markFailed(any(), any());
        verify(decryptRequestService, never()).handleArtifactCompletion(any());

        ArgumentCaptor<MapRecord<String, Object, Object>> streamCaptor = ArgumentCaptor.forClass(MapRecord.class);
        verify(streamOperations).add(streamCaptor.capture());
        Map<Object, Object> payload = streamCaptor.getValue().getValue();
        assertThat(payload.get("artifactId")).isEqualTo("11");
    }

    @Test
    void shouldMarkFailedWhenTimeoutAndRetryExhausted() {
        CryptoProperties properties = new CryptoProperties();
        properties.setTaskTimeoutSeconds(30);
        properties.setMaxRetryCount(3);
        properties.getStream().setKey("bdc:decrypt:tasks");

        DecryptTimeoutRecoveryScheduler scheduler = new DecryptTimeoutRecoveryScheduler(
                artifactMapper,
                artifactService,
                decryptRequestService,
                stringRedisTemplate,
                properties
        );

        BdcDecryptArtifact artifact = new BdcDecryptArtifact();
        artifact.setId(12L);
        artifact.setStatus(DecryptArtifactStatus.PROCESSING.name());
        artifact.setRetryCount(3);
        artifact.setProcessingStartedAt(new Date(System.currentTimeMillis() - 60_000L));

        when(artifactMapper.selectList(any())).thenReturn(List.of(artifact));

        scheduler.recoverTimeoutTasks();

        verify(artifactService).markFailed(eq(12L), eq("解密任务超时且超过最大重试次数"));
        verify(decryptRequestService).handleArtifactCompletion(12L);
        verify(artifactService, never()).resetForRetry(any());
        verify(stringRedisTemplate, never()).opsForStream();
    }
}
