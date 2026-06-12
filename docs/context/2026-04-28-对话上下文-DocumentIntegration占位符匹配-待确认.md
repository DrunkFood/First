# 对话上下文文档 - DocumentIntegration 占位符AI匹配实现

> 生成时间: 2026-04-28 13:30 | 状态: 待确认

---

## 📋 问题背景

**项目**: 招标文件AI编制工具 (`tender-document-tool`)
**涉及模块**: `ele-ai-tender-ai`（AI服务 :8083）
**涉及组件**: `DocumentIntegration`（文档集成器）、`DocumentDataAssembler`（core模块数据组装器）、poi-tl模板引擎（file模块）

**当前状态**: `DocumentIntegration.integration()` 方法中存在 TODO，需要通过AI模型将Word文档占位符与模板填充数据key进行语义匹配。

---

## 🔴 核心问题

**问题表现**: Word模板使用中文占位符（如 `{{项目名称}}`、`{{预算金额}}`），而 `DocumentDataAssembler` 组装的数据使用英文字段名（如 `projectName`、`budget`）。两者语义相同但名称不同，导致 poi-tl 填充时无法匹配，生成的Word文档中占位符未被替换。

**根源**: 数据组装（core模块 `DocumentDataAssembler`）与模板占位符命名（Word模板文件）是两个独立体系，没有自动对齐机制。

---

## 🎯 实现目标

- 通过AI模型自动建立 Word模板占位符名 → 数据字段key 的语义映射
- 重组数据使 key 与模板占位符对齐，确保 poi-tl 正确填充
- AI匹配失败时安全回退到原始数据

---

## 🔧 技术约束

- **AI调用模式**: 必须使用项目已有的 `ModelRouter` + `AiCallRecorder` + `PromptTemplates` 模式
- **任务类型**: `DOCUMENT_INTEGRATION` 对应 `AiUsageScenario.OPTIMIZATION` 场景
- **Prompt模板**: Spring AI 的 PromptTemplate 使用 `{variableName}` 语法，JSON中的花括号必须双写 `{{` `}}`
- **poi-tl**: `{{name}}` 渲染简单值，`{{#list}}` 渲染列表循环
- **JDK**: 项目基于 JDK 21（Spring Boot 3.2.2），编译需指定 `JAVA_HOME`
- **编译命令**: `export JAVA_HOME="/d/Users/admin/.jdks/openjdk-21.0.5" && cd ele-ai-tender-system && mvn compile -pl ele-ai-tender-ai -am -q`

### 关键文件路径

| 文件 | 作用 |
|------|------|
| `ele-ai-tender-ai/.../generator/DocumentIntegration.java` | 文档集成器（本次主要修改） |
| `ele-ai-tender-ai/.../prompt/PromptTemplates.java` | Prompt模板常量 |
| `ele-ai-tender-ai/.../prompt/PromptBuilder.java` | Prompt动态构建器 |
| `ele-ai-tender-ai/.../model/ModelRouter.java` | 模型路由器 |
| `ele-ai-tender-ai/.../recorder/AiCallRecorder.java` | AI调用记录器 |
| `ele-ai-tender-core/.../engine/DocumentDataAssembler.java` | 文档数据组装器（core模块） |
| `ele-ai-tender-common/.../dto/response/WordStructureVO.java` | Word文档结构VO（含placeholders） |
| `ele-ai-tender-common/.../client/InternalFileServiceClient.java` | 文件服务客户端 |

---

## 🚫 已尝试的方案

无（本次为首次实现）

---

## ✅ 最终方案

### 设计思路

复用项目已有的 AI 调用模式（`ModelRouter` → `AiCallRecorder` → `GenerateResultParser`），新增占位符匹配场景的 Prompt，让 AI 输出「占位符名 → 数据key」的 JSON 映射，然后基于映射重组数据。

### 核心机制

```
Word模板占位符: [项目名称, 预算金额, 符合性审查项, ...]
源数据key:      [projectName, budget, complianceItems, ...]
        ↓ AI语义匹配
映射结果:       {项目名称→projectName, 预算金额→budget, 符合性审查项→complianceItems}
        ↓ 重组数据
最终数据:       {projectName:"xx", 项目名称:"xx", budget:"100", 预算金额:"100", ...}
```

**关键设计决策**:
1. 映射后的数据 **保留原始key** 作为兜底（初始化为 `new LinkedHashMap<>(sourceData)`），同时添加占位符名映射条目，确保两种 key 都能工作
2. 提取占位符名时去掉 `{{}}` 和 `#`（poi-tl列表标记），并 `distinct()` 去重
3. 数据字段描述附带值预览（字符串截断80字符，列表标记为 `[列表数据]`），帮助 AI 理解语义
4. AI匹配整体 try-catch，失败时回退到原始数据
5. `templateFileId` 元数据字段在填充前从数据中移除

---

## 📝 关键代码变更

### 1. PromptTemplates.java — 新增2个常量

- `PLACEHOLDER_MATCH`: 系统提示词，指导AI输出占位符→数据key的JSON映射
- `PLACEHOLDER_MATCH_USER`: 用户提示词模板，包含占位符列表和数据字段描述

### 2. PromptBuilder.java — 新增1个方法

- `buildPlaceholderMatch(String placeholders, String dataFields)`: 构建占位符匹配的User Prompt

### 3. DocumentIntegration.java — 主要改动

- 注入 `ModelRouter` 和 `AiCallRecorder`
- `integration()`: 分离 `templateFileId` 元数据，调用 `matchPlaceholders()` 获取匹配后数据
- 新增 `matchPlaceholders()`: 提取占位符名 → 构建Prompt → AI调用 → 解析映射JSON → 重组数据
- 新增 `formatDataField()`: 格式化数据字段描述（值预览截断80字符）

**编译状态**: ✅ 通过

---

## 🎯 当前进度

| 项目 | 状态 |
|------|------|
| Prompt模板设计 | ✅ 已完成 |
| PromptBuilder方法 | ✅ 已完成 |
| DocumentIntegration核心逻辑 | ✅ 已完成 |
| 编译验证 | ✅ 通过 |
| 集成测试 | ⏳ 待确认（需启动服务实际验证） |

---

## 💡 使用方法

### 恢复上下文

在新对话中，将本文档路径告知 Claude：
```
请阅读 docs/context/202604281330-对话上下文-DocumentIntegration占位符匹配-待确认.md 恢复上下文
```

### 编译验证

```bash
export JAVA_HOME="/d/Users/admin/.jdks/openjdk-21.0.5"
cd D:/Archives/IDEAProjects/tender-document-tool/ele-ai-tender-system
mvn compile -pl ele-ai-tender-ai -am -q
```

### 集成测试要点

1. 启动 support(:8080)、file(:8081)、core(:8082)、ai(:8083) 四个服务
2. 创建项目 → 进入文档阶段 → 触发 DOCUMENT_INTEGRATION AI任务
3. 验证生成的Word文档中占位符是否被正确替换
4. 验证AI匹配失败时是否安全回退

---

## 🐛 已知问题和待解决

1. **待集成测试**: 当前仅编译通过，未启动服务进行端到端验证
2. **AI映射准确性**: AI可能对相似语义字段产生错误映射（如 `projectCategory` vs `projectType`），需要在实际数据上验证
3. **大数据量性能**: 如果数据字段或占位符很多，AI prompt 可能变长，需关注 token 消耗
4. **列表占位符格式**: `fileStructure.getPlaceholders()` 返回的列表占位符格式（是否含 `#` 前缀）需在实际模板上确认

---

## 🚀 下一步计划

1. 启动全链路服务进行集成测试
2. 根据实际Word模板的占位符格式调整 `matchPlaceholders` 中的占位符名提取逻辑
3. 观察AI匹配准确性，必要时调整 Prompt 或增加字段描述信息
4. 考虑是否需要缓存映射结果（同一模板的映射关系固定，避免重复AI调用）

---

## 📝 备注

### 数据组装器字段清单（DocumentDataAssembler.assemble() 输出）

**简单字段**: projectName, projectCode, projectCategory, projectType, budget, tenderUnit, projectLocation, contactPerson, contactPhone, projectDescription, requirementContent, reviewType

**列表字段**: complianceItems, technicalItems, creditItems, commercialItems（均为 `List<Map<String, String>>`）

### AI调用模式参考（项目统一模式）

```java
ChatClient client = modelRouter.route(AiTaskType.XXX);
String aiOutput = aiCallRecorder.callAndRecord(
    client,
    PromptTemplates.XXX_SYSTEM,   // System Prompt
    userPrompt,                    // User Prompt
    "ROLE",                        // GENERATION/OPTIMIZATION/DETECTION/CHAT
    task.getId(),                  // taskId
    task.getCreateId(),            // userId
    task.getFileIdList()           // fileIds
);
String content = resultParser.extractJson/extractMarkdown(aiOutput);
```
