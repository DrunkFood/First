package com.jy.eletender.crypto.support;

import com.jy.eletender.common.enums.ResponseCode;
import com.jy.eletender.common.exception.AuthException;
import com.jy.eletender.common.security.LoginUser;
import com.jy.eletender.common.security.SecurityContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class JwtCurrentExternalUserResolver implements CurrentExternalUserResolver {

    @Override
    public CryptoUserContext resolve() {
        LoginUser loginUser = SecurityContextHolder.getLoginUser();
        if (loginUser == null) {
            throw new AuthException(ResponseCode.UNAUTHORIZED);
        }

        CryptoUserContext context = new CryptoUserContext();
        context.setAppKey(loginUser.getAppKey());
        context.setAuthorization(resolveAuthorizationHeader());
        context.setUserId(loginUser.getExternalUserId());
        context.setUserName(loginUser.getExternalUserName());
        context.setEnterpriseId(loginUser.getEnterpriseId());
        context.setEnterpriseName(loginUser.getEnterpriseName());
        context.setEnterpriseCode(loginUser.getEnterpriseCode());
        return context;
    }

    private String resolveAuthorizationHeader() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return null;
        }
        return servletAttributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
    }
}
