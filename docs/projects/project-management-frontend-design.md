# 项目管理模块前端页面还原设计

> 基于原型设计索引，还原编制项目管理模块的11个前端页面。
> 策略：保持Wizard模式，逐页重写模板+样式，保留脚本业务逻辑。

## 一、范围

### 页面清单

| 序号 | 页面 | 文件路径 | 改动程度 |
|------|------|----------|----------|
| 1 | 项目列表 | `views/project/ProjectList.vue` | 中 |
| 2 | 项目创建 | `views/project/ProjectCreate.vue` | 中 |
| 3 | 项目编辑 | `views/project/ProjectCreate.vue`（复用） | 中 |
| 4 | 项目详情 | `views/project/ProjectDetail.vue` | 中 |
| 5 | 编制向导外壳 | `views/project/ProjectWizard.vue` | 大 |
| 6 | 基础信息录入 | `views/project/phases/PhaseBasicInfo.vue` | 大 |
| 7 | 详细需求生成 | `views/project/phases/PhaseRequirement.vue` | 大 |
| 8 | 评审项设置 | `views/project/phases/PhaseReviewItem.vue` | 大 |
| 9 | 文档集成 | `views/project/phases/PhaseDocument.vue` | 大 |
| 10 | 智能检测 | `views/project/phases/PhaseDetection.vue` | 小 |

### 不在范围内

- 交易需求目录生成（独立步骤）- 按用户要求忽略
- 业务需求模块、模板管理模块等 - 不在本次范围

## 二、各页面设计详情

### 2.1 项目列表（ProjectList）

**改动**：

1. 搜索区：`el-form inline` 包裹在 `el-card` 中，标题"筛选条件"
2. 类别/类型列：文本改为彩色药丸徽章（`el-tag` + `effect="dark"`）
   - 限额以下=蓝色, 产权交易=紫色, 政府采购=绿色
   - 工程=蓝色, 货物=橙色, 服务=绿色
3. 操作列：新增"编辑"按钮（路由跳转 `project/edit/:id`）
4. 批量操作：表格上方新增"批量删除"按钮，绑定 `selectedIds`
5. 分页：微调信息文案为"共 X 条记录"

**新增方法**：`handleEdit(row)`、`handleBatchDelete()`

### 2.2 项目创建（ProjectCreate）

**改动**：

1. 页面布局：表单区居中，最大宽度 1600px
2. 基本信息区：编号+名称改为两列 `el-row`/`el-col` grid 布局
3. 评审类型：新增"自动推荐"标签（`el-tag type="success" size="small"`），根据预算和类型自动推荐
4. 招标需求-引用模式：选择需求后，新增需求内容预览区（`el-card` + 格式化文本展示）
5. 底部按钮：调整为原型样式（取消 + 保存并继续）

**新增响应式变量**：`autoRecommended`（boolean）、`requirementPreviewContent`（string）

### 2.3 项目编辑（ProjectCreate 编辑模式）

**改动**：

1. 编辑模式区分：当 `isEditMode` 为 true 时，渲染独立的编辑布局
2. 项目基本信息区：编号禁用、名称可编辑、类型下拉、评审类型下拉、状态只读、描述可编辑
3. 新增"资格要求"区：大文本区，可编辑编号资格项
4. 新增"其他信息"区：创建时间/创建人/当前版本/使用模板（只读）
5. 底部按钮拆分布局：
   - 左侧："返回列表" + "查看详情"
   - 右侧："取消项目"（danger） + "保存修改"（success）

**新增响应式变量**：`qualificationRequirements`（string）

### 2.4 项目详情（ProjectDetail）

**改动**：

1. 顶部信息卡：调整为原型样式（名称+状态徽章+元数据网格）
2. 文档信息Tab：
   - 新增文档卡片（文件名、大小、页数、版本）
   - 新增文档结构大纲（章节树形展示）
   - 新增预览/导出按钮
3. 保留现有的版本对比模态框和文档预览模态框

**新增组件/数据**：章节大纲数据结构

### 2.5 编制向导外壳（ProjectWizard）

**改动**：

1. 两栏布局：左侧步骤导航面板（240px） + 右侧内容区
2. 左侧步骤导航：
   - 竖向卡片列表，每项：步骤编号 + 步骤名称
   - 当前步骤高亮（蓝色左边框 + 蓝色背景）
   - 已完成步骤显示对勾图标
   - 未到达步骤灰色
3. 右侧内容区：承载当前 Phase 组件
4. 底部操作栏：固定在内容区底部（上一步/保存草稿/下一步）

**新增样式**：左侧导航面板 CSS、步骤状态样式

### 2.6 基础信息录入（PhaseBasicInfo）

**改动**：

1. 项目基本信息表单：改为两列 grid 布局
2. 模板选择区：
   - 改为卡片网格（`el-row` + `el-col`，每个模板一张卡片）
   - 卡片内容：模板名称 + 推荐标签 + 描述 + 元信息（页数、更新日期）+ 预览按钮
   - 选中卡片蓝色边框高亮
3. 历史匹配区：
   - 3种模式单选切换：系统自动匹配 / 手动选择 / 上传文件
   - 自动匹配：显示描述文本 + 匹配按钮
   - 手动选择：文件卡片列表，每个卡片含匹配百分比+预览/选择按钮
   - 上传：拖拽上传区域（.doc/.docx，最大50MB）

**新增响应式变量**：`matchMode`（'auto'|'manual'|'upload'）、`templateList`（TemplateInfo[]）

### 2.7 详细需求生成（PhaseRequirement）

**改动**：

1. 新增生成状态卡片：
   - 图标（旋转/完成） + 状态文字 + 进度条 + "已完成 X/Y 章节"
2. Markdown编辑器区：保留 md-editor-v3，微调工具栏样式
3. 新增AI内容反馈：赞/踩按钮（`el-button` + 图标），帮助改进AI生成质量
4. AI助手侧边栏增强：
   - 新增快速操作药丸按钮（修改工程范围、修改技术要求、修改质量标准）
5. 底部操作栏调整：新增"重新生成章节"按钮（warning 样式）

**新增响应式变量**：`aiFeedbackType`（'like'|'dislike'|null）

### 2.8 评审项设置（PhaseReviewItem）

**改动**（最大改动页面）：

1. 新增评分摘要栏（顶部水平展示）：
   - 符合性审查（通过/不通过）
   - 资信评审（X分）
   - 技术评审（X分）
   - 商务评审（X分）
   - 合计总分（100分=绿色，否则橙色）
2. 新增评分说明信息框：三项合计必须为100分
3. 4个Tab改为表格编辑模式（替换现有树形编辑器）：
   - **符合性审查** Tab：序号 + 评审标准(文本区) + 操作(删除)
   - **资信评审** Tab：序号 + 评审标准(文本区) + 主观/客观(下拉) + 分值(数字) + 操作(删除)
   - **技术评审** Tab：同资信评审
   - **商务评审** Tab：序号 + 评审标准(文本区) + 分值(数字) + 操作(删除)
4. 每个Tab底部：虚线"添加评审项"按钮
5. 分值实时计算+100分校验

**替换**：`el-tree` → `el-table` 行内编辑模式

**新增方法**：`computeTotalScore()`、`validateScoreTotal()`、`addReviewItem(type)`、`deleteReviewItem(type, index)`

### 2.9 文档集成（PhaseDocument）

**改动**：

1. 新增生成状态卡片：图标 + "文档集成完成" + 100%进度条
2. 新增文档信息栏：水平展示 文档名称/大小/页数/生成时间
3. 文档预览布局重构：
   - 左侧：可切换的目录面板（章节导航，点击滚动到对应部分）
   - 中间：预览工具栏（目录开关/打印/下载/缩放）
   - 主区域：文档内容预览
4. 政策文件匹配：改为模态框形式
   - 匹配的政策文件（复选框，预选中AI推荐的）
   - 本单位政策文件（复选框）
   - 取消 + 确认并检测按钮

**新增组件**：文档目录面板（TOC Panel）

**新增响应式变量**：`showTocPanel`（boolean）、`policyModalVisible`（boolean）、`documentMeta`（object）

### 2.10 智能检测（PhaseDetection）

**改动**：

1. 检查 `DetectionProgress` 组件是否匹配原型进度样式（4项卡片+总进度）
2. 检查 `DetectionReport` 组件是否匹配原型报告样式（摘要卡片+问题列表+接受/拒绝）
3. 如不匹配，调整子组件模板

**改动程度**：小，主要是验证现有子组件

## 三、共性样式规范

### 3.1 卡片容器

```scss
.page-card {
  background: var(--app-bg-primary);
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
  border: 1px solid var(--app-border-color);
}
```

### 3.2 步骤导航面板

```scss
.step-nav {
  width: 240px;
  flex-shrink: 0;
  background: var(--app-bg-primary);
  border-radius: 8px;
  padding: 16px;
  border: 1px solid var(--app-border-color);

  .step-item {
    padding: 12px 16px;
    border-radius: 6px;
    cursor: pointer;
    border-left: 3px solid transparent;
    &.active { border-left-color: var(--app-brand); background: var(--app-brand-light); }
    &.completed { color: var(--app-success); }
  }
}
```

### 3.3 彩色徽章

```scss
// 类别
.category-限额以下 { --tag-color: var(--el-color-primary); }
.category-产权交易 { --tag-color: var(--el-color-warning); }
.category-政府采购 { --tag-color: var(--el-color-success); }

// 类型
.type-工程 { --tag-color: var(--el-color-primary); }
.type-货物 { --tag-color: var(--el-color-warning); }
.type-服务 { --tag-color: var(--el-color-success); }
```

### 3.4 评分摘要栏

```scss
.score-summary {
  display: flex;
  gap: 16px;
  .score-item {
    flex: 1;
    padding: 12px;
    border-radius: 6px;
    text-align: center;
    background: var(--app-bg-secondary);
    border-left: 3px solid var(--app-border-color);
    &.total { border-left-color: var(--app-brand); }
    &.valid { border-left-color: var(--app-success); }
    &.invalid { border-left-color: var(--app-warning); }
  }
}
```

## 四、实施顺序

```
Phase 1: 基础页面（无依赖）
  ├── ProjectList（列表页）
  ├── ProjectCreate + 编辑模式（创建/编辑页）
  └── ProjectDetail（详情页）

Phase 2: 向导外壳
  └── ProjectWizard（左侧步骤导航 + 两栏布局）

Phase 3: 编制步骤页面（按顺序）
  ├── PhaseBasicInfo（基础信息录入）
  ├── PhaseRequirement（详细需求生成）
  ├── PhaseReviewItem（评审项设置）
  ├── PhaseDocument（文档集成）
  └── PhaseDetection（智能检测）

Phase 4: 验证与修复
  └── 端到端流程验证 + 样式微调
```

## 五、风险与注意事项

1. **评审项树→表格转换**：PhaseReviewItem 从 `el-tree` 改为 `el-table` 行内编辑，需确保数据结构和API兼容
2. **模板选择改卡片**：PhaseBasicInfo 模板选择从下拉改为卡片，需确保 `templateApi.getList` 返回足够字段（描述、页数等）
3. **文档预览布局**：PhaseDocument 新增目录面板，需确保文档结构数据可从API获取
4. **主题兼容**：所有新样式使用 CSS 变量（`var(--app-*)`），确保深色/浅色主题兼容
5. **AI助手面板**：PhaseRequirement 的快速操作按钮需与现有 AiChatPanel 集成

---

*文档生成时间: 2026-04-15*
