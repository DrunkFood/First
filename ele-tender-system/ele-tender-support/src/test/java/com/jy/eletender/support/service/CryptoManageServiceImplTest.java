package com.jy.eletender.support.service;

import com.jy.eletender.common.entity.crypto.BdcDecryptArtifact;
import com.jy.eletender.common.entity.crypto.BdcDecryptRequest;
import com.jy.eletender.common.enums.CallbackStatus;
import com.jy.eletender.common.enums.DecryptArtifactStatus;
import com.jy.eletender.common.enums.DecryptRequestStatus;
import com.jy.eletender.support.config.CryptoAdminProperties;
import com.jy.eletender.support.mapper.BdcDecryptArtifactMapper;
import com.jy.eletender.support.mapper.BdcDecryptRequestMapper;
import com.jy.eletender.support.service.impl.CryptoManageServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CryptoManageServiceImplTest {

    @Mock
    private BdcDecryptRequestMapper requestMapper;
    @Mock
    private BdcDecryptArtifactMapper artifactMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @Test
    void shouldResetRequestArtifactAndPushRetryTask() {
        CryptoAdminProperties properties = new CryptoAdminProperties();
        properties.setStreamKey("bdc:decrypt:tasks");

        CryptoManageServiceImpl service = new CryptoManageServiceImpl(requestMapper, artifactMapper, stringRedisTemplate, properties);

        BdcDecryptRequest request = new BdcDecryptRequest();
        request.setId(1L);
        request.setArtifactId(100L);
        request.setStatus(DecryptRequestStatus.FAILED.name());
        request.setCallbackStatus(CallbackStatus.FAILED.name());

        BdcDecryptArtifact artifact = new BdcDecryptArtifact();
        artifact.setId(100L);
        artifact.setStatus(DecryptArtifactStatus.FAILED.name());
        artifact.setRetryCount(1);

        when(requestMapper.selectById(1L)).thenReturn(request);
        when(artifactMapper.selectById(100L)).thenReturn(artifact);
        when(stringRedisTemplate.opsForStream()).thenReturn(streamOperations);

        service.retryRequest(1L);

        ArgumentCaptor<BdcDecryptRequest> requestCaptor = ArgumentCaptor.forClass(BdcDecryptRequest.class);
        verify(requestMapper).updateById(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getStatus()).isEqualTo(DecryptRequestStatus.PENDING.name());
        assertThat(requestCaptor.getValue().getCallbackStatus()).isEqualTo(CallbackStatus.NOT_CALLED.name());

        ArgumentCaptor<BdcDecryptArtifact> artifactCaptor = ArgumentCaptor.forClass(BdcDecryptArtifact.class);
        verify(artifactMapper).updateById(artifactCaptor.capture());
        assertThat(artifactCaptor.getValue().getStatus()).isEqualTo(DecryptArtifactStatus.PENDING.name());
        assertThat(artifactCaptor.getValue().getRetryCount()).isEqualTo(2);

        ArgumentCaptor<MapRecord<String, Object, Object>> streamCaptor = ArgumentCaptor.forClass(MapRecord.class);
        verify(streamOperations).add(streamCaptor.capture());
        Map<Object, Object> payload = streamCaptor.getValue().getValue();
        assertThat(payload.get("artifactId")).isEqualTo("100");
    }
}
