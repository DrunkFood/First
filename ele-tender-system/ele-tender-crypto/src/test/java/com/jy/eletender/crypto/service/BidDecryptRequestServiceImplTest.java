package com.jy.eletender.crypto.service;

import com.jy.eletender.common.entity.crypto.BdcBidDocument;
import com.jy.eletender.common.entity.crypto.BdcDecryptArtifact;
import com.jy.eletender.common.entity.crypto.BdcDecryptRequest;
import com.jy.eletender.common.interaction.dto.BidDecryptSubmitRequest;
import com.jy.eletender.common.interaction.dto.BidDecryptSubmitResponse;
import com.jy.eletender.crypto.config.CryptoProperties;
import com.jy.eletender.crypto.mapper.BdcBidDocumentMapper;
import com.jy.eletender.crypto.mapper.BdcDecryptArtifactMapper;
import com.jy.eletender.crypto.mapper.BdcDecryptRequestMapper;
import com.jy.eletender.crypto.service.impl.BidDecryptRequestServiceImpl;
import com.jy.eletender.crypto.support.CryptoFingerprintUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BidDecryptRequestServiceImplTest {

    @Mock
    private BdcDecryptRequestMapper requestMapper;
    @Mock
    private BdcDecryptArtifactMapper artifactMapper;
    @Mock
    private BdcBidDocumentMapper bidDocumentMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @Test
    void shouldCreateArtifactRequestAndPushStreamTask() {
        CryptoProperties properties = new CryptoProperties();
        properties.setBidderPwdFingerprintSecret("fingerprint-secret");
        properties.getStream().setKey("bdc:decrypt:tasks");
        CryptoFingerprintUtil.configure(properties.getBidderPwdFingerprintSecret());

        BidDecryptRequestServiceImpl service = new BidDecryptRequestServiceImpl(
                requestMapper,
                artifactMapper,
                bidDocumentMapper,
                stringRedisTemplate,
                properties
        );

        BidDecryptSubmitRequest request = new BidDecryptSubmitRequest();
        request.setProjectId("P1");
        request.setTenderId("T1");
        request.setBidRecordId("B1");
        request.setFileSha256("sha-001");
        request.setBidderPwdStr("pwd-001");

        BdcBidDocument bidDocument = new BdcBidDocument();
        bidDocument.setAppKey("app-a");
        bidDocument.setFileSha256("sha-001");

        when(bidDocumentMapper.selectOne(any())).thenReturn(bidDocument);
        when(artifactMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            BdcDecryptArtifact arg = invocation.getArgument(0);
            arg.setId(101L);
            return 1;
        }).when(artifactMapper).insert(any(BdcDecryptArtifact.class));
        doAnswer(invocation -> {
            BdcDecryptRequest arg = invocation.getArgument(0);
            arg.setId(202L);
            return 1;
        }).when(requestMapper).insert(any(BdcDecryptRequest.class));

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForStream()).thenReturn(streamOperations);

        BidDecryptSubmitResponse response = service.submit("app-a", "u1", "张三", "e1", request);

        assertThat(response.getRecordId()).isEqualTo("202");

        ArgumentCaptor<BdcDecryptArtifact> artifactCaptor = ArgumentCaptor.forClass(BdcDecryptArtifact.class);
        verify(artifactMapper).insert(artifactCaptor.capture());
        assertThat(artifactCaptor.getValue().getBidderPwdFingerprint())
                .isEqualTo("afe2b9ed40eb25cb1484c05b25540248af0fd2e823f8c493c771274f025fe379");

        verify(valueOperations).set(org.mockito.ArgumentMatchers.startsWith("crypto:pwd:101"), org.mockito.ArgumentMatchers.eq("pwd-001"), org.mockito.ArgumentMatchers.any());

        ArgumentCaptor<MapRecord<String, Object, Object>> streamCaptor = ArgumentCaptor.forClass(MapRecord.class);
        verify(streamOperations).add(streamCaptor.capture());
        Map<Object, Object> payload = streamCaptor.getValue().getValue();
        assertThat(payload.get("artifactId")).isEqualTo("101");
    }

    @Test
    void shouldReuseExistingArtifactWhenFingerprintMatches() {
        CryptoProperties properties = new CryptoProperties();
        properties.setBidderPwdFingerprintSecret("fingerprint-secret");
        properties.getStream().setKey("bdc:decrypt:tasks");
        CryptoFingerprintUtil.configure(properties.getBidderPwdFingerprintSecret());

        BidDecryptRequestServiceImpl service = new BidDecryptRequestServiceImpl(
                requestMapper,
                artifactMapper,
                bidDocumentMapper,
                stringRedisTemplate,
                properties
        );

        BidDecryptSubmitRequest request = new BidDecryptSubmitRequest();
        request.setProjectId("P2");
        request.setTenderId("T2");
        request.setBidRecordId("B2");
        request.setFileSha256("sha-002");
        request.setBidderPwdStr("pwd-002");

        BdcBidDocument bidDocument = new BdcBidDocument();
        bidDocument.setAppKey("app-b");
        bidDocument.setFileSha256("sha-002");

        BdcDecryptArtifact artifact = new BdcDecryptArtifact();
        artifact.setId(303L);
        artifact.setAppKey("app-b");
        artifact.setFileSha256("sha-002");
        artifact.setBidderPwdFingerprint("58965b9838575b4ab6ee1a1c821eedec38cf8cad309e45e2e8cc330363879f6a");

        when(bidDocumentMapper.selectOne(any())).thenReturn(bidDocument);
        when(artifactMapper.selectOne(any())).thenReturn(artifact);
        doAnswer(invocation -> {
            BdcDecryptRequest arg = invocation.getArgument(0);
            arg.setId(404L);
            return 1;
        }).when(requestMapper).insert(any(BdcDecryptRequest.class));

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForStream()).thenReturn(streamOperations);

        BidDecryptSubmitResponse response = service.submit("app-b", "u2", "李四", "e2", request);

        assertThat(response.getRecordId()).isEqualTo("404");
        verify(artifactMapper, never()).insert(any(BdcDecryptArtifact.class));

        ArgumentCaptor<BdcDecryptRequest> requestCaptor = ArgumentCaptor.forClass(BdcDecryptRequest.class);
        verify(requestMapper).insert(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getArtifactId()).isEqualTo(303L);

        verify(valueOperations).set(org.mockito.ArgumentMatchers.startsWith("crypto:pwd:303"), org.mockito.ArgumentMatchers.eq("pwd-002"), org.mockito.ArgumentMatchers.any());

        ArgumentCaptor<MapRecord<String, Object, Object>> streamCaptor = ArgumentCaptor.forClass(MapRecord.class);
        verify(streamOperations).add(streamCaptor.capture());
        Map<Object, Object> payload = streamCaptor.getValue().getValue();
        assertThat(payload.get("artifactId")).isEqualTo("303");
    }
}
