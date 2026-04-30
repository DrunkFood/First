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
| `/file-api/*` | file :8081 | 直接转发 |

## 6. 常用组合式函数

| 函数 | 用途 |
|------|------|
| `useLatestTask(taskType, bizId, bizType)` | 轮询最新AI任务状态，自动处理进度映射和终态判断 |
| `useProjectPhase(projectId)` | 获取项目阶段信息和推进 |

## 7. 第三方库

| 库 | 用途 | 项目 |
|----|------|------|
| `md-editor-v3` | Markdown 编辑器 | AI编制前端 |
| `docx-preview` | Word 文档预览 | AI编制前端 |
| `diff2html` | Diff 可视化对比 | AI编制前端 |

## 相关规范

- [CODE_CONVENTIONS.md](CODE_CONVENTIONS.md) — 后端编码规范
- [CORE_MODULE_SPEC.md](CORE_MODULE_SPEC.md) — 核心业务模块规范
