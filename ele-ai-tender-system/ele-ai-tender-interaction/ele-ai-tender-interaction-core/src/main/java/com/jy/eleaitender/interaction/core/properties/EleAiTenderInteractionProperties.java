package com.jy.eleaitender.interaction.core.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 电子标交互配置
 */
@Data
@ConfigurationProperties(prefix = "ele-ai-tender.interaction")
public class EleAiTenderInteractionProperties {

    private boolean enabled = true;
    private String appKey;
    private String appSecret;

    private String apiBaseUrl;
    private String tokenPath = "/api/external/token";
    private String userInfoPath = "/api/external/userinfo";

    private String fileBaseUrl;
    private String fileInfoPath = "/api/file/info/{fileId}";
    private String fileInfoPathSha256 = "/api/file/info/sha256/{fileId}";
    private String fileDownloadPath = "/api/file/download/{fileId}";
    private String fileUploadPath = "/api/file/upload";

    private String coreBaseUrl;
    private String aiTaskCreatePath = "/api/external/ai-tasks";
    private String aiTaskQueryPath = "/api/external/ai-tasks/{taskId}";
    private String policyFileQueryPath = "/api/external/policy-file/all";

    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(20);

    /**
     * 解析文件服务 base URL，为空时 fallback 到 apiBaseUrl。
     */
    public String resolveFileBaseUrl() {
        return resolveBaseUrl(fileBaseUrl);
    }

    private String resolveBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return apiBaseUrl;
        }
        return baseUrl;
    }
}
