-- =========================================
-- AI编制系统阶段二：初始测试数据
-- 用于开发测试，包含示例项目、模板、AI模型配置等
-- =========================================

USE `ele_ai_tender`;

-- =========================================
-- 1. 初始化AI模型配置数据
-- =========================================

-- 示例云端模型配置(DeepSeek)
INSERT IGNORE INTO `ai_model_config` (`model_name`, `model_type`, `api_endpoint`, `api_key`, `model_params`, `usage_scenario`, `is_active`, `create_id`, `create_name`, `modify_id`, `modify_name`) 
VALUES 
('DeepSeek-V3', 'CLOUD', 'https://api.deepseek.com/v1', '${DEEPSEEK_API_KEY:sk-placeholder}', 
 JSON_OBJECT('temperature', 0.7, 'max_tokens', 4000, 'top_p', 0.9), 
 'GENERATION', 1, 1, 'admin', 1, 'admin'),

('DeepSeek-Coder', 'CLOUD', 'https://api.deepseek.com/v1', '${DEEPSEEK_API_KEY:sk-placeholder}', 
 JSON_OBJECT('temperature', 0.5, 'max_tokens', 2000, 'top_p', 0.9), 
 'OPTIMIZATION', 1, 1, 'admin', 1, 'admin'),

('GPT-4', 'CLOUD', 'https://api.openai.com/v1', '${OPENAI_API_KEY:sk-placeholder}', 
 JSON_OBJECT('temperature', 0.7, 'max_tokens', 8000, 'top_p', 0.9), 
 'DETECTION', 0, 1, 'admin', 1, 'admin');

-- =========================================
-- 2. 初始化示例模板数据
-- =========================================

-- 示例模板:政府采购-服务类
INSERT IGNORE INTO `ai_template` (`template_code`, `template_name`, `project_category`, `project_type`, `content`, `structure_definition`, `is_default`, `create_id`, `create_name`, `modify_id`, `modify_name`) 
VALUES 
(
    'GOV_SERVICE_TEMPLATE_V1',
    '政府采购-服务类招标文件模板',
    'GOVERNMENT_PROCUREMENT',
    'SERVICE',
    '# 招标文件\n\n## 第一章 招标公告\n\n### 一、项目概况\n{{project_name}}项目，预算金额{{budget}}万元，现进行公开招标。\n\n### 二、投标人资格要求\n1. 符合《政府采购法》第二十二条规定\n2. 未被列入失信被执行人、重大税收违法案件当事人名单\n3. 本项目不接受联合体投标\n\n### 三、获取招标文件\n1. 时间：{{get_doc_start}}至{{get_doc_end}}\n2. 地点：{{get_doc_location}}\n3. 售价：{{get_doc_price}}元\n\n### 四、提交投标文件截止时间和开标时间\n1. 提交投标文件截止时间：{{bid_deadline}}\n2. 开标时间：{{bid_open_time}}\n3. 开标地点：{{bid_open_location}}\n\n---\n\n## 第二章 投标人须知\n\n### 一、总则\n1. 项目说明\n2. 合格的投标人\n3. 投标费用\n\n### 二、招标文件\n1. 招标文件的构成\n2. 招标文件的澄清与修改\n\n### 三、投标文件的编制\n1. 投标文件的组成\n2. 投标报价\n3. 投标保证金\n\n### 四、投标文件的提交\n1. 投标文件的密封和标记\n2. 提交投标文件的截止时间\n\n### 五、开标和评标\n1. 开标\n2. 评标委员会\n3. 评标方法\n4. 定标\n\n---\n\n## 第三章 项目需求\n\n### 一、项目背景\n{{project_background}}\n\n### 二、服务内容\n{{service_content}}\n\n### 三、服务要求\n{{service_requirements}}\n\n### 四、服务人员要求\n{{staff_requirements}}\n\n### 五、服务质量标准\n{{quality_standards}}\n\n---\n\n## 第四章 评标办法\n\n### 一、评标方法\n本项目采用综合评分法。\n\n### 二、评分标准\n1. 价格部分({{price_score}}分)\n2. 技术部分({{tech_score}}分)\n3. 商务部分({{business_score}}分)\n\n### 三、废标条款\n{{invalid_bid_conditions}}\n\n---\n\n## 第五章 合同条款及格式\n\n### 一、合同条款\n{{contract_terms}}\n\n### 二、合同格式\n{{contract_format}}\n\n---\n\n## 第六章 投标文件格式\n\n### 一、投标函\n### 二、法定代表人身份证明\n### 三、授权委托书\n### 四、投标保证金\n### 五、报价表\n### 六、技术方案\n### 七、商务响应表\n### 八、资格证明文件',
    JSON_OBJECT(
        'chapters', JSON_ARRAY(
            JSON_OBJECT('name', '招标公告', 'required', true),
            JSON_OBJECT('name', '投标人须知', 'required', true),
            JSON_OBJECT('name', '项目需求', 'required', true),
            JSON_OBJECT('name', '评标办法', 'required', true),
            JSON_OBJECT('name', '合同条款', 'required', false),
            JSON_OBJECT('name', '投标文件格式', 'required', true)
        ),
        'placeholders', JSON_ARRAY(
            'project_name', 'budget', 'get_doc_start', 'get_doc_end',
            'bid_deadline', 'bid_open_time', 'project_background',
            'service_content', 'service_requirements'
        )
    ),
    1,
    1, 'admin', 1, 'admin'
);

-- 示例模板:产权交易-货物类
INSERT IGNORE INTO `ai_template` (`template_code`, `template_name`, `project_category`, `project_type`, `content`, `structure_definition`, `is_default`, `create_id`, `create_name`, `modify_id`, `modify_name`) 
VALUES 
(
    'PROPERTY_GOODS_TEMPLATE_V1',
    '产权交易-货物类招标文件模板',
    'PROPERTY_TRADE',
    'GOODS',
    '# 招标文件\n\n## 第一章 投标邀请\n\n### 一、项目基本情况\n项目名称：{{project_name}}\n预算金额：{{budget}}万元\n采购内容：{{goods_description}}\n\n### 二、申请人的资格要求\n1. 满足《中华人民共和国政府采购法》第二十二条规定\n2. 落实政府采购政策需满足的资格要求\n3. 本项目的特定资格要求：{{specific_requirements}}\n\n### 三、获取招标文件\n1. 时间：{{get_doc_start}}至{{get_doc_end}}\n2. 方式：{{get_doc_method}}\n3. 售价：{{get_doc_price}}\n\n### 四、提交投标文件截止时间、开标时间和地点\n1. 截止时间：{{bid_deadline}}\n2. 开标时间：{{bid_open_time}}\n3. 地点：{{bid_open_location}}\n\n---\n\n## 第二章 投标人须知\n\n### 投标人须知前附表\n{{bid_instruction_table}}\n\n### 一、总则\n1. 项目概况\n2. 资金来源和落实情况\n3. 投标人的资格要求\n\n### 二、招标文件\n1. 招标文件的组成\n2. 招标文件的澄清和修改\n\n### 三、投标文件\n1. 投标文件的组成\n2. 投标报价说明\n3. 投标有效期\n4. 投标保证金\n\n### 四、投标文件的提交\n1. 投标文件的密封和标记\n2. 提交投标文件的截止时间\n\n### 五、开标和评标\n1. 开标时间和地点\n2. 评标委员会\n3. 评标原则和方法\n4. 中标候选人的确定\n\n---\n\n## 第三章 货物需求及技术规格\n\n### 一、货物清单\n{{goods_list}}\n\n### 二、技术规格和要求\n{{technical_requirements}}\n\n### 三、交货期和交货地点\n1. 交货期：{{delivery_time}}\n2. 交货地点：{{delivery_location}}\n\n### 四、质量保证期和售后服务\n1. 质量保证期：{{warranty_period}}\n2. 售后服务要求：{{after_sale_service}}\n\n### 五、验收标准和方法\n{{acceptance_criteria}}\n\n---\n\n## 第四章 评标方法和标准\n\n### 一、评标方法\n{{evaluation_method}}\n\n### 二、评分标准\n1. 价格评分({{price_score}}分)\n2. 技术评分({{tech_score}}分)\n3. 商务评分({{business_score}}分)\n\n### 三、中标候选人的推荐\n{{recommendation_rules}}\n\n---\n\n## 第五章 合同主要条款\n\n### 一、合同签订\n{{contract_signing}}\n\n### 二、合同履行\n{{contract_performance}}\n\n### 三、付款方式\n{{payment_terms}}\n\n### 四、违约责任\n{{liability_for_breach}}\n\n---\n\n## 第六章 投标文件格式\n\n### 格式一：投标函\n### 格式二：法定代表人身份证明\n### 格式三：授权委托书\n### 格式四：投标保证金\n### 格式五：投标报价表\n### 格式六：技术规格响应表\n### 格式七：资格证明文件',
    JSON_OBJECT(
        'chapters', JSON_ARRAY(
            JSON_OBJECT('name', '投标邀请', 'required', true),
            JSON_OBJECT('name', '投标人须知', 'required', true),
            JSON_OBJECT('name', '货物需求及技术规格', 'required', true),
            JSON_OBJECT('name', '评标方法和标准', 'required', true),
            JSON_OBJECT('name', '合同主要条款', 'required', false),
            JSON_OBJECT('name', '投标文件格式', 'required', true)
        ),
        'placeholders', JSON_ARRAY(
            'project_name', 'budget', 'goods_description',
            'goods_list', 'technical_requirements', 'delivery_time'
        )
    ),
    0,
    1, 'admin', 1, 'admin'
);

-- =========================================
-- 3. 初始化示例项目数据(可选)
-- =========================================

-- 示例项目
INSERT IGNORE INTO `ai_project` (`project_code`, `project_name`, `project_category`, `project_type`, `service_sub_type`, `budget`, `review_type`, `status`, `template_id`, `create_id`, `create_name`, `modify_id`, `modify_name`) 
VALUES 
('PRJ-2026-001', '某市政府物业服务项目', 'GOVERNMENT_PROCUREMENT', 'SERVICE', 'PROPERTY', 500.00, 'MANUAL', 'DRAFT', 
 (SELECT id FROM ai_template WHERE template_code = 'GOV_SERVICE_TEMPLATE_V1' LIMIT 1),
 1, 'admin', 1, 'admin'),

('PRJ-2026-002', '某单位信息化设备采购项目', 'PROPERTY_TRADE', 'GOODS', NULL, 200.00, 'INTELLIGENT', 'IN_PROGRESS',
 (SELECT id FROM ai_template WHERE template_code = 'PROPERTY_GOODS_TEMPLATE_V1' LIMIT 1),
 1, 'admin', 1, 'admin');

-- =========================================
-- 4. 初始化示例评审项数据(关联示例项目)
-- =========================================

-- 获取示例项目ID
SET @project_id_1 = (SELECT id FROM ai_project WHERE project_code = 'PRJ-2026-001' LIMIT 1);

-- 示例评审项(三级结构)
INSERT IGNORE INTO `ai_review_item` (`project_id`, `parent_id`, `level`, `item_name`, `item_content`, `sort_order`, `create_id`, `create_name`, `modify_id`, `modify_name`) 
VALUES 
-- 一级评审项
(@project_id_1, 0, 1, '资格性审查', '审查投标人是否符合基本资格要求', 1, 1, 'admin', 1, 'admin'),
(@project_id_1, 0, 1, '符合性审查', '审查投标文件是否符合招标文件要求', 2, 1, 'admin', 1, 'admin'),
(@project_id_1, 0, 1, '详细评审', '对通过资格性和符合性审查的投标文件进行详细评审', 3, 1, 'admin', 1, 'admin');

-- 获取一级评审项ID
SET @qualification_review_id = (SELECT id FROM ai_review_item WHERE project_id = @project_id_1 AND item_name = '资格性审查' LIMIT 1);
SET @compliance_review_id = (SELECT id FROM ai_review_item WHERE project_id = @project_id_1 AND item_name = '符合性审查' LIMIT 1);
SET @detailed_review_id = (SELECT id FROM ai_review_item WHERE project_id = @project_id_1 AND item_name = '详细评审' LIMIT 1);

-- 二级评审项
INSERT IGNORE INTO `ai_review_item` (`project_id`, `parent_id`, `level`, `item_name`, `item_content`, `sort_order`, `create_id`, `create_name`, `modify_id`, `modify_name`) 
VALUES 
-- 资格性审查的子项
(@project_id_1, @qualification_review_id, 2, '营业执照', '具有有效的营业执照', 1, 1, 'admin', 1, 'admin'),
(@project_id_1, @qualification_review_id, 2, '财务状况', '提供近三年的财务审计报告', 2, 1, 'admin', 1, 'admin'),
(@project_id_1, @qualification_review_id, 2, '纳税社保', '提供近三个月的纳税和社保证明', 3, 1, 'admin', 1, 'admin'),

-- 符合性审查的子项
(@project_id_1, @compliance_review_id, 2, '投标文件完整性', '投标文件包含所有必需的内容', 1, 1, 'admin', 1, 'admin'),
(@project_id_1, @compliance_review_id, 2, '投标保证金', '按要求提交投标保证金', 2, 1, 'admin', 1, 'admin'),
(@project_id_1, @compliance_review_id, 2, '投标有效期', '投标有效期满足要求', 3, 1, 'admin', 1, 'admin'),

-- 详细评审的子项
(@project_id_1, @detailed_review_id, 2, '技术方案评审', '评审技术方案的科学性、可行性', 1, 1, 'admin', 1, 'admin'),
(@project_id_1, @detailed_review_id, 2, '商务报价评审', '评审报价的合理性和完整性', 2, 1, 'admin', 1, 'admin'),
(@project_id_1, @detailed_review_id, 2, '服务能力评审', '评审投标人的服务能力和经验', 3, 1, 'admin', 1, 'admin');

-- =========================================
-- 验证
-- =========================================
SELECT '========== AI模型配置 ==========' AS info;
SELECT id, model_name, model_type, usage_scenario, is_active FROM ai_model_config;

SELECT '========== 招标文件模板 ==========' AS info;
SELECT id, template_code, template_name, project_category, project_type, is_default FROM ai_template;

SELECT '========== 示例项目 ==========' AS info;
SELECT id, project_code, project_name, project_category, project_type, budget, status FROM ai_project;

SELECT '========== 评审项结构 ==========' AS info;
SELECT 
    r1.id AS level1_id, r1.item_name AS level1_name,
    r2.id AS level2_id, r2.item_name AS level2_name
FROM ai_review_item r1
LEFT JOIN ai_review_item r2 ON r2.parent_id = r1.id AND r2.level = 2
WHERE r1.level = 1 AND r1.project_id = @project_id_1
ORDER BY r1.sort_order, r2.sort_order;

SELECT '========== 阶段二测试数据初始化完成 ==========' AS message;
