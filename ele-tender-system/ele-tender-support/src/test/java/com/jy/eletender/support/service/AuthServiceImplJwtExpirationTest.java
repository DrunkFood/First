package com.jy.eletender.support.service;

import com.jy.eletender.common.dto.request.UserLoginRequest;
import com.jy.eletender.common.dto.response.UserLoginResponse;
import com.jy.eletender.common.entity.support.SysUser;
import com.jy.eletender.common.util.JwtUtil;
import com.jy.eletender.common.util.PasswordUtil;
import com.jy.eletender.support.mapper.SysUserMapper;
import com.jy.eletender.support.service.impl.AuthServiceImpl;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplJwtExpirationTest {

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @Test
    void shouldUseConfiguredJwtExpirationForInternalTokenAndRedisTtl() {
        AuthServiceImpl authService = new AuthServiceImpl();
        ReflectionTestUtils.setField(authService, "userMapper", userMapper);
        ReflectionTestUtils.setField(authService, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(authService, "tokenExpireSeconds", 600L);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        SysUser user = new SysUser();
        user.setId(1001L);
        user.setUsername("admin");
        user.setPassword(PasswordUtil.encode("123456"));
        user.setStatus(1);

        when(userMapper.selectByUsername("admin")).thenReturn(user);
        when(userMapper.selectPermissionsByUserId(1001L)).thenReturn(List.of("user:view"));
        when(userMapper.selectRoleCodesByUserId(1001L)).thenReturn(List.of("admin"));

        UserLoginRequest request = new UserLoginRequest();
        request.setUsername("admin");
        request.setPassword("123456");

        UserLoginResponse response = authService.login(request);
        Claims claims = JwtUtil.parseToken(response.getToken());
        long expirationMs = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();

        assertThat(response.getExpireIn()).isEqualTo(600L);
        assertThat(expirationMs).isEqualTo(600L * 1000);
        verify(valueOperations).set(eq("token:1001"), eq(response.getToken()), eq(600L), eq(TimeUnit.SECONDS));
    }
}
