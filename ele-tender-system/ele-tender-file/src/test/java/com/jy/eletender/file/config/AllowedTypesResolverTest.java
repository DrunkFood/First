package com.jy.eletender.file.config;

import com.jy.eletender.common.constant.RedisKeyConstant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AllowedTypesResolverTest {

    @Mock
    private FileStorageConfig fileStorageConfig;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    private AllowedTypesResolver resolver;

    @BeforeEach
    void setUp() {
        when(fileStorageConfig.getAllowedTypes()).thenReturn(".pdf,.HzctZbs,.HzctTbs");
        resolver = new AllowedTypesResolver(fileStorageConfig, redisTemplate);
    }

    @Test
    void isAllowed_shouldAcceptStaticWhitelistSuffix() {
        assertThat(resolver.isAllowed("report.pdf")).isTrue();
        assertThat(resolver.isAllowed("tender.HzctZbs")).isTrue();
    }

    @Test
    void isAllowed_shouldAcceptRegisteredDynamicSuffix() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(RedisKeyConstant.REGISTERED_SUFFIXES))
                .thenReturn(Set.of(".CustomZbs", ".CustomTbs"));

        assertThat(resolver.isAllowed("project.CustomZbs")).isTrue();
        assertThat(resolver.isAllowed("bid.CustomTbs")).isTrue();
    }

    @Test
    void isAllowed_shouldRejectUnknownSuffix() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(RedisKeyConstant.REGISTERED_SUFFIXES))
                .thenReturn(Set.of());

        assertThat(resolver.isAllowed("malware.exe")).isFalse();
    }

    @Test
    void isAllowed_shouldCacheRedisResult() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(RedisKeyConstant.REGISTERED_SUFFIXES))
                .thenReturn(Set.of(".CustomZbs"));

        resolver.isAllowed("file.CustomZbs");
        resolver.isAllowed("file.CustomZbs");

        verify(setOperations, times(1)).members(RedisKeyConstant.REGISTERED_SUFFIXES);
    }

    @Test
    void isAllowed_shouldBeCaseInsensitive() {
        assertThat(resolver.isAllowed("FILE.PDF")).isTrue();
        assertThat(resolver.isAllowed("tender.hzctzbs")).isTrue();
    }

    @Test
    void isAllowed_shouldHandleNullRedisResult() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(RedisKeyConstant.REGISTERED_SUFFIXES))
                .thenReturn(null);

        assertThat(resolver.isAllowed("file.unknown")).isFalse();
    }

    @Test
    void isAllowed_shouldRetryAfterRedisFailureInsteadOfLockingCache() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(RedisKeyConstant.REGISTERED_SUFFIXES))
                .thenThrow(new RuntimeException("Redis down"))
                .thenReturn(Set.of(".NewSuffix"));

        assertThat(resolver.isAllowed("file.NewSuffix")).isFalse();
        assertThat(resolver.isAllowed("file.NewSuffix")).isTrue();

        verify(setOperations, times(2)).members(RedisKeyConstant.REGISTERED_SUFFIXES);
    }
}
