package com.jy.eletender.tenderdocument.support.interaction;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "tender-document.interaction")
public class TenderDocumentInteractionProperties {

    private String fileServiceBaseUrl = "http://localhost:8081";
    private String fileUploadPath = "/api/file/upload";
    private String fileDownloadPath = "/api/file/download/{fileId}";
    private String uploadBizType = "tender-document";
    private String finalPackageSuffix = ".HzctZbs";
    private String finalPackageEncryptAlgorithm = "AES/GCM/NoPadding";
    private String finalPackageAesKeyBase64 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private String compileInfoWatermarkText = "城投集团采购平台";
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(20);

    public String getFileServiceBaseUrl() {
        return fileServiceBaseUrl;
    }

    public void setFileServiceBaseUrl(String fileServiceBaseUrl) {
        this.fileServiceBaseUrl = fileServiceBaseUrl;
    }

    public String getFileUploadPath() {
        return fileUploadPath;
    }

    public void setFileUploadPath(String fileUploadPath) {
        this.fileUploadPath = fileUploadPath;
    }

    public String getFileDownloadPath() {
        return fileDownloadPath;
    }

    public void setFileDownloadPath(String fileDownloadPath) {
        this.fileDownloadPath = fileDownloadPath;
    }

    public String getUploadBizType() {
        return uploadBizType;
    }

    public void setUploadBizType(String uploadBizType) {
        this.uploadBizType = uploadBizType;
    }

    public String getFinalPackageSuffix() {
        return finalPackageSuffix;
    }

    public void setFinalPackageSuffix(String finalPackageSuffix) {
        this.finalPackageSuffix = finalPackageSuffix;
    }

    public String getFinalPackageEncryptAlgorithm() {
        return finalPackageEncryptAlgorithm;
    }

    public void setFinalPackageEncryptAlgorithm(String finalPackageEncryptAlgorithm) {
        this.finalPackageEncryptAlgorithm = finalPackageEncryptAlgorithm;
    }

    public String getFinalPackageAesKeyBase64() {
        return finalPackageAesKeyBase64;
    }

    public void setFinalPackageAesKeyBase64(String finalPackageAesKeyBase64) {
        this.finalPackageAesKeyBase64 = finalPackageAesKeyBase64;
    }

    public String getCompileInfoWatermarkText() {
        return compileInfoWatermarkText;
    }

    public void setCompileInfoWatermarkText(String compileInfoWatermarkText) {
        this.compileInfoWatermarkText = compileInfoWatermarkText;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }
}
