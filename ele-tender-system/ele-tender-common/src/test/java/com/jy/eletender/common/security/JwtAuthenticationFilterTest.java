package com.jy.eletender.common.security;

import com.jy.eletender.common.constant.CommonConstant;
import com.jy.eletender.common.constant.RedisKeyConstant;
import com.jy.eletender.common.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private static final String DEFAULT_SECRET = "EleTenderSystemSecretKey2024JWT!";

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    void shouldPopulateContextForValidExternalToken() throws Exception {
        String token = JwtUtil.generateExternalToken("app-a", "user-1", "测试用户", "ent-1", "测试企业", "913301");
        String redisKey = RedisKeyConstant.EXTERNAL_TOKEN_PREFIX + "app-a:user-1:" + JwtUtil.parseToken(token).getId();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(token);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                redisTemplate,
                List.of(),
                Set.of(CommonConstant.TOKEN_TYPE_EXTERNAL)
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tender-documents/overview/100");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<LoginUser> observedLoginUser = new AtomicReference<>();

        filter.doFilter(request, response, (req, resp) ->
                observedLoginUser.set(SecurityContextHolder.getLoginUser()));

        LoginUser loginUser = observedLoginUser.get();
        assertThat(loginUser).isNotNull();
        assertThat(loginUser.getAppKey()).isEqualTo("app-a");
        assertThat(loginUser.getExternalUserId()).isEqualTo("user-1");
        assertThat(loginUser.getEnterpriseCode()).isEqualTo("913301");
        assertThat(loginUser.getExternalJti()).isNotBlank();
        assertThat(SecurityContextHolder.getLoginUser()).isNull();
        verify(valueOperations).get(redisKey);
    }

    @Test
    void shouldPopulateContextForLegacyExternalTokenWithoutJti() throws Exception {
        String token = buildLegacyExternalToken("app-legacy", "user-legacy");
        String redisKey = RedisKeyConstant.EXTERNAL_TOKEN_PREFIX + "app-legacy:user-legacy";
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(token);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                redisTemplate,
                List.of(),
                Set.of(CommonConstant.TOKEN_TYPE_EXTERNAL)
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tender-documents/overview/101");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<LoginUser> observedLoginUser = new AtomicReference<>();

        filter.doFilter(request, response, (req, resp) ->
                observedLoginUser.set(SecurityContextHolder.getLoginUser()));

        LoginUser loginUser = observedLoginUser.get();
        assertThat(loginUser).isNotNull();
        assertThat(loginUser.getAppKey()).isEqualTo("app-legacy");
        assertThat(loginUser.getExternalUserId()).isEqualTo("user-legacy");
        assertThat(loginUser.getExternalJti()).isNull();
        verify(valueOperations).get(redisKey);
    }

    @Test
    void shouldPopulateContextForValidInternalToken() throws Exception {
        String token = JwtUtil.generateToken(1001L, "admin");
        String redisKey = RedisKeyConstant.TOKEN_PREFIX + 1001L;
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(token);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                redisTemplate,
                List.of(),
                Set.of(CommonConstant.TOKEN_TYPE_INTERNAL)
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<LoginUser> observedLoginUser = new AtomicReference<>();

        filter.doFilter(request, response, (req, resp) ->
                observedLoginUser.set(SecurityContextHolder.getLoginUser()));

        LoginUser loginUser = observedLoginUser.get();
        assertThat(loginUser).isNotNull();
        assertThat(loginUser.getUserId()).isEqualTo(1001L);
        assertThat(loginUser.getUsername()).isEqualTo("admin");
        verify(valueOperations).get(redisKey);
    }

    @Test
    void shouldSkipExcludedPath() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                redisTemplate,
                List.of("/api/auth/login", "/swagger-ui"),
                Set.of(CommonConstant.TOKEN_TYPE_INTERNAL, CommonConstant.TOKEN_TYPE_EXTERNAL)
        );
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<LoginUser> observedLoginUser = new AtomicReference<>();

        filter.doFilter(request, response, (req, resp) ->
                observedLoginUser.set(SecurityContextHolder.getLoginUser()));

        assertThat(observedLoginUser.get()).isNull();
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void shouldPopulateContextWhenSubclassReadsTokenFromRequestParameter() throws Exception {
        String token = JwtUtil.generateExternalToken("app-b", "user-2", "签章用户", "ent-2", "测试企业2", "913302");
        String redisKey = RedisKeyConstant.EXTERNAL_TOKEN_PREFIX + "app-b:user-2:" + JwtUtil.parseToken(token).getId();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(token);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                redisTemplate,
                List.of(),
                Set.of(CommonConstant.TOKEN_TYPE_EXTERNAL)
        ) {
            @Override
            protected String getTokenFromRequest(HttpServletRequest request) {
                return request.getParameter("token");
            }
        };
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/file/esign/upload");
        request.setParameter("token", token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<LoginUser> observedLoginUser = new AtomicReference<>();

        filter.doFilter(request, response, (req, resp) ->
                observedLoginUser.set(SecurityContextHolder.getLoginUser()));

        LoginUser loginUser = observedLoginUser.get();
        assertThat(loginUser).isNotNull();
        assertThat(loginUser.getAppKey()).isEqualTo("app-b");
        assertThat(loginUser.getExternalUserId()).isEqualTo("user-2");
        verify(valueOperations).get(redisKey);
    }

    private String buildLegacyExternalToken(String appKey, String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", CommonConstant.TOKEN_TYPE_EXTERNAL);
        claims.put("appKey", appKey);
        claims.put("userId", userId);
        claims.put("userName", "旧用户");
        claims.put("enterpriseId", "old-ent");
        claims.put("enterpriseName", "旧企业");
        claims.put("enterpriseCode", "OLD-CODE");
        Date now = new Date();

        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 60_000))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(DEFAULT_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
