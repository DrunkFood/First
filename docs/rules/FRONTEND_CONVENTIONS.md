# 前端编码规范

## 1. 双前端项目架构

| 项目 | 目录 | 端口 | 用途 |
|------|------|------|------|
| 支撑中心管理后台 | `ele-ai-tender-support-frontend/` | 3060 | 用户/角色/菜单/模板/模型/知识库管理 |
| AI编制业务前端 | `ele-ai-tender-frontend/` | 5173 | 项目编制/需求/评审项/文档/检测 |

两个项目独立部署，技术栈相同：Vue 3 + TypeScript + Vite + Element Plus + Pinia。

## 2. 组件规范

- 统一使用 Vue 3 `<script setup>` Composition API
- 组件文件名 PascalCase，路由文件名 kebab-case

## 3. 状态管理

- 状态变更必须通过 Pinia store
- 禁止在组件中直接读写 `localStorage`（store 文件内除外）

## 4. API 调用

- 所有后端调用在 `src/api/` 添加对应函数
- 修改后端路径/参数/代理规则时，必须同步更新 `src/api/*`

## 5. 代理规则

### AI编制业务前端（5173）

| 前端路径前缀 | 目标服务 | 路径重写 |
|-------------|----------|----------|
| `/support-api/*` | support :8080 | `/support-api/` → `/api/` |
| `/file-api/*` | file :8081 | `/file-api/` → `/api/` |
| `/core-api/*` | core :8082 | `/core-api/` → `/api/` |
| `/ai-api/*` | ai :8083 | `/ai-api/` → `/api/` |

### 支撑中心管理后台（3060）

| 前端路径前缀 | 目标服务 | 路径重写 |
|-------------|----------|----------|
| `/support-api/*` | support :8080 | `/support-api/` → `/api/` |
| `/file-api/*` | file :8081 | `/file-api/` → `/api/` |

## 6. 常用组合式函数与工具

| 函数 / 模块 | 用途 |
|------|------|
| `useLatestTask(taskType, bizId, bizType)` | 轮询最新AI任务状态，自动处理进度映射和终态判断；需求/评审项生成用它轮询 `ai_task.result` 渲染渐进式进度 |
| `useProjectPhase(projectId)` | 获取项目阶段信息和推进 |
| `src/types/ai-task.ts` | AI任务类型定义与工具函数：`AiTaskVO`/`RequirementGenerationProgressResult`、`isTaskSucceeded`/`isTaskTerminal`/`getTaskProgress`/`parseRequirementGenerationProgress`/`buildRequirementGenerationProgressMarkdown` |
| `src/utils/aiReplacement.ts` | AI助手替换工具：`extractReplaceableContents` 解析 `【可替换正文开始/结束】`多方案、`applyAiReplacement` 执行选中原文替换（去重前缀/空壳行清理） |

**AI助手替换模式**：`AiChatPanel.vue` 接收编辑器选中文本（props `selectedText`），发送时带 `replaceMode:true` 并将选中文本放入 `ChatRequest.context` 字段；AI 回复中固定标记包裹的可替换正文由 `aiReplacement.ts` 解析为多方案，UI 显示"替换方案1/2…"按钮，点击 `emit('replace', {selectedText, replacement})` 回写编辑器。需求阶段、需求编辑器、需求生成页均接入。

**渐进式生成渲染**：`PhaseRequirement.vue`/`RequirementGenerate.vue` 轮询任务 `result`，`parseRequirementGenerationProgress` 解析 `contentStage`（OUTLINE_GENERATED→...→COMPLETED），`buildRequirementGenerationProgressMarkdown` 实时渲染大纲骨架+已完成章节内容填充编辑器，覆盖层显示状态/进度条；编辑器在生成期间锁定，`COMPLETED + resultSynced=1` 后解锁。

## 7. 第三方库

| 库 | 用途 | 项目 |
|----|------|------|
| `md-editor-v3` | Markdown 编辑器 | AI编制前端 |
| `docx-preview` | Word 文档预览（`DocxPreview.vue` 组件，`renderAsync(blob, container)`） | 双前端（AI编制前端文档阶段 + 支撑前端模板预览） |
| `diff2html` | Diff 可视化对比 | AI编制前端 |

## 相关规范

- [CODE_CONVENTIONS.md](CODE_CONVENTIONS.md) — 后端编码规范
- [CORE_MODULE_SPEC.md](CORE_MODULE_SPEC.md) — 核心业务模块规范
