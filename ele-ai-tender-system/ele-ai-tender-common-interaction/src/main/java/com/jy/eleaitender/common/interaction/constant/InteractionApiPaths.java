package com.jy.eleaitender.common.interaction.constant;

/**
 * 交互接口路径常量
 */
public final class InteractionApiPaths {

    public static final String BASE = "/api/eleTender/interaction";
    public static final String IDENTITY_CURRENT = BASE + "/identity/current";
    public static final String PROJECT_BASIC_INFO = BASE + "/projects/basic-info";
    public static final String BID_RECORD_SCHEME = BASE + "/bid-record-schemes/query";
    public static final String CA_KEYS_INFO = BASE + "/ca-keys/query";
    public static final String CALLBACK_TENDER_PDF = BASE + "/callbacks/tender-pdf";
    public static final String CALLBACK_TENDER_PACKAGE = BASE + "/callbacks/tender-package";
    public static final String CALLBACK_BID_DOCUMENT_RESULT = BASE + "/callbacks/bid-document-result";
    public static final String CALLBACK_BID_DECRYPT_RESULT = BASE + "/callbacks/bid-decrypt-result";

    private InteractionApiPaths() {
    }
}
