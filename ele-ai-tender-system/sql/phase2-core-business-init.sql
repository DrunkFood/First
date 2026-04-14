-- =========================================
-- AI编制系统阶段二：核心业务数据库初始化脚本
-- 包含：项目、需求、模板、评审项、检测、知识库、AI模型、消息表
-- =========================================

USE `ele_ai_tender`;

-- =========================================
-- 1. 项目管理相关表
-- =========================================

-- AI编制项目表
CREATE TABLE IF NOT EXISTS ai_project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_code VARCHAR(50) NOT NULL UNIQUE COMMENT '项目编号',
    project_name VARCHAR(200) NOT NULL COMMENT '项目名称',
    project_category VARCHAR(20) NOT NULL COMMENT '项目类别:LIMITED_BELOW/PROPERTY_TRADE/GOVERNMENT_PROCUREMENT',
    project_type VARCHAR(20) NOT NULL COMMENT '项目类型:ENGINEERING/GOODS/SERVICE',
    service_sub_type VARCHAR(50) COMMENT '服务子类型:PROPERTY/IT_SERVICE/CONSULTING/MAINTENANCE',
    budget DECIMAL(15,2) COMMENT '预算金额(万元)',
    review_type VARCHAR(20) COMMENT '评审类型:MANUAL/INTELLIGENT',
    status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态:DRAFT/IN_PROGRESS/PENDING_DETECTION/DETECTING/DETECTION_PASSED/DETECTION_FAILED/DETECTION_SKIPPED/PUBLISHED/ARCHIVED/CANCELLED',
    template_id BIGINT COMMENT '使用的模板ID',
    requirement_id BIGINT COMMENT '关联的业务需求ID',
    requirement_source VARCHAR(20) COMMENT '需求来源:REFERENCE/AI_GENERATED',
    requirement_content TEXT COMMENT '招标需求内容',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_id BIGINT COMMENT '创建人ID',
    create_name VARCHAR(50) COMMENT '创建人名称',
    modify_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    modify_id BIGINT COMMENT '修改人ID',
    modify_name VARCHAR(50) COMMENT '修改人名称',
    ver INT DEFAULT 0 COMMENT '乐观锁版本号',
    is_delete INT DEFAULT 0 COMMENT '逻辑删除:0未删除/1已删除',
    INDEX idx_status (status),
    INDEX idx_category (project_category),
    INDEX idx_create_id (create_id),
    INDEX idx_project_code (project_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI编制项目表';

-- AI编制项目版本表
CREATE TABLE IF NOT EXISTS ai_project_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '项目ID',
    version_no INT NOT NULL COMMENT '版本号',
    content_snapshot JSON COMMENT '内容快照(JSON格式存储完整文档内容)',
    change_description VARCHAR(500) COMMENT '变更说明',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_id BIGINT COMMENT '创建人ID',
    create_name VARCHAR(50) COMMENT '创建人名称',
    modify_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    modify_id BIGINT COMMENT '修改人ID',
    modify_name VARCHAR(50) COMMENT '修改人名称',
    ver INT DEFAULT 0 COMMENT '乐观锁版本号',
    is_delete INT DEFAULT 0 COMMENT '逻辑删除:0未删除/1已删除',
    INDEX idx_project_version (project_id, version_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI编制项目版本表';

-- =========================================
-- 2. 业务需求表
-- =========================================

CREATE TABLE IF NOT EXISTS ai_requirement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    requirement_name VARCHAR(200) NOT NULL COMMENT '需求名称',
    project_category VARCHAR(20) NOT NULL COMMENT '项目类别:LIMITED_BELOW/PROPERTY_TRADE/GOVERNMENT_PROCUREMENT',
    project_type VARCHAR(20) NOT NULL COMMENT '项目类型:ENGINEERING/GOODS/SERVICE',
    budget DECIMAL(15,2) COMMENT '预算金额(万元)',
    requirement_description VARCHAR(1000) COMMENT '需求描述',
    match_mode VARCHAR(20) COMMENT '匹配模式:AUTO_MATCH/MANUAL_SELECT/UPLOAD',
    matched_file_id BIGINT COMMENT '匹配的历史文件ID(关联file_info表)',
    matched_similarity DECIMAL(5,2) COMMENT '匹配度百分比(0-100)',
    uploaded_file_id BIGINT COMMENT '上传的文件ID(关联file_info表)',
    project_id BIGINT COMMENT '关联的项目ID',
    content LONGTEXT COMMENT '业务需求内容(Markdown格式)',
    status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态:DRAFT/CONFIRMED/SUBMITTED/APPROVED',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_id BIGINT COMMENT '创建人ID',
    create_name VARCHAR(50) COMMENT '创建人名称',
    modify_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    modify_id BIGINT COMMENT '修改人ID',
    modify_name VARCHAR(50) COMMENT '修改人名称',
    ver INT DEFAULT 0 COMMENT '乐观锁版本号',
    is_delete INT DEFAULT 0 COMMENT '逻辑删除:0未删除/1已删除',
    INDEX idx_status (status),
    INDEX idx_create_id (create_id),
    INDEX idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务需求表';

-- =========================================
-- 3. 模板管理表
-- =========================================

CREATE TABLE IF NOT EXISTS ai_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_code VARCHAR(50) NOT NULL UNIQUE COMMENT '模板编码',
    template_name VARCHAR(200) NOT NULL COMMENT '模板名称',
    project_category VARCHAR(20) NOT NULL COMMENT '适用项目类别:LIMITED_BELOW/PROPERTY_TRADE/GOVERNMENT_PROCUREMENT',
    project_type VARCHAR(20) NOT NULL COMMENT '适用项目类型:ENGINEERING/GOODS/SERVICE',
    content LONGTEXT NOT NULL COMMENT '模板内容(Markdown格式)',
    structure_definition JSON COMMENT '模板结构定义(JSON格式描述章节结构)',
    version_no INT DEFAULT 1 COMMENT '版本号',
    is_default TINYINT(1) DEFAULT 0 COMMENT '是否默认模板:0否/1是',
    status VARCHAR(20) DEFAULT 'ENABLED' COMMENT '状态:ENABLED/DISABLED',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_id BIGINT COMMENT '创建人ID',
    create_name VARCHAR(50) COMMENT '创建人名称',
    modify_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    modify_id BIGINT COMMENT '修改人ID',
    modify_name VARCHAR(50) COMMENT '修改人名称',
    ver INT DEFAULT 0 COMMENT '乐观锁版本号',
    is_delete INT DEFAULT 0 COMMENT '逻辑删除:0未删除/1已删除',
    INDEX idx_category_type (project_category, project_type),
    INDEX idx_status (status),
    INDEX idx_is_default (is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招标文件模板表';

-- =========================================
-- 4. 评审项表
-- =========================================

CREATE TABLE IF NOT EXISTS ai_review_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '项目ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父级ID(0表示顶级节点)',
    level INT DEFAULT 1 COMMENT '层级:1一级/2二级/3三级',
    item_name VARCHAR(200) NOT NULL COMMENT '评审项名称',
    item_content TEXT COMMENT '评审项内容(Markdown格式)',
    sort_order INT DEFAULT 0 COMMENT '排序序号(同级内排序)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_id BIGINT COMMENT '创建人ID',
    create_name VARCHAR(50) COMMENT '创建人名称',
    modify_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    modify_id BIGINT COMMENT '修改人ID',
    modify_name VARCHAR(50) COMMENT '修改人名称',
    ver INT DEFAULT 0 COMMENT '乐观锁版本号',
    is_delete INT DEFAULT 0 COMMENT '逻辑删除:0未删除/1已删除',
    INDEX idx_project (project_id),
    INDEX idx_parent (parent_id),
    INDEX idx_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评审项表(三级嵌套结构)';

-- =========================================
-- 5. 智能检测相关表
-- =========================================

CREATE TABLE IF NOT EXISTS ai_detection_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '项目ID',
    detection_type VARCHAR(50) NOT NULL COMMENT '检测类型:FAIRNESS/COMPLIANCE/TYPO/SENSITIVE_WORD',
    content_snapshot LONGTEXT COMMENT '检测内容快照(Markdown格式)',
    result JSON COMMENT '检测结果(JSON格式存储问题列表、分数等)',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态:PENDING/RUNNING/PASSED/FAILED/SKIPPED',
    issue_count INT DEFAULT 0 COMMENT '发现问题数量',
    score DECIMAL(5,2) COMMENT '检测得分(0-100)',
    started_at DATETIME COMMENT '开始时间',
    completed_at DATETIME COMMENT '完成时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_id BIGINT COMMENT '创建人ID',
    create_name VARCHAR(50) COMMENT '创建人名称',
    modify_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    modify_id BIGINT COMMENT '修改人ID',
    modify_name VARCHAR(50) COMMENT '修改人名称',
    ver INT DEFAULT 0 COMMENT '乐观锁版本号',
    is_delete INT DEFAULT 0 COMMENT '逻辑删除:0未删除/1已删除',
    INDEX idx_project (project_id),
    INDEX idx_status (status),
    INDEX idx_detection_type (detection_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能检测记录表';

-- =========================================
-- 6. 知识库相关表
-- =========================================

CREATE TABLE IF NOT EXISTS ai_knowledge_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    doc_name VARCHAR(200) NOT NULL COMMENT '文档名称',
    doc_category VARCHAR(50) COMMENT '文档类别:POLICY(政策文件)/HISTORY_TEMPLATE(历史模板)/STANDARD(标准规范)/OTHER',
    file_id BIGINT NOT NULL COMMENT '文件ID(关联file_info表)',
    file_type VARCHAR(20) COMMENT '文件类型:DOC/DOCX/PDF/TXT/MD',
    content LONGTEXT COMMENT '文档文本内容(解析后的纯文本)',
    vector_collection VARCHAR(100) COMMENT '向量集合名称(Milvus collection)',
    vector_ids JSON COMMENT '向量ID列表(关联Milvus中的向量)',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态:ACTIVE/ARCHIVED/PROCESSING/FAILED',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_id BIGINT COMMENT '创建人ID',
    create_name VARCHAR(50) COMMENT '创建人名称',
    modify_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    modify_id BIGINT COMMENT '修改人ID',
    modify_name VARCHAR(50) COMMENT '修改人名称',
    ver INT DEFAULT 0 COMMENT '乐观锁版本号',
    is_delete INT DEFAULT 0 COMMENT '逻辑删除:0未删除/1已删除',
    INDEX idx_category (doc_category),
    INDEX idx_status (status),
    INDEX idx_file_id (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档表';

-- =========================================
-- 7. AI模型配置表
-- =========================================

CREATE TABLE IF NOT EXISTS ai_model_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    model_name VARCHAR(100) NOT NULL COMMENT '模型名称',
    model_type VARCHAR(20) NOT NULL COMMENT '模型类型:LOCAL(本地微调)/CLOUD(云端大模型)/PRIVATE(私有化部署)',
    api_endpoint VARCHAR(500) COMMENT 'API端点地址',
    api_key VARCHAR(500) COMMENT 'API密钥(AES加密存储)',
    model_params JSON COMMENT '模型参数(如temperature、max_tokens等)',
    usage_scenario VARCHAR(50) COMMENT '使用场景:GENERATION(生成)/OPTIMIZATION(优化)/DETECTION(检测)',
    is_active TINYINT(1) DEFAULT 1 COMMENT '是否启用:0停用/1启用',
    token_usage BIGINT DEFAULT 0 COMMENT 'Token使用量(累计)',
    cost DECIMAL(10,2) DEFAULT 0.00 COMMENT '累计费用(元)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_id BIGINT COMMENT '创建人ID',
    create_name VARCHAR(50) COMMENT '创建人名称',
    modify_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    modify_id BIGINT COMMENT '修改人ID',
    modify_name VARCHAR(50) COMMENT '修改人名称',
    ver INT DEFAULT 0 COMMENT '乐观锁版本号',
    is_delete INT DEFAULT 0 COMMENT '逻辑删除:0未删除/1已删除',
    INDEX idx_model_type (model_type),
    INDEX idx_usage_scenario (usage_scenario),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型配置表';

-- =========================================
-- 8. 消息中心表(support模块)
-- =========================================

CREATE TABLE IF NOT EXISTS sup_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '接收用户ID',
    title VARCHAR(200) NOT NULL COMMENT '消息标题',
    content TEXT COMMENT '消息内容',
    message_type VARCHAR(20) NOT NULL COMMENT '消息类型:SYSTEM(系统)/AUDIT(审核)/DETECTION(检测)/WARNING(警告)',
    biz_id BIGINT COMMENT '关联业务ID(如项目ID、需求ID等)',
    biz_type VARCHAR(20) COMMENT '关联业务类型:PROJECT/REQUIREMENT/DETECTION/TEMPLATE',
    is_read TINYINT(1) DEFAULT 0 COMMENT '是否已读:0未读/1已读',
    read_time DATETIME COMMENT '阅读时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_id BIGINT COMMENT '创建人ID',
    create_name VARCHAR(50) COMMENT '创建人名称',
    modify_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    modify_id BIGINT COMMENT '修改人ID',
    modify_name VARCHAR(50) COMMENT '修改人名称',
    ver INT DEFAULT 0 COMMENT '乐观锁版本号',
    is_delete INT DEFAULT 0 COMMENT '逻辑删除:0未删除/1已删除',
    INDEX idx_user (user_id),
    INDEX idx_type (message_type),
    INDEX idx_read (is_read),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- =========================================
-- 执行完成提示
-- =========================================
SELECT '阶段二核心业务表创建完成!' AS message;
SELECT 
    TABLE_NAME,
    TABLE_COMMENT,
    CREATE_TIME
FROM 
    information_schema.TABLES 
WHERE 
    TABLE_SCHEMA = 'ele_ai_tender' 
    AND TABLE_NAME LIKE 'ai_%'
ORDER BY 
    CREATE_TIME;
