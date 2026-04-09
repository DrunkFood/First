package com.jy.eletender.file.security;

import com.jy.eletender.common.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Set;

/**
 * 仅为特定路径提供“请求参数 token”认证能力，避免影响全局 Bearer 语义。
 */
public class RequestParamJwtAuthenticationFilter extends JwtAuthenticationFilter {

    private final String tokenParameterName;

    public RequestParamJwtAuthenticationFilter(StringRedisTemplate redisTemplate,
                                               List<String> excludePathPrefixes,
                                               Set<String> acceptedTokenTypes,
                                               String tokenParameterName) {
        super(redisTemplate, excludePathPrefixes, acceptedTokenTypes);
        this.tokenParameterName = tokenParameterName;
    }

    @Override
    protected String getTokenFromRequest(HttpServletRequest request) {
        String token = request.getParameter(tokenParameterName);
        return StringUtils.trimToNull(token);
    }
}
