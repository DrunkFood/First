package com.jy.eletender.tenderdocument.support;

import com.jy.eletender.common.enums.ResponseCode;
import com.jy.eletender.common.exception.AuthException;
import com.jy.eletender.common.security.LoginUser;
import com.jy.eletender.common.security.SecurityContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 基于 JWT 登录态的外部用户解析实现。
 * 将安全上下文中的登录主体转换为编制系统内部统一的用户上下文对象。
 */
@Component
public class JwtCurrentExternalUserResolver implements CurrentExternalUserResolver {

    /**
     * 组装编制模块调用下游交互所需的最小身份信息。
     */
    @Override
    public TenderDocumentUserContext resolve() {
        LoginUser loginUser = SecurityContextHolder.getLoginUser();
        if (loginUser == null) {
            throw new AuthException(ResponseCode.UNAUTHORIZED);
        }

        TenderDocumentUserContext context = new TenderDocumentUserContext();
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
        // 回传业务系统时需要透传原始 Authorization，若当前线程不在 Web 请求内则返回 null。
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return null;
        }
        return servletAttributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
    }
}
