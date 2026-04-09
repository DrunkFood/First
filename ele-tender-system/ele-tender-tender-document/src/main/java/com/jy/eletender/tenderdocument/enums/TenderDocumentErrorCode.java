package com.jy.eletender.tenderdocument.enums;

/**
 * 招标文件编制系统异常码
 */
public enum TenderDocumentErrorCode {
    TENDER_DOCUMENT_NOT_FOUND(6001, "编制单不存在"),
    PROJECT_LOCKED(6002, "当前项目已由首创人锁定，仅首创人可查看和编辑"),
    TENDER_DOCUMENT_NOT_EDITABLE(6003, "当前编制单状态不允许编辑"),
    ENTRY_BASIC_INFO_SYNC_FAILED(6004, "入口同步项目信息失败"),
    BASIC_INFO_NOT_SYNCED(6005, "基本信息尚未完成同步"),
    BID_RECORD_NOT_SYNCED(6006, "标录信息尚未完成同步"),
    STEP_NOT_FOUND(6007, "步骤不存在"),
    STEP_COMPLETE_NOT_ALLOWED(6008, "当前步骤不允许完成并进入下一步"),
    RULE_NOT_FOUND(6009, "评审规则不存在"),
    RULE_VALIDATION_FAILED(6010, "评审规则校验失败"),
    TENDER_ID_REQUIRED(6011, "当前操作必须传入标段ID"),
    FILE_BINDING_NOT_FOUND(6012, "当前步骤缺少可用文件绑定"),
    GENERATION_NOT_ALLOWED(6013, "当前编制单不允许生成文件"),
    GENERATED_FILE_MISSING(6014, "缺少已生成产物文件"),
    GENERATION_IN_PROGRESS(6019, "当前生成正在进行，请勿重复提交"),
    GENERATION_ALREADY_COMPLETED(6020, "当前编制单已完成，无需再次生成"),
    CALLBACK_IN_PROGRESS(6022, "当前回传正在进行，请勿重复提交"),
    CALLBACK_NOT_ALLOWED(6015, "当前编制单不允许回传"),
    BUSINESS_SYSTEM_CONFIG_INVALID(6016, "业务系统接入配置无效"),
    BUSINESS_SYSTEM_INTERACTION_FAILED(6017, "调用业务系统交互接口失败"),
    FILE_SERVICE_ACCESS_FAILED(6018, "访问文件服务失败"),
    RECOMPILE_NOT_ALLOWED(6021, "当前编制单状态不允许重新编制");

    private final int code;
    private final String message;

    TenderDocumentErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
