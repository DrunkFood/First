# 项目管理模块前端页面还原实施计划

> **Goal:** 按原型设计还原项目管理模块10个前端页面的UI层
> **Architecture:** 保持Wizard模式，逐页重写template+style，保留script业务逻辑
> **Tech Stack:** Vue 3 + TypeScript + Element Plus + SCSS

---

## 文件结构

| 操作 | 文件 | 职责 |
|------|------|------|
| 修改 | `views/project/ProjectList.vue` | 项目列表 - 搜索卡片化/彩色徽章/编辑+批量删除 |
| 修改 | `views/project/ProjectCreate.vue` | 项目创建/编辑 - 两列grid/需求预览/编辑模式 |
| 修改 | `views/project/ProjectDetail.vue` | 项目详情 - 文档结构大纲 |
| 修改 | `views/project/ProjectWizard.vue` | 编制向导 - 左侧步骤导航+两栏布局 |
| 修改 | `views/project/phases/PhaseBasicInfo.vue` | 基础信息 - 模板卡片网格/匹配模式切换 |
| 修改 | `views/project/phases/PhaseRequirement.vue` | 需求生成 - 状态卡片/AI反馈/快速操作 |
| 修改 | `views/project/phases/PhaseReviewItem.vue` | 评审项 - 评分摘要栏/表格编辑/100分校验 |
| 修改 | `views/project/phases/PhaseDocument.vue` | 文档集成 - 状态卡片/目录面板/政策文件模态框 |
| 修改 | `views/project/phases/PhaseDetection.vue` | 智能检测 - 微调 |
| 修改 | `components/detection/DetectionProgress.vue` | 检测进度 - 按原型优化4项卡片+总进度 |
| 修改 | `components/detection/DetectionReport.vue` | 检测报告 - 按原型优化摘要卡片+问题列表 |

## 实施阶段

### Phase 1: 基础页面
- Task 1: ProjectList 列表页
- Task 2: ProjectCreate 创建/编辑页
- Task 3: ProjectDetail 详情页

### Phase 2: 向导外壳
- Task 4: ProjectWizard 左侧导航+两栏布局

### Phase 3: 编制步骤页面
- Task 5: PhaseBasicInfo 基础信息录入
- Task 6: PhaseRequirement 详细需求生成
- Task 7: PhaseReviewItem 评审项设置（最大改动）
- Task 8: PhaseDocument 文档集成
- Task 9: PhaseDetection 智能检测 + 子组件优化

### Phase 4: 验证
- Task 10: 端到端验证 + 样式微调
