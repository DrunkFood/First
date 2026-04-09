package com.jy.eletender.devtools.cli;

final class DevToolsConstants {

    static final String TENDER_DOCUMENT_SUFFIX = ".HzctZbs";
    static final String BID_DOCUMENT_SUFFIX = ".HzctTbs";

    /** mock 标录方案（schemeContent），招标文件和投标文件共用同一份方案定义 */
    static final String MOCK_BID_FORMS_SCHEME_CONTENT =
            "[{\"id\":\"f001\",\"sign\":\"bidPrice\",\"description\":\"投标报价\",\"unit\":\"元\","
            + "\"indexOf\":0,\"mustFlag\":true,\"selected\":true,\"controlLength\":20},"
            + "{\"id\":\"f002\",\"sign\":\"quality\",\"description\":\"质量\",\"unit\":\"\","
            + "\"indexOf\":1,\"mustFlag\":true,\"selected\":true,\"controlLength\":10},"
            + "{\"id\":\"f003\",\"sign\":\"time\",\"description\":\"工期\",\"unit\":\"天\","
            + "\"indexOf\":2,\"mustFlag\":true,\"selected\":true,\"controlLength\":5}]";

    private DevToolsConstants() {
    }
}
