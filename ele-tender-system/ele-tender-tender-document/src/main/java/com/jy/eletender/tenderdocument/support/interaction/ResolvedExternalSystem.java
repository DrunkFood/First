package com.jy.eletender.tenderdocument.support.interaction;

public class ResolvedExternalSystem {

    private final String appKey;
    private final String appSecret;
    private final String baseUrl;

    public ResolvedExternalSystem(String appKey, String appSecret, String baseUrl) {
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.baseUrl = baseUrl;
    }

    public String getAppKey() {
        return appKey;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
