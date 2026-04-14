# AI编制系统阶段二核心业务开发 - SQL脚本说明

## 概述

本文档汇总了阶段二核心业务开发所需的所有数据库初始化脚本。

## SQL脚本清单

### 1. 核心业务表初始化
**文件**: `sql/phase2-core-business-init.sql`  
**用途**: 创建AI编制系统核心业务表  
**包含表**:

| 表名 | 说明 |
|------|------|
| `ai_project` | 项目表（含编号、名称、类别、类型、预算、状态等） |
| `ai_project_version` | 项目版本表（版本号、内容快照、变更说明） |
| `ai_requirement` | 业务需求表（需求描述、匹配模式、内容） |
| `ai_template` | 模板表（Markdown内容、结构定义、版本） |
| `ai_review_item` | 评审项表（三级嵌套结构） |
| `ai_detection_record` | 检测记录表（检测类型、结果、状态） |
| `ai_knowledge_document` | 知识库文档表（文档类别、向量集合） |
| `ai_model_config` | AI模型配置表（模型类型、API端点、参数） |
| `sup_message` | 消息表（支撑模块的消息中心） |

### 2. 业务用户角色初始化
**文件**: `ele-ai-tender-support/sql/init-bid-user-role.sql`  
**用途**: 创建普通业务用户角色，仅允许访问AI编制系统  
**包含内容**:
- 创建 `BID_USER` 业务用户角色
- 创建AI编制系统专属菜单（项目管理、需求管理、评审项、文档生成、AI助手、模板库）
- 创建按钮级权限（创建、编辑、删除、生成、导出等）
- 为业务用户角色分配菜单权限
- 创建测试用户 `testuser`（密码: user123）

### 3. 手机验证码功能
**文件**: `ele-ai-tender-support/sql/init-sms-code.sql`  
**用途**: 支持手机验证码登录功能  
**包含内容**:
- 创建 `sup_sms_code` 短信验证码表
- 为 `sup_user` 表添加 `phone` 和 `email` 字段（如不存在）
- 添加手机号唯一索引

### 4. 测试数据初始化
**文件**: `sql/phase2-test-data-init.sql`  
**用途**: 插入示例数据，方便开发测试  
**包含内容**:
- 3个AI模型配置（DeepSeek-V3、DeepSeek-Coder、GPT-4）
- 2个招标文件模板（政府采购-服务类、产权交易-货物类）
- 2个示例项目
- 示例评审项（三级结构）

## 执行顺序

按照以下顺序执行SQL脚本:

```sql
-- 1. 创建核心业务表
SOURCE sql/phase2-core-business-init.sql;

-- 2. 创建短信验证码表
SOURCE ele-ai-tender-support/sql/init-sms-code.sql;

-- 3. 创建业务用户角色和菜单
SOURCE ele-ai-tender-support/sql/init-bid-user-role.sql;

-- 4. 插入测试数据（可选）
SOURCE sql/phase2-test-data-init.sql;
```

## 数据库配置

- **数据库**: `ele_ai_tender`
- **连接**: `jdbc:mysql://127.0.0.1:3306/ele_ai_tender?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai`
- **用户名**: `root`
- **密码**: `123456`（或环境变量 `SPRING_DATASOURCE_PASSWORD`）

## 验证

执行完成后，运行以下查询验证:

```sql
-- 查看所有AI相关表
SHOW TABLES LIKE 'ai_%';
SHOW TABLES LIKE 'sup_%';

-- 查看业务用户角色
SELECT * FROM sup_role WHERE role_code = 'BID_USER';

-- 查看测试用户
SELECT id, username, phone FROM sup_user WHERE username IN ('admin', 'testuser');

-- 查看AI模型配置
SELECT id, model_name, model_type FROM ai_model_config;

-- 查看模板
SELECT id, template_code, template_name FROM ai_template;
```

## 测试用户账号

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| admin | admin123 | ADMIN | 系统管理员（支撑系统+AI编制系统） |
| testuser | user123 | BID_USER | 普通业务用户（仅AI编制系统） |

## 注意事项

1. **执行前备份**: 执行SQL前请备份数据库，避免数据丢失
2. **环境变量**: AI模型配置中的API密钥使用占位符，需替换为真实值或通过环境变量传入
3. **密码加密**: 所有密码使用BCrypt加密，不要明文存储
4. **菜单权限**: 业务用户角色的菜单权限仅包含AI编制相关，不包含支撑系统菜单
5. **数据隔离**: 本系统使用独立数据库 `ele_ai_tender`，与现有系统 `ele_tender`（db=5）隔离

## 数据字典

### 项目状态流转

```
DRAFT → IN_PROGRESS → PENDING_DETECTION → DETECTING → DETECTION_PASSED → PUBLISHED
                                                       ↓
                                            DETECTION_FAILED / DETECTION_SKIPPED
                                                                 ↓
                                                      ARCHIVED / CANCELLED
```

### 项目类别

- `LIMITED_BELOW` - 限额以下
- `PROPERTY_TRADE` - 产权交易
- `GOVERNMENT_PROCUREMENT` - 政府采购

### 项目类型

- `ENGINEERING` - 工程
- `GOODS` - 货物
- `SERVICE` - 服务

### 服务子类型

- `PROPERTY` - 物业服务
- `IT_SERVICE` - IT服务
- `CONSULTING` - 咨询服务
- `MAINTENANCE` - 维护服务

### 检测类型

- `FAIRNESS` - 公平性检测
- `COMPLIANCE` - 合规性检测
- `TYPO` - 错别字检测
- `SENSITIVE_WORD` - 敏感词检测

### 文档类别

- `POLICY` - 政策文件
- `HISTORY_TEMPLATE` - 历史模板
- `STANDARD` - 标准规范
- `OTHER` - 其他
