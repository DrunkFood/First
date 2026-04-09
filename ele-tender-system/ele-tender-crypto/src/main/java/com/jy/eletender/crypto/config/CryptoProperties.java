package com.jy.eletender.crypto.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "crypto")
public class CryptoProperties {

    private String bidDocumentPath = "/data/ele-tender/crypto/bid-documents";

    private String decryptFilePath = "/data/ele-tender/crypto/decrypted";

    private String tempFilePath = "/data/ele-tender/crypto/temp";

    private String organizedFilePath = "/data/ele-tender/crypto/organized";

    private int segmentSize = 2 * 1024 * 1024;

    private String rsaPrivateKey;

    private String bidderPwdFingerprintSecret;

    private int consumerThreads = 4;

    private int taskTimeoutSeconds = 1800;

    private int timeoutScanIntervalSeconds = 300;

    private int maxRetryCount = 3;

    private int callbackMaxRetryCount = 3;

    private String fileServiceBaseUrl = "http://localhost:8081";

    private String fileInfoPath = "/api/file/info/{fileId}";

    private String fileDownloadPath = "/api/file/download/{fileId}";

    private long passwordCacheSeconds = 3600;

    private int streamPollBatchSize = 20;

    private int streamPollBlockMillis = 1000;

    private Stream stream = new Stream();

    @Data
    public static class Stream {

        private String key = "bdc:decrypt:tasks";

        private String group = "bdc-decrypt-group";

        private long maxLen = 10000;

        private String consumerName = "crypto-consumer";
    }
}
