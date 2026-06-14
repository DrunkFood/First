# 项目模块 — 历史招标文件匹配 功能设计

## 背景

项目基础信息页（PhaseBasicInfo）有"历史招标文件匹配"UI 空壳，但前后端未打通：
- 前端：三种模式切换有，但无 API 调用，matchResults 永远为空
- 后端：TbProject 无匹配字段，ProjectController 无匹配端点
- ProjectServiceImpl:232 有 TODO 注释，generateRequirement() 未读取参考文档

需求模块已有完整匹配实现，项目模块复用其匹配源（AI DocumentMatchController）和前端组件（MatchModePanel）。

## 方案

TbProject 加匹配字段，匹配数据随项目信息一起保存（复用已有 PUT 端点），不新增独立匹配端点。复用 AI 模块做候选查询，项目侧只负责记录和传递。

## 数据模型

`tb_project` 新增 4 列（与 tb_requirement 对齐）：

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `match_mode` | VARCHAR(30) | NULL | AUTO_MATCH / MANUAL_SELECT / UPLOAD |
| `matched_file_id` | BIGINT | NULL | 自动/手动匹配的历史文件ID |
| `matched_similarity` | DECIMAL(5,2) | NULL | 匹配度百分比 0-100 |
| `uploaded_file_id` | BIGINT | NULL | 上传模式下的文件ID |

`TbProject` 实体对应加 4 个字段，类型同 TbRequirement。

## 后端 API 与 Service

### 不新增端点

匹配数据随项目信息一起保存，复用已有 `PUT /api/v1/projects/{id}`。前端在保存时将 matchMode / matchedFileId / uploadedFileId 一并提交，后端 update 逻辑自然写入。

- 不需要 `POST /projects/{id}/match` — 合并到项目 update
- 不需要 `GET /projects/match-files` — 前端直接调 AI 模块 `/ai-api/v1/ai/match/auto` 和 `/ai-api/v1/ai/match/manual`
- 不需要预览端点 — 复用需求模块已有逻辑

### 修改已有方法

`ProjectServiceImpl.generateRequirement()` — 解决 TODO，根据 matchMode 取参考文档传给 AI 任务：
- AUTO_MATCH / MANUAL_SELECT → 传 matchedFileId
- UPLOAD → 传 uploadedFileId
- 未匹配 → 不传参考文档

### 不新增的端点

- 不需要 `GET /projects/match-files` — 前端直接调 AI 模块 `/ai-api/v1/ai/match/auto` 和 `/ai-api/v1/ai/match/manual`
- 不需要预览端点 — 复用需求模块已有逻辑

## 前端改动

### PhaseBasicInfo.vue

替换 146-214 行内联匹配 UI，改用 MatchModePanel 组件：

```vue
<MatchModePanel
  v-model="matchMode"
  v-model:selected-file-id="selectedMatchId"
  v-model:uploaded-file-id="uploadedFileId"
  :mode="isEdit ? 'edit' : 'create'"
  @file-preview="handlePreviewMatch"
/>
```

删除：手动选择卡片列表、上传区域 el-upload、auto-match 说明文案、匹配相关 CSS。

新增状态变量：
- `matchMode` — AUTO_MATCH / MANUAL_SELECT / UPLOAD 枚举值（对齐 MatchModePanel）
- `selectedMatchId` — 匹配文件 ID
- `uploadedFileId` — 上传文件 ID

修改 `loadProject()` — 回填匹配字段：
```typescript
matchMode: project.matchMode || 'AUTO_MATCH',
selectedMatchId: project.matchedFileId || null,
uploadedFileId: project.uploadedFileId || null,
```

修改 `handleSaveAndNext()` — 保存时将匹配数据一并提交：
```typescript
await projectApi.update(props.projectId, {
  ...updateData,
  budget: toYuan(form.value.budget),
  matchMode: matchMode.value,
  matchedFileId: selectedMatchId.value,
  uploadedFileId: uploadedFileId.value,
})
```

### project.ts 类型更新

ProjectInfo 加：`matchMode`, `matchedFileId`, `matchedSimilarity`, `uploadedFileId`

## 端到端数据流

1. 用户选择匹配模式
   - 自动匹配 → 调 AI /ai/match/auto → 自动选中第一个
   - 手动选择 → 调 AI /ai/match/manual → 用户点选
   - 上传 → 上传到文件服务 → 获取 uploadedFileId
2. 用户点击"保存并继续"
   - PUT /projects/{id} 携带基础信息 + matchMode + matchedFileId + uploadedFileId（一次调用）
3. 推进阶段到需求生成
   - generateRequirement() 读取 matchMode，按模式取参考文档 ID 传给 AI 任务

## 边界情况

| 场景 | 处理 |
|------|------|
| 未选匹配模式直接保存 | 允许，matchMode 为空，AI 生成时不传参考文档 |
| 自动匹配无结果 | 展示"暂无匹配结果"，用户可切换模式 |
| 只读模式回看 | loadProject 回填匹配字段，MatchModePanel disabled |
| 编辑已有项目 | 回填之前的匹配选择，用户可更改 |
