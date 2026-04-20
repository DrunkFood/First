package com.jy.eleaitender.common.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 内部文件服务客户端配置属性
 */
@Data
@ConfigurationProperties(prefix = InternalFileServiceProperties.PREFIX)
public class InternalFileServiceProperties {

    static final String PREFIX = "internal.file-service";

    /** 文件服务基础URL，如 http://localhost:8081 */
    private String baseUrl;

    /** 上传路径 */
    private String uploadPath = "/api/file/upload";

    /** 下载路径 */
    private String downloadPath = "/api/file/download";

    /** 文件信息路径 */
    private String infoPath = "/api/file/info";

    /** 连接超时（毫秒） */
    private int connectTimeout = 5000;

    /** 读取超时（毫秒） */
    private int readTimeout = 30000;

    /** 服务间调用使用的JWT密钥（默认复用系统JWT密钥） */
    private String jwtSecret;

    /** 服务间调用Token过期时间（毫秒），默认30分钟 */
    private long tokenExpiration = 30 * 60 * 1000L;
}
