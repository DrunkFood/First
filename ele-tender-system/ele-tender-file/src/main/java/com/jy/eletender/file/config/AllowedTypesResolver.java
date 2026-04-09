package com.jy.eletender.file.config;

import com.jy.eletender.common.constant.RedisKeyConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AllowedTypesResolver {

    private final Set<String> staticAllowedTypes;
    private final StringRedisTemplate redisTemplate;

    private volatile Set<String> cachedDynamicSuffixes = Collections.emptySet();
    private volatile long lastRefreshTime = 0;

    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;

    public AllowedTypesResolver(FileStorageConfig fileStorageConfig,
                                StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.staticAllowedTypes = Arrays.stream(fileStorageConfig.getAllowedTypes().split(","))
                .map(s -> s.trim().toLowerCase())
                .collect(Collectors.toSet());
    }

    public boolean isAllowed(String filename) {
        if (filename == null) {
            return false;
        }
        String lowerFilename = filename.toLowerCase();

        for (String type : staticAllowedTypes) {
            if (lowerFilename.endsWith(type)) {
                return true;
            }
        }

        Set<String> dynamicSuffixes = getDynamicSuffixes();
        for (String suffix : dynamicSuffixes) {
            if (lowerFilename.endsWith(suffix.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    private Set<String> getDynamicSuffixes() {
        long now = System.currentTimeMillis();
        if (now - lastRefreshTime < CACHE_TTL_MS) {
            return cachedDynamicSuffixes;
        }
        try {
            Set<String> members = redisTemplate.opsForSet()
                    .members(RedisKeyConstant.REGISTERED_SUFFIXES);
            cachedDynamicSuffixes = (members != null) ? members : Collections.emptySet();
            lastRefreshTime = now;
            log.debug("刷新动态后缀缓存: {}", cachedDynamicSuffixes);
        } catch (Exception e) {
            log.warn("从 Redis 读取注册后缀失败，保留本地缓存", e);
        }
        return cachedDynamicSuffixes;
    }
}
