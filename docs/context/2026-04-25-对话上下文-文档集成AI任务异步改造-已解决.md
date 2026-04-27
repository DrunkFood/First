# 对话上下文：文档集成AI任务异步改造

> 生成时间：2026-04-25 | 状态：已解决

---

## 📋 问题背景

**项目**：招标文件AI编制工具（ele-ai-tender）

**当前状态**：文档集成功能原本是同步调用（core模块直接调用FileServiceClient生成Word），进度太慢，改为通过AI任务异步模式调用。

**涉及组件**：
- 后端：`ele-ai-tender-ai`（AI模块）、`ele-ai-tender-core`（核心模块）、`ele-ai-tender-common`（公共模块）
- 前端：`ele-ai-tender-frontend`（AI编制业务前端）
- 异步机制：`ai_task` 表 + `AiTaskProcessor` 轮询 + `AiTaskResultSyncHandler` 结果回写

---

## 🔴 核心问题

**问题表现**：
1. 文档集成为同步阻塞调用，用户等待时间长
2. 改造为AI任务异步模式后，代码存在残留、注释错误、缺少异常处理和通知
3. 前端未同步改造，接口返回类型不匹配（后端返回 `AiTask`，前端仍按 `DocumentPreviewVO` 处理）

**根源分析**：
- `DocumentIntegration.java` 是从评审项生成器复制粘贴改造的，残留了大量AI模型相关代码
- 前端 `PhaseDocument.vue` 没有引入 `useLatestTask` 轮询机制，无法感知异步任务状态
- `AiTaskResultSyncHandler` 中 `Long.valueOf(task.getResult())` 无异常防护
- 缺少重复任务检查和用户通知

---

## 🎯 实现目标

1. 文档集成改为AI任务异步模式，与其他AI任务类型架构一致
2. 清理残留代码和错误注释
3. 增加健壮性（异常处理、重复任务检查）
4. 前后端接口对齐，前端支持任务轮询

---

## 🔧 技术约束

- AI任务异步机制：core 写入 `ai_task` → `AiTaskProcessor` 每5秒轮询 → ai模块执行 → `AiTaskResultSyncHandler` 回写结果
- 前端轮询：`useLatestTask` composable，8秒间隔，终态自动停止
- `DocumentIntegration` 实际不调用AI模型，仅调用 `InternalFileServiceClient.generateDocument()` 生成Word
- 编译需 JDK 21：`JAVA_HOME="D:/Users/admin/.jdks/openjdk-21.0.5"`

---

## ✅ 最终方案

### 设计思路

```
原流程：Controller → Service → FileServiceClient.generateDocument() (同步)
新流程：Controller → Service → 创建AI任务 → AiTaskProcessor异步执行 → SyncHandler回写结果
```

### 核心机制

**后端**：
1. `DocumentIntegrationServiceImpl.integrate()` → 组装参数 → `aiTaskService.createTask(DOCUMENT_INTEGRATION)`
2. `AiTaskProcessor.dispatch()` → `documentIntegration.integration(task)` → `fileServiceClient.generateDocument()` → 返回文件ID
3. `AiTaskResultSyncHandler.syncDocumentIntegration()` → 解析结果 → 更新 `project.generatedFileId` → 发送通知

**前端**：
1. `PhaseDocument.vue` 引入 `useLatestTask('DOCUMENT_INTEGRATION')`
2. 点击"执行集成" → 调用API获取 `AiTask` → `setActive(task.id)` 启动轮询
3. 任务完成回调 → `loadPreviewWithRetry()` 带重试刷新预览
4. `isIntegrating` 由 `latestTask.status` 驱动，`canCreateNew` 防止重复提交

---

## 📝 关键代码变更

### 后端（11个文件）

| 文件 | 变更内容 |
|------|----------|
| `AiTaskType.java` | 新增 `DOCUMENT_INTEGRATION` 枚举值 |
| `AiUsageScenario.java` | `DOCUMENT_INTEGRATION` 映射到 `OPTIMIZATION` |
| `ResponseCode.java` | 新增 `DOCUMENT_INTEGRATE_DUPLICATE(9044)` |
| `AiTaskProcessor.java` | dispatch 增加 `DOCUMENT_INTEGRATION` 分支 |
| `DocumentIntegration.java` | 从评审项生成器重构为文件服务调用器，清理ModelRouter/AiCallRecorder/PromptBuilder/ChatClient残留 |
| `DocumentIntegrationController.java` | 返回类型 `DocumentPreviewVO` → `AiTask`，清理无用import |
| `IDocumentIntegrationService.java` | 返回类型 `DocumentPreviewVO` → `AiTask` |
| `DocumentIntegrationServiceImpl.java` | 改为创建AI任务，增加重复任务检查（`getLatestTask` + `isActive`） |
| `AiTaskResultSyncHandler.java` | 新增 `syncDocumentIntegration()`，含NumberFormatException防护 + 用户通知 |
| `MessageHelper.java` | 新增 `sendDocumentIntegrationNotice()` |
| `DocumentTrigger.java` | 适配新返回类型 `AiTask`，改进日志 |

### 前端（2个文件）

| 文件 | 变更内容 |
|------|----------|
| `src/api/document.ts` | `integrate` 返回类型 `DocumentPreviewVO` → `AiTaskVO` |
| `src/views/project/phases/PhaseDocument.vue` | 引入 `useLatestTask`，改为异步任务模式，`isIntegrating` 改为computed，增加 `loadPreviewWithRetry`，按钮增加 `canCreateNew` 防护 |

---

## 🎯 当前进度

| 项目 | 状态 |
|------|------|
| 后端：枚举扩展（AiTaskType, AiUsageScenario） | ✅ 已完成 |
| 后端：任务分发（AiTaskProcessor.dispatch） | ✅ 已完成 |
| 后端：AI模块执行器（DocumentIntegration） | ✅ 已完成 |
| 后端：core模块Service改造 | ✅ 已完成 |
| 后端：结果同步（AiTaskResultSyncHandler） | ✅ 已完成 |
| 后端：用户通知（MessageHelper） | ✅ 已完成 |
| 后端：重复任务检查 | ✅ 已完成 |
| 后端：阶段触发器（DocumentTrigger） | ✅ 已完成 |
| 前端：API类型定义对齐 | ✅ 已完成 |
| 前端：PhaseDocument异步任务模式 | ✅ 已完成 |
| 编译验证（后端 mvn compile） | ✅ 通过 |
| 类型检查（前端 vue-tsc --noEmit） | ✅ 通过 |
| 前端构建（npm run build） | ✅ 通过 |
| Git提交 | ✅ f33e6b8 |

---

## 💡 使用方法

### 启动服务验证

```bash
# 后端（需JDK 21）
cd ele-ai-tender-system/ele-ai-tender-common && JAVA_HOME="D:/Users/admin/.jdks/openjdk-21.0.5" mvn install -Dmaven.test.skip=true
cd ele-ai-tender-system/ele-ai-tender-support && JAVA_HOME="D:/Users/admin/.jdks/openjdk-21.0.5" mvn spring-boot:run  # :8080
cd ele-ai-tender-system/ele-ai-tender-file    && JAVA_HOME="D:/Users/admin/.jdks/openjdk-21.0.5" mvn spring-boot:run  # :8081
cd ele-ai-tender-system/ele-ai-tender-core    && JAVA_HOME="D:/Users/admin/.jdks/openjdk-21.0.5" mvn spring-boot:run  # :8082
cd ele-ai-tender-system/ele-ai-tender-ai      && JAVA_HOME="D:/Users/admin/.jdks/openjdk-21.0.5" mvn spring-boot:run  # :8083

# 前端
cd ele-ai-tender-frontend && npm run dev  # :5173
```

### 验证流程

1. 进入项目文档阶段 → 点击"执行集成"
2. 按钮应变为禁用状态，显示"正在集成中"
3. 等待AI任务完成（观察后端日志 `AiTaskProcessor` 和 `AiTaskResultSyncHandler`）
4. 前端自动轮询检测到完成 → 刷新预览
5. 用户收到系统通知"文档生成完成"

---

## 🐛 已知问题和待解决

1. **AiUsageScenario 分类**：`DOCUMENT_INTEGRATION` 映射到 `OPTIMIZATION`（优化），实际更接近 `GENERATION`（生成），可能影响用量统计准确性。设计决策，暂不改动。
2. **DocumentPreviewVO.htmlContent / markdownContent 字段**：后端 `getPreview` 不再填充这两个字段，前端也未使用，但类型定义仍保留。可后续清理。
3. **单测缺失**：文档集成相关模块无单元测试，建议后续补充。
4. **DocumentIntegrationController.editContent**：后端抛 `OPERATION_NOT_SUPPORTED`，前端仍有编辑入口，应考虑移除或改为其他交互方式。

---

## 🚀 下一步计划

1. 启动全链路验证（4个后端服务 + 前端），端到端测试文档集成异步流程
2. 验证 DocumentTrigger 自动触发场景（从评审项阶段推进到文档阶段）
3. 验证重复任务检查（快速连续点击"执行集成"按钮）
4. 清理 DocumentPreviewVO 中未使用的 htmlContent / markdownContent 字段
5. 补充单元测试

---

## 📝 备注

### 开发心得

1. **复制粘贴遗留风险**：`DocumentIntegration` 从评审项生成器复制时保留了 AI 模型相关代码（ModelRouter、AiCallRecorder、PromptBuilder），导致 Javadoc 和注释都指向"评审项"而非"文档集成"。改造时必须彻底清理。
2. **异步改造前端必须同步**：后端接口返回类型变更（`DocumentPreviewVO` → `AiTask`），前端必须配套引入 `useLatestTask` 轮询，否则用户无法感知任务进度和结果。
3. **`Long.valueOf()` 防护**：AI任务结果是字符串，解析为数字时必须加 `NumberFormatException` 防护，否则整个同步事务回滚会阻塞其他任务。
4. **重复任务检查应按类型精准**：`hasActiveTasks(projectId)` 检查所有类型太宽泛，用 `getLatestTask(taskType, bizId, bizType)` 按具体类型检查更合理。

### 参考资料

- 阶段流程控制器规范：`docs/rules/PHASE_FLOW_SPEC.md`
- 前端轮询模式参考：`PhaseReviewItem.vue`（评审项生成，使用 `useLatestTask`）
- AI任务架构：core 写任务 → ai 轮询执行 → core 同步结果，详见 CLAUDE.md "AI能力架构"
