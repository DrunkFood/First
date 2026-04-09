package com.jy.eletender.tenderdocument.model.generation;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 最终招标文件明文顶层载荷。
 */
@Data
public class FinalPackagePayload {

    private BaseInfo baseInfo;
    private List<FinalPackageTenderPayload> tenders;
    private TenderDocumentSignPdf tenderDocumentSignPdf;
    private List<CaKeyInfo> caKeysInfo;
    private Settings settings;
    private VersionInfo versionInfo;

    @Data
    public static class BaseInfo {
        private String projectId;
        private String projectName;
        private String projectNo;
        private String purchaserName;
        private String purchaseMethod;
        private String bidEndTime;
        private List<TenderInfo> tendersInfo;
    }

    @Data
    public static class TenderInfo {
        private String tenderId;
        private String tenderName;
        private String tenderNo;
        private BigDecimal tenderAmount;
        private String purchaseContext;
    }

    @Data
    public static class TenderDocumentSignPdf {
        private String fileName;
        private Long size;
        private String signedFileBase64;
        private String sha256;
    }

    @Data
    public static class CaKeyInfo {
        private Integer encryptOrder;
        private String userId;
        private String caId;
        private String caNo;
        private String publicKey;
    }

    @Data
    public static class Settings {
        private SignPosition signPosition;
    }

    @Data
    public static class SignPosition {
        private Integer x;
        private Integer y;
    }

    @Data
    public static class VersionInfo {
        private String tenderDocumentFormatVersion;
        private String tenderDocumentAppVersion;
        private String tenderDocumentUniqueCode;
    }
}
