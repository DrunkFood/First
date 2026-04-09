package com.jy.eletender.interaction.core.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 电子标交互配置
 */
@Data
@ConfigurationProperties(prefix = "ele-tender.interaction")
public class EleTenderInteractionProperties {

    private boolean enabled = true;
    private String apiBaseUrl;
    private String pageBaseUrl;
    private String appKey;
    private String appSecret;
    private String tokenPath = "/api/external/token";
    private String userInfoPath = "/api/external/userinfo";
    private String tenderDocumentPagePath = "/tender-document/compose";
    private String fileBaseUrl;
    private String fileInfoPath = "/api/file/info/{fileId}";
    private String fileDownloadPath = "/api/file/download/{fileId}";
    private String fileUploadPath = "/api/file/upload";
    private String cryptoBaseUrl;
    private String bidDocumentPushPath = "/api/crypto/bid-document/push";
    private String bidDecryptSubmitPath = "/api/crypto/bid-decrypt/submit";
    private String bidDecryptStatusPath = "/api/crypto/bid-decrypt/status/{recordId}";
    private String envelopeJoinPath = "/api/crypto/envelope/join";
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(20);

    /**
     * 解析文件服务 base URL，为空时 fallback 到 apiBaseUrl。
     */
    public String resolveFileBaseUrl() {
        return resolveBaseUrl(fileBaseUrl);
    }

    /**
     * 解析加解密服务 base URL，为空时 fallback 到 apiBaseUrl。
     */
    public String resolveCryptoBaseUrl() {
        return resolveBaseUrl(cryptoBaseUrl);
    }

    private String resolveBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return apiBaseUrl;
        }
        return baseUrl;
    }
}
