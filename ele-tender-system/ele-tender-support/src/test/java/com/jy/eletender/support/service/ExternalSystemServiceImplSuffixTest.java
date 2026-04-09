package com.jy.eletender.support.service;

import com.jy.eletender.common.constant.RedisKeyConstant;
import com.jy.eletender.common.entity.support.SysAccessSystem;
import com.jy.eletender.support.mapper.SysAccessSystemMapper;
import com.jy.eletender.support.service.impl.ExternalSystemServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExternalSystemServiceImplSuffixTest {

    @Mock
    private SysAccessSystemMapper accessSystemMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private ExternalSystemServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        lenient().when(accessSystemMapper.insert(any())).thenReturn(1);
        lenient().when(accessSystemMapper.updateById(any())).thenReturn(1);
    }

    @Test
    void createSystem_shouldRejectInvalidTenderDocumentSuffix() {
        SysAccessSystem system = new SysAccessSystem();
        system.setSystemName("test");
        system.setTenderDocumentSuffix("../evil");

        assertThatThrownBy(() -> service.createSystem(system))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("文件后缀格式不合法");
    }

    @Test
    void createSystem_shouldRejectInvalidBidDocumentSuffix() {
        SysAccessSystem system = new SysAccessSystem();
        system.setSystemName("test");
        system.setBidDocumentSuffix(".has space");

        assertThatThrownBy(() -> service.createSystem(system))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("文件后缀格式不合法");
    }

    @Test
    void createSystem_shouldAcceptValidSuffixAndSyncRedis() {
        SysAccessSystem system = new SysAccessSystem();
        system.setSystemName("test");
        system.setTenderDocumentSuffix(".CustomZbs");

        SysAccessSystem existing = new SysAccessSystem();
        existing.setTenderDocumentSuffix(".OtherZbs");
        existing.setBidDocumentSuffix(".OtherTbs");
        when(accessSystemMapper.selectList(any())).thenReturn(List.of(existing, system));

        service.createSystem(system);

        verify(accessSystemMapper).insert(system);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<String[]> valCaptor = ArgumentCaptor.forClass(String[].class);
        verify(setOperations).add(keyCaptor.capture(), valCaptor.capture());
        assertThat(keyCaptor.getValue()).startsWith(RedisKeyConstant.REGISTERED_SUFFIXES + ":tmp:");
        assertThat(valCaptor.getValue()).containsExactlyInAnyOrder(".OtherZbs", ".OtherTbs", ".CustomZbs");
        verify(redisTemplate).rename(keyCaptor.getValue(), RedisKeyConstant.REGISTERED_SUFFIXES);
    }

    @Test
    void createSystem_shouldSucceedEvenWhenRedisRefreshFails() {
        SysAccessSystem system = new SysAccessSystem();
        system.setSystemName("test");
        system.setTenderDocumentSuffix(".ValidZbs");

        when(accessSystemMapper.selectList(any())).thenReturn(List.of(system));
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        doThrow(new RuntimeException("Redis down")).when(redisTemplate).rename(any(), any());

        service.createSystem(system);
        verify(accessSystemMapper).insert(system);
    }

    @Test
    void createSystem_shouldAllowNullSuffix() {
        SysAccessSystem system = new SysAccessSystem();
        system.setSystemName("test");

        lenient().when(accessSystemMapper.selectList(any())).thenReturn(List.of());

        service.createSystem(system);
        verify(accessSystemMapper).insert(system);
    }

    @Test
    void updateSystem_shouldValidateSuffixBeforeUpdate() {
        SysAccessSystem system = new SysAccessSystem();
        system.setId(1L);
        system.setTenderDocumentSuffix(".valid");
        system.setBidDocumentSuffix("no-dot");

        assertThatThrownBy(() -> service.updateSystem(system))
                .isInstanceOf(IllegalArgumentException.class);

        verify(accessSystemMapper, never()).updateById(any());
    }
}
