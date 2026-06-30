package com.jy.eleaitender.support.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 短信网关配置。
 */
@Data
@ConfigurationProperties(prefix = "msg")
public class SmsGatewayProperties {

    /**
     * 网关账号。
     */
    private String username;

    /**
     * 网关密码。
     */
    private String pwd;

    /**
     * 扩展号。
     */
    private String extend;

    /**
     * 网关地址。
     */
    private String url;

    /**
     * 是否正式发送短信。
     */
    private Boolean isformal = Boolean.FALSE;
}
