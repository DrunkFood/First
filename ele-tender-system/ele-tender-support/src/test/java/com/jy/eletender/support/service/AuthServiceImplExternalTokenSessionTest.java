package com.jy.eletender.support.service;

import com.jy.eletender.common.entity.support.SysAccessSystem;
import com.jy.eletender.common.util.JwtUtil;
import com.jy.eletender.support.mapper.SysAccessSystemMapper;
import com.jy.eletender.support.model.external.ExternalTokenIssueCommand;
import com.jy.eletender.support.model.external.ExternalTokenIssueResult;
import com.jy.eletender.support.service.impl.AuthServiceImpl;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplExternalTokenSessionTest {

    @Mock
    private SysAccessSystemMapper accessSystemMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void shouldStoreExternalTokenBySessionJtiKey() {
        AuthServiceImpl authService = new AuthServiceImpl();
        ReflectionTestUtils.setField(authService, "accessSystemMapper", accessSystemMapper);
        ReflectionTestUtils.setField(authService, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(authService, "externalTokenExpireSeconds", 3600L);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        SysAccessSystem system = new SysAccessSystem();
        system.setAppKey("demo-key");
        system.setStatus(1);
        when(accessSystemMapper.selectByAppKey("demo-key")).thenReturn(system);

        ExternalTokenIssueCommand command = new ExternalTokenIssueCommand();
        command.setUserId("U1");
        command.setUserName("张三");
        command.setEnterpriseId("E1");
        command.setEnterpriseName("企业A");
        command.setEnterpriseCode("QY001");

        ExternalTokenIssueResult result = authService.getExternalToken("demo-key", command);
        Claims claims = JwtUtil.parseToken(result.getToken());

        assertThat(claims.getId()).isNotBlank();
        verify(valueOperations).set(
                eq("external:token:demo-key:U1:" + claims.getId()),
                eq(result.getToken()),
                eq(3600L),
                eq(TimeUnit.SECONDS)
        );
    }
}
