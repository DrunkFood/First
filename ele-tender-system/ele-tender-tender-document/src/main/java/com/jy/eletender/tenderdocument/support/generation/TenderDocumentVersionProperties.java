package com.jy.eletender.tenderdocument.support.generation;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 最终招标文件版本信息配置。
 */
@ConfigurationProperties(prefix = "tender-document.version")
public class TenderDocumentVersionProperties {

    /**
     * 招标文件编制系统版本号（整数上升）。
     */
    private Integer appVersion = 1;

    /**
     * 招标文件格式版本号。
     * 推荐格式：格式版本号_招标系统版本号_自定义。
     */
    private String formatVersion = "0.0.1_1_default";

    public Integer getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(Integer appVersion) {
        this.appVersion = appVersion;
    }

    public String getFormatVersion() {
        return formatVersion;
    }

    public void setFormatVersion(String formatVersion) {
        this.formatVersion = formatVersion;
    }
}
