package com.jy.eletender.tenderdocument.support;

/**
 * 解析当前登录态对应的外部业务用户上下文。
 * 控制层统一通过该接口获取 appKey、授权信息和外部用户身份，避免直接依赖安全框架细节。
 */
public interface CurrentExternalUserResolver {

    /**
     * 从当前请求上下文中提取外部用户信息。
     */
    TenderDocumentUserContext resolve();
}
