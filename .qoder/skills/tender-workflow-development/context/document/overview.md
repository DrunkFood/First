# 文档集成与导出 (Document Integration)

> 通过 poi-tl 模板引擎填充 Word 文档 → 文件服务存储 → fileId 回写

## 核心流程

文档集成**不是** Markdown → Word 的转换流程，而是通过 **File 服务的 poi-tl 模板引擎**直接填充 Word 模板生成最终文档。

```
项目模板(TbProjectTemplate) → 获取 templateFileId
        ↓
DocumentDataAssembler.assemble(projectId) → 组装 FillData 列表
        ↓
创建 DOCUMENT_INTEGRATION AI任务（参数: templateFileId + fillDataList + projectName）
        ↓
AI模块 DocumentIntegration.integration():
  → fileServiceClient.generateDocument(templateFileId, fillDataList, "{projectName}.docx")
  → 返回 generatedFileId
        ↓
结果同步 → project.generatedFileId = generatedFileId
```

**代码位置**:
- Core服务: `core/service/impl/DocumentIntegrationServiceImpl.java`
- AI执行器: `ai/processor/generator/DocumentIntegration.java`
- 数据组装: `core/engine/DocumentDataAssembler.java`
- Controller: `core/controller/DocumentIntegrationController.java`
- 文件服务客户端: `common/client/InternalFileServiceClient.java`
- 前端: `frontend/src/views/project/phases/PhaseDocument.vue`

## 核心数据结构

### FillData (`common/dto/FillData.java`)

文档填充数据单元，每条 FillData 对应模板中的一个占位符:

| 字段 | 类型 | 说明 |
|------|------|------|
| `key` | String | 占位符名称（与Word模板中的 `{{key}}` 对应） |
| `value` | Object | 填充值（按 type 转换为具体类型） |
| `type` | String | 数据类型：text / table / image / list |

### TbProjectTemplate (`common/entity/core/TbProjectTemplate.java`)

项目模板快照:

| 字段 | 类型 | 说明 |
|------|------|------|
| `projectId` | Long | 项目ID |
| `templateId` | Long | 源模板ID |
| `templateCode` | String | 模板编码 |
| `templateName` | String | 模板名称 |
| `fileId` | Long | Word模板文件ID |
| `structureDefinition` | text | 模板结构定义 |
| `versionNo` | int | 版本号 |

## 文档集成流程详解

### 1. Core 模块 — DocumentIntegrationServiceImpl.integrate()

```java
// 1. 检查活跃任务（防止重复提交）
AiTaskVO latestTask = aiTaskService.getLatestTask(DOCUMENT_INTEGRATION, projectId, "PROJECT");
if (isActive(latestTask)) throw DOCUMENT_INTEGRATE_DUPLICATE;

// 2. 获取项目模板（必须有 Word 模板文件）
TbProjectTemplate pt = projectTemplateService.getByProjectId(projectId);
if (pt == null || pt.getFileId() == null) throw TEMPLATE_NOT_FOUND;

// 3. 组装 FillData 列表
List<FillData> fillDataList = dataAssembler.assemble(projectId);

// 4. 创建 AI 任务
DocumentIntegrationParams params = new DocumentIntegrationParams();
params.setTemplateFileId(pt.getFileId());
params.setProjectName(project.getProjectName());
params.setFillDataList(fillDataList);
return aiTaskService.createInternalTask(DOCUMENT_INTEGRATION, ...);
```

### 2. AI 模块 — DocumentIntegration.integration()

```java
// 1. 反序列化参数（FillData 的 value 按 type 转换具体类型）
List<FillData> fillDataList = deserializeFillDataList(params.getFillDataList());

// 2. 调用 File 服务生成 Word 文档
Long generatedFileId = fileServiceClient.generateDocument(
    templateFileId, fillDataList, projectName + ".docx");

// 3. 返回文件ID字符串（后续由结果同步机制回写到 project.generatedFileId）
return String.valueOf(generatedFileId);
```

> **注意**: `deserializeFillDataList()` 方法处理 Jackson 反序列化问题 — `List<FillData>` 反序列化时 value 会变成 LinkedHashMap，需按 type 转换为具体类型（TableData / ImageData 等）。

## 文档预览

**DocumentIntegrationServiceImpl.getPreview(projectId)**:
- 返回 `DocumentPreviewVO`（projectId, projectName, generatedFileId, integrated）
- `integrated` 字段 = `generatedFileId != null`

**前端 PhaseDocument.vue**:
- 预览: 通过 `generatedFileId` 调用文件服务获取 Word 文档，使用 `docx-preview` 渲染
- 导出: 下载 `generatedFileId` 对应的 Word 文件

## 文档修复机制

检测建议 accept 时，调用文件服务修复 Word 文档:

**DetectionServiceImpl.acceptIssue()**:
```java
FixReplacement replacement = new FixReplacement();
replacement.setOriginal(original);      // 原文
replacement.setTargeted(targeted);      // 替换文本
replacement.setLocationRef(locationRef); // 段落定位信息

WordFixResultVO fixResult = fileServiceClient.fixDocument(
    project.getGeneratedFileId(), List.of(replacement));

if (fixResult.getFixedCount() > 0) {
    project.setGeneratedFileId(fixResult.getFileId()); // 更新为新文件ID
}
```

**FixReplacement** (`common/dto/FixReplacement.java`):
| 字段 | 说明 |
|------|------|
| `original` | 要替换的原文 |
| `targeted` | 替换后的文本 |
| `locationRef` | LocationRefVO（段落索引、文本定位） |

**WordFixResultVO** (`common/dto/response/WordFixResultVO.java`):
| 字段 | 说明 |
|------|------|
| `fileId` | 修复后的新文件ID |
| `fixedCount` | 成功替换数 |
| `failedCount` | 替换失败数 |

## 版本快照

**ProjectVersionServiceImpl.createVersion(projectId, remark)**:
- 发布时自动将项目当前内容（JSON）写入 `tb_project_version`
- 所有检测问题处理完成后也会自动创建版本备份
- 支持版本对比（`VersionCompare.vue`）

**TbProjectVersion** (`common/entity/core/TbProjectVersion.java`):
| 字段 | 说明 |
|------|------|
| `projectId` | 项目ID |
| `versionNo` | 版本号 |
| `contentSnapshot` | 项目内容快照(JSON) |

## 前端组件

| 组件 | 说明 |
|------|------|
| `PhaseDocument.vue` | 文档集成阶段页（预览+导出+推进） |
| `DocumentPreview.vue` | 文档预览组件 |
| `DocxPreview.vue` | Word文档预览（docx-preview） |
| `VersionCompare.vue` | 版本对比组件 |

## 注意事项

1. **不是 Markdown 转 Word**: 文档集成是通过 poi-tl 模板引擎填充 Word 模板，不是 Markdown → Word 转换
2. **必须有模板文件**: 项目模板的 `fileId` 不能为空，否则抛 `TEMPLATE_NOT_FOUND`
3. **防重复提交**: 有 PENDING 或 PROCESSING 状态的文档集成任务时，抛 `DOCUMENT_INTEGRATE_DUPLICATE`
4. **在线编辑不支持**: `editContent()` 直接抛 `OPERATION_NOT_SUPPORTED`，需通过修改模板后重新生成
5. **FillData 反序列化**: Jackson 反序列化 `List<FillData>` 时 value 变成 LinkedHashMap，需按 type 转换
6. **文件ID回写**: 生成/修复后的新文件ID会回写到 `project.generatedFileId`，后续操作基于新文件
7. **REQUIRES_NEW事务**: `integrate()` 使用 `Propagation.REQUIRES_NEW`，独立事务创建AI任务
