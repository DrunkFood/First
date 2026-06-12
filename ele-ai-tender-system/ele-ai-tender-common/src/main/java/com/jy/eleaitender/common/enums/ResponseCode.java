package com.jy.eleaitender.common.enums;

/**
 * 响应状态码枚举
 */
public enum ResponseCode {

    // 通用状态码
    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),
    
    // 参数错误 400-499
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "方法不允许"),
    
    // 用户相关 1001-1999
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_PASSWORD_ERROR(1002, "用户名或密码错误"),
    USER_DISABLED(1003, "用户已被禁用"),
    USER_EXISTS(1004, "用户已存在"),
    USER_TOKEN_EXPIRED(1005, "Token已过期"),
    USER_TOKEN_INVALID(1006, "Token无效"),

    // 短信验证码相关 1011-1019
    SMS_CODE_EXPIRED(1011, "验证码已过期"),
    SMS_CODE_ERROR(1012, "验证码错误"),
    SMS_SEND_TOO_FREQUENT(1013, "短信发送过于频繁"),
    
    // 角色权限相关 2001-2999
    ROLE_NOT_FOUND(2001, "角色不存在"),
    ROLE_EXISTS(2002, "角色已存在"),
    PERMISSION_DENIED(2003, "权限不足"),
    DATA_ACCESS_DENIED(2004, "无权访问该数据"),
    MENU_NOT_FOUND(2005, "菜单不存在"),
    
    // 接入系统相关 3001-3999
    SYSTEM_NOT_FOUND(3001, "接入系统不存在"),
    SYSTEM_DISABLED(3002, "接入系统已禁用"),
    SYSTEM_EXPIRED(3003, "接入系统已过期"),
    SIGNATURE_ERROR(3004, "签名验证失败"),
    TIMESTAMP_EXPIRED(3005, "时间戳已过期"),
    APP_KEY_NOT_FOUND(3006, "AppKey不存在"),
    
    // 版本管理相关 4001-4999
    VERSION_NOT_FOUND(4001, "版本不存在"),
    VERSION_EXISTS(4002, "版本已存在"),
    VERSION_STATUS_ERROR(4003, "版本状态错误"),
    PLUGIN_NOT_FOUND(4004, "插件不存在"),
    
    // 文件相关 5001-5999
    FILE_NOT_FOUND(5001, "文件不存在"),
    FILE_UPLOAD_ERROR(5002, "文件上传失败"),
    FILE_TYPE_NOT_ALLOWED(5003, "文件类型不允许"),
    FILE_SIZE_EXCEEDED(5004, "文件大小超出限制"),
    FILE_SHA256_ERROR(5005, "文件SHA-256校验失败"),

    // 加解密相关 7001-7999
    BID_DOCUMENT_PUSH_PARAM_ERROR(7001, "投标文件预存参数错误"),
    BID_DOCUMENT_NOT_FOUND(7002, "投标文件不存在"),
    BID_DOCUMENT_SHA256_MISMATCH(7003, "投标文件SHA-256不匹配"),
    BID_DECRYPT_REQUEST_NOT_FOUND(7004, "解密请求不存在"),
    BID_DECRYPT_REQUEST_STATUS_INVALID(7005, "解密请求状态非法"),
    BID_DECRYPT_ARTIFACT_NOT_FOUND(7006, "解密工件不存在"),
    BID_DECRYPT_ARTIFACT_EXECUTE_FAILED(7007, "解密工件执行失败"),
    BID_DECRYPT_ARTIFACT_TIMEOUT(7008, "解密工件执行超时"),
    BID_DECRYPT_CALLBACK_FAILED(7009, "解密回调失败"),
    BID_DECRYPT_NATIVE_ERROR(7010, "原生解密调用失败"),
    BID_DECRYPT_PARSE_ERROR(7011, "解密结果解析失败"),
    BID_DECRYPT_DUPLICATE_REQUEST(7012, "重复解密请求"),
    BID_DECRYPT_INTERNAL_ERROR(7013, "解密系统内部错误"),

    // AI编制系统相关 8001-8999
    // 项目相关 8001-8009
    PROJECT_NOT_FOUND(8001, "项目不存在"),
    PROJECT_EXISTS(8002, "项目已存在"),
    PROJECT_STATUS_ERROR(8003, "项目状态错误"),
    PROJECT_CODE_GENERATE_ERROR(8004, "项目编号生成失败"),
    PROJECT_PHASE_ERROR(8005, "项目阶段错误"),
    PROJECT_PHASE_ACTIVE_TASK(8006, "项目存在正在执行的AI任务，请等待完成后再推进"),

    // 需求相关 8011-8019
    REQUIREMENT_NOT_FOUND(8011, "需求不存在"),
    REQUIREMENT_STATUS_ERROR(8012, "需求状态错误"),
    REQUIREMENT_MATCH_ERROR(8013, "需求模板匹配失败"),
    REQUIREMENT_NAME_EXISTS(8014, "需求名称已存在"),

    // 模板相关 8021-8029
    TEMPLATE_NOT_FOUND(8021, "模板不存在"),
    TEMPLATE_EXISTS(8022, "模板已存在"),
    TEMPLATE_SET_DEFAULT_ERROR(8023, "设置默认模板失败"),
    TEMPLATE_STATUS_ERROR(8024, "模板状态错误"),

    // 评审项相关 8031-8039
    REVIEW_ITEM_NOT_FOUND(8031, "评审项不存在"),
    REVIEW_ITEM_TREE_ERROR(8032, "评审项树构建失败"),
    REVIEW_ITEM_CASCADE_DELETE_ERROR(8033, "评审项级联删除失败"),
    REVIEW_ITEM_MAX_LEVEL_EXCEEDED(8034, "评审项层级不能超过3级"),

    // 版本相关 8041-8049
    VERSION_SNAPSHOT_ERROR(8041, "版本快照创建失败"),
    VERSION_COMPARE_ERROR(8042, "版本对比失败"),

    // 知识库相关 8051-8059
    KNOWLEDGE_NOT_FOUND(8051, "知识文档不存在"),
    KNOWLEDGE_UPLOAD_ERROR(8052, "知识文档上传失败"),
    KNOWLEDGE_PARSE_ERROR(8053, "知识文档解析失败"),
    KNOWLEDGE_VECTOR_ERROR(8054, "知识文档向量化失败"),

    // 检测相关 8061-8069
    DETECTION_NOT_FOUND(8061, "检测记录不存在"),
    DETECTION_STATUS_ERROR(8062, "检测状态错误"),
    DETECTION_START_ERROR(8063, "启动检测失败"),
    DETECTION_CONFIRM_ERROR(8064, "确认检测结果失败"),

    // 模型配置相关 8071-8079
    MODEL_CONFIG_NOT_FOUND(8071, "模型配置不存在"),
    MODEL_CONFIG_ENCRYPT_ERROR(8072, "模型密钥加密失败"),
    MODEL_CONFIG_ACTIVE_ERROR(8073, "激活模型配置失败"),

    // 系统参数相关 9001-9009
    SYS_PARAM_NOT_FOUND(9001, "系统参数不存在"),

    // 政策文件相关 9011-9019
    POLICY_FILE_NOT_FOUND(9011, "政策文件不存在"),

    // AI任务相关 8081-8099
    TASK_NOT_FOUND(8081, "AI任务不存在"),
    TASK_STATUS_ERROR(8082, "AI任务状态错误"),
    TASK_NOT_RETRYABLE(8083, "AI任务不可重试"),
    TASK_ALREADY_PROCESSING(8084, "AI任务正在处理中"),

    // 反馈相关 8091-8099
    FEEDBACK_NOT_FOUND(8091, "反馈不存在"),
    FEEDBACK_TARGET_NOT_FOUND(8092, "反馈目标不存在"),
    FEEDBACK_ALREADY_EXISTS(8093, "已反馈，不可修改"),

    // 消息相关 9021-9029
    MESSAGE_NOT_FOUND(9021, "消息不存在"),

    // 模型路由相关 9031-9039
    MODEL_ROUTE_NOT_FOUND(9031, "路由规则不存在"),

    // 文档集成相关 9041-9049
    DOCUMENT_INTEGRATE_ERROR(9041, "文档集成失败"),
    DOCUMENT_EXPORT_ERROR(9042, "文档导出失败"),
    OPERATION_NOT_SUPPORTED(9043, "操作暂不支持"),
    DOCUMENT_INTEGRATE_DUPLICATE(9044, "文档集成任务正在执行中");

    private final int code;
    private final String message;

    ResponseCode(int code, String message) {
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
