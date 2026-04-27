package com.jy.eleaitender.support.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 内部AI服务客户端配置属性
 */
@Data
@ConfigurationProperties(prefix = InternalAiServiceProperties.PREFIX)
public class InternalAiServiceProperties {

    static final String PREFIX = "internal.ai-service";

    /** AI服务基础URL，如 http://localhost:8083 */
    private String baseUrl;

    /** 连接超时（毫秒） */
    private int connectTimeout = 5000;

    /** 读取超时（毫秒） */
    private int readTimeout = 10000;

    /** 服务间调用使用的JWT密钥 */
    private String jwtSecret;

    /** 服务间调用Token过期时间（毫秒），默认30分钟 */
    private long tokenExpiration = 30 * 60 * 1000L;
}
