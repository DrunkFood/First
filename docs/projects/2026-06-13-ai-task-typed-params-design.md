# AI任务强类型参数类设计

> 日期: 2026-06-13
> 状态: 已确认

## 背景

当前 AI 任务的 `requestParams` 使用 `Map<String, Object>` 传递参数，存在以下问题：
- 参数名靠字符串约定，无编译期检查
- 类型不安全，消费端需手动 cast
- 创建端和消费端参数容易不对齐
- 缺乏自文档能力，新开发者无法快速了解每种任务需要什么参数

## 设计决策

| 决策项 | 选择 | 理由 |
|--------|------|------|
| clazz 定位 | 运行时自动反序列化 | 彻底告别 Map 和手动 cast |
| 参数类位置 | ele-ai-tender-common | core 和 ai 模块都能直接使用 |
| 改造策略 | 一步到位 | 调用方约5-6个，改动可控 |
| 预算字段类型 | 统一 String | 消费端都当 String 用，避免 BigDecimal 序列化差异 |

## 参数类设计

### RequirementGenerateParams (需求生成)

| 字段 | 类型 | 说明 |
|------|------|------|
| requirementName | String | 需求名称 |
| projectType | String | 项目类型 |
| budget | String | 预算金额 |
| description | String | 需求描述 |

### ProjectRequirementGenerateParams (项目需求生成)

| 字段 | 类型 | 说明 |
|------|------|------|
| requirementName | String | 需求名称(项目名+" - 招标需求") |
| projectType | String | 项目类型 |
| projectCategory | String | 项目类别 |
| budget | String | 预算金额 |
| description | String | 项目描述 |
| referenceContent | String | 参考内容(预留) |

### ReviewItemGenerateParams (评审项生成)

| 字段 | 类型 | 说明 |
|------|------|------|
| projectName | String | 项目名称 |
| projectType | String | 项目类型 |
| projectCategory | String | 项目类别 |
| budget | String | 预算金额 |
| reviewMethod | String | 评审方法 |
| requirementContent | String | 需求内容 |
| reviewConfig | String | 评审配置(JSON字符串，可选) |

### DocumentIntegrationParams (文档集成)

| 字段 | 类型 | 说明 |
|------|------|------|
| templateFileId | Long | 模板文件ID |
| projectName | String | 项目名称 |
| fillDataList | List\<FillData\> | 填充数据列表 |

### DetectionParams (4种检测共用)

| 字段 | 类型 | 说明 |
|------|------|------|
| detectionRecordId | Long | 检测记录ID |
| detectionType | String | 检测类型编码 |
| contentFileId | Long | 内容文件ID(可选) |
| content | String | 待检测文本(可选) |

### TextOptimizeParams (文本优化)

| 字段 | 类型 | 说明 |
|------|------|------|
| content | String | 待优化文本 |
| requirement | String | 优化要求(可选) |

## 枚举改造

```java
REQUIREMENT_GENERATE("REQUIREMENT_GENERATE", "需求生成", RequirementGenerateParams.class),
PROJECT_REQUIREMENT_GENERATE("PROJECT_REQUIREMENT_GENERATE", "项目需求生成", ProjectRequirementGenerateParams.class),
REVIEW_ITEM_GENERATE("REVIEW_ITEM_GENERATE", "评审项生成", ReviewItemGenerateParams.class),
DOCUMENT_INTEGRATION("DOCUMENT_INTEGRATION", "文档集成", DocumentIntegrationParams.class),
DETECTION_SENSITIVE_WORD("DETECTION_SENSITIVE_WORD", "敏感词检测", DetectionParams.class),
DETECTION_TYPO("DETECTION_TYPO", "错别字检测", DetectionParams.class),
DETECTION_POLICY_REVIEW("DETECTION_POLICY_REVIEW", "政策文件审查", DetectionParams.class),
DETECTION_FORMAT_CHECK("DETECTION_FORMAT_CHECK", "格式规范检测", DetectionParams.class),
TEXT_OPTIMIZE("TEXT_OPTIMIZE", "文本优化", TextOptimizeParams.class);
```

## 接口改造

### createTask

```java
// Before
AiTask createTask(AiTaskType type, Long projectId, Long bizId, String bizType,
                  Map<String, Object> requestParams, String fileIds);

// After
AiTask createTask(AiTaskType type, Long projectId, Long bizId, String bizType,
                  Object requestParams, String fileIds);
```

序列化逻辑不变：`objectMapper.writeValueAsString(requestParams)`，仅入参类型从 Map 改为 Object。

### 消费端解析

```java
// Before
Map<String, Object> params = parseParams(task.getRequestParams());
String name = MapUtils.getString(params, "requirementName");

// After
T params = objectMapper.readValue(task.getRequestParams(), type.getClazz());
String name = params.getRequirementName();
```

## 改动范围

| 模块 | 文件 | 改动 |
|------|------|------|
| common | `AiTaskType.java` | clazz 赋值 |
| common | 新增 5 个参数类 | `dto/ai/` 包 |
| core | `IAiTaskService.java` | 接口签名 Map → Object |
| core | `AiTaskServiceImpl.java` | 实现签名同步改 |
| core | `RequirementServiceImpl.java` | Map → 强类型 |
| core | `ProjectServiceImpl.java` | Map → 强类型 |
| core | `ReviewItemServiceImpl.java` | Map → 强类型 |
| core | `DocumentIntegrationServiceImpl.java` | Map → 强类型 |
| core | `DetectionServiceImpl.java` | Map → 强类型 |
| ai | `GenerateResultParser.java` | 新增泛型 parseParams |
| ai | `RequirementGenerator.java` | Map 取值 → getter |
| ai | `ReviewItemGenerator.java` | Map 取值 → getter |
| ai | `DocumentIntegration.java` | Map 取值 → getter |
| ai | `DetectionEngine.java` | Map 取值 → getter |
| ai | `TextOptimizer.java` | Map 取值 → getter |

**不改动**: `AiTask.requestParams` 仍是 String(JSON)，数据库 schema 不变。

## 已知差异

| 创建端 | 参数 | 传递类型 | 统一后 |
|--------|------|----------|--------|
| RequirementServiceImpl | budget | String (toPlainString) | String |
| ProjectServiceImpl | budget | BigDecimal | → String (toPlainString) |
| ReviewItemServiceImpl | budget | String (toPlainString) | String |
