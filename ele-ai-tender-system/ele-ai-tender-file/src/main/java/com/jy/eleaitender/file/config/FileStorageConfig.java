package com.jy.eleaitender.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 文件存储配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageConfig {

    /**
     * 存储根路径
     */
    private String basePath = "/data/ele-ai-tender/files";

    /**
     * 允许的文件类型（逗号分隔）
     */
    private String allowedTypes = ".jar,.war,.zip,.tar.gz,.pdf,.HzctZbs,.HzctTbs";

    /**
     * 最大文件大小(MB)
     */
    private int maxSize = 500;

    /**
     * 获取允许的文件类型列表
     */
    public List<String> getAllowedTypeList() {
        return Arrays.asList(allowedTypes.split(","));
    }

    /**
     * 获取最大文件大小（字节）
     */
    public long getMaxSizeBytes() {
        return (long) maxSize * 1024 * 1024;
    }
}
