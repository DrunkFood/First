# 核心业务模块规范

## 1. 定位

`ele-ai-tender-core` (端口 8082) 负责项目管理的业务编排：项目管理、业务需求编制、评审项管理、文档集成、检测管理、AI任务管理、AI内容反馈、用户消息、用户政策文件、项目模板快照。

AI能力由 ai 模块通过 `ai_task` 表异步解耦提供，core 模块负责"何时生成"——组装参数、写入任务、管理记录、版本快照。

## 2. 当前接口（`/api/v1`）

### 2.1 项目管理 (`/api/v1/projects`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/projects` | 分页查询项目列表 |
| GET | `/api/v1/projects/{id}` | 获取项目详情 |
| POST | `/api/v1/projects` | 创建项目 |
| PUT | `/api/v1/projects/{id}` | 更新项目 |
| DELETE | `/api/v1/projects` | 批量删除项目 |
| GET | `/api/v1/projects/{id}/phase` | 获取项目当前阶段信息 |
| PUT | `/api/v1/projects/{id}/phase` | 推进项目阶段（RequestBody: targetPhase + context） |
| PUT | `/api/v1/projects/{id}/status` | 变更项目状态 |
| POST | `/api/v1/projects/{id}/requirement-generate` | 提交AI生成需求 |
| POST | `/api/v1/projects/{id}/cancel` | 取消项目 |
| POST | `/api/v1/projects/{id}/publish` | 发布项目 |
| POST | `/api/v1/projects/{id}/archive` | 归档项目 |
| GET | `/api/v1/projects/{id}/versions` | 获取项目版本历史 |
| POST | `/api/v1/projects/{id}/export` | 导出项目招标文件 |

### 2.2 业务需求 (`/api/v1/requirements`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/requirements` | 分页查询需求列表 |
| GET | `/api/v1/requirements/{id}` | 获取需求详情 |
| POST | `/api/v1/requirements` | 创建需求 |
| PUT | `/api/v1/requirements/{id}` | 更新需求 |
| DELETE | `/api/v1/requirements/{id}` | 删除需求 |
| GET | `/api/v1/requirements/match-files` | 获取匹配文件列表 |
| POST | `/api/v1/requirements/{id}/match` | 匹配历史模板 |
| POST | `/api/v1/requirements/{id}/generate` | 提交AI生成需求任务 |
| POST | `/api/v1/requirements/{id}/auto-save` | 自动保存草稿 |
| GET | `/api/v1/requirements/{id}/auto-save` | 获取自动保存内容 |
| DELETE | `/api/v1/requirements/{id}/auto-save` | 清除自动保存内容 |
| POST | `/api/v1/requirements/{id}/detect` | 提交需求检测（敏感词+错别字） |
| POST | `/api/v1/requirements/{id}/detect/{recordId}/accept` | 接受需求检测建议 |
| POST | `/api/v1/requirements/{id}/detect/{recordId}/reject` | 拒绝需求检测建议 |
| GET | `/api/v1/requirements/{id}/detect/records` | 获取需求检测记录列表 |
| POST | `/api/v1/requirements/{id}/detect/finish` | 完成需求检测 |
| GET | `/api/v1/requirements/{id}/export` | 导出需求文档 |

### 2.3 评审项 (`/api/v1/review-items`)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/review-items` | 创建评审项 |
| GET | `/api/v1/review-items/{projectId}` | 获取项目评审项树 |
| PUT | `/api/v1/review-items/{id}` | 更新评审项 |
| DELETE | `/api/v1/review-items/{id}` | 删除评审项 |
| POST | `/api/v1/review-items/{projectId}/generate` | 提交AI生成评审项 |
| POST | `/api/v1/review-items/batch` | 批量创建评审项 |
| PUT | `/api/v1/review-items/batch` | 批量更新评审项 |

### 2.4 智能检测 (`/api/v1/detection`)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/detection/submit/{projectId}` | 提交最终文档检测 |
| GET | `/api/v1/detection/progress/{projectId}` | 获取检测进度 |
| GET | `/api/v1/detection/report/{projectId}` | 获取检测报告 |
| POST | `/api/v1/detection/{recordId}/accept` | 接受检测建议 |
| POST | `/api/v1/detection/{recordId}/reject` | 拒绝检测建议 |
| POST | `/api/v1/detection/accept-all/{projectId}` | 一键接受全部建议 |
| POST | `/api/v1/detection/skip/{projectId}` | 跳过检测 |
| POST | `/api/v1/detection/retry/{projectId}` | 重新检测 |

> 检测全链路详见 [DETECTION_FLOW_SPEC.md](DETECTION_FLOW_SPEC.md)。

### 2.5 文档集成 (`/api/v1/documents`)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/documents/integrate/{projectId}` | 执行文档集成（异步，返回AiTask） |
| GET | `/api/v1/documents/preview/{projectId}` | 获取集成预览 |
| PUT | `/api/v1/documents/edit/{projectId}` | 编辑集成后的文档内容 |

### 2.6 项目模板快照 (`/api/v1/project-templates`)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/project-templates/bind` | 项目绑定模板 |
| GET | `/api/v1/project-templates/project/{projectId}` | 获取项目模板 |

### 2.7 模板查询 (`/api/v1/templates`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/templates` | 分页查询模板列表 |
| GET | `/api/v1/templates/{id}` | 获取模板详情 |
| GET | `/api/v1/templates/default` | 获取默认模板 |

### 2.8 AI任务管理 (`/api/v1/ai-tasks`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/ai-tasks/{id}` | 查询任务状态 |
| POST | `/api/v1/ai-tasks/{id}/skip` | 跳过任务(降级手动) |
| GET | `/api/v1/ai-tasks/latest` | 查询业务实体的最新AI任务 |

### 2.9 AI内容反馈 (`/api/v1/feedback`)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/feedback` | 提交或更新反馈 |
| GET | `/api/v1/feedback` | 查询当前用户对某个目标的反馈状态 |

### 2.10 消息中心 (`/api/v1/messages`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/messages` | 分页查询消息 |
| PUT | `/api/v1/messages/{id}/read` | 标记已读 |
| PUT | `/api/v1/messages/read-all` | 全部标记已读 |
| GET | `/api/v1/messages/unread-count` | 未读消息数 |
| DELETE | `/api/v1/messages/{id}` | 删除消息 |

### 2.11 用户政策文件 (`/api/v1/policy-files`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/policy-files` | 分页查询当前用户的政策文件 |
| POST | `/api/v1/policy-files` | 上传政策文件 |
| GET | `/api/v1/policy-files/{id}` | 查看详情 |
| DELETE | `/api/v1/policy-files/{id}` | 删除政策文件 |
| PUT | `/api/v1/policy-files/{id}/status` | 启用/禁用 |
| GET | `/api/v1/policy-files/all` | 获取全部可用政策文件（系统级+用户级合并） |
| GET | `/api/v1/policy-files/knowledge-policy` | 获取知识库中所有政策类文档 |

## 3. 当前关键表

### 核心业务表（`tb_*`）

| 表名 | 用途 |
|------|------|
| `tb_project` | 项目表：project_code / project_name / project_category / project_type / service_sub_type / budget / status / current_phase / progress / template_id / requirement_id / requirement_content / generated_file_id |
| `tb_project_version` | 项目版本表：project_id / version_no / content_snapshot(JSON) / change_description |
| `tb_requirement` | 业务需求表：requirement_name / project_category / project_type / service_sub_type / budget / requirement_description / match_mode / matched_file_id / matched_similarity / uploaded_file_id / content / auto_save_content / status / progress（**无 projectId**，需求与项目通过 `tb_project.requirement_id` 单向关联） |
| `tb_project_template` | 项目模板快照表：project_id / template_id / template_name / project_category / project_type / file_id / content / structure_definition(JSON) / version_no |
| `tb_detection_record` | 检测记录表：requirement_id / project_id / detection_type / content_snapshot / result(JSON) / status / task_id / policy_file_ids / started_at / completed_at |
| `tb_project_review_item` | 评审项表：project_id / parent_id / level / item_name / item_content / sort_order / review_type / score / max_score / weight / subjectivity / is_required |
| `tb_policy_file` | 用户政策文件表：file_name / file_category / applicable_category（多选，逗号分隔 `SMALL_TRADE,GOVERNMENT_PROCUREMENT`，VARCHAR(100)）/ file_id / file_size / file_type / description / user_id / status |

所有表继承 `BaseEntity` 基础字段（`create_time`、`modify_time`、`ver`、`is_delete` 等）。

## 4. 核心约束

### 4.1 项目状态流转

```
DRAFT → IN_PROGRESS → PENDING_DETECTION → DETECTING → DETECTION_PASSED / DETECTION_FAILED / DETECTION_SKIPPED → PUBLISHED → ARCHIVED / CANCELLED
```

- 状态流转必须在 Service 层校验，不允许跳过中间状态
- `DETECTION_FAILED` 可通过 `DETECTION_PASSED`（所有问题处理完毕后自动转换）或 `IN_PROGRESS`（重试检测时）转出

### 4.2 编制阶段流转

```
BASIC_INFO(1) → REQUIREMENT(2) → REVIEW_ITEM(3) → DOCUMENT(4) → DETECTION(5)
```

- 阶段只能顺序推进，不能跳跃或倒退
- 每个阶段有独立的 PhaseTrigger，进入新阶段时自动触发AI任务
- Phase 与 Status 联动：进入编制阶段→IN_PROGRESS，进入检测阶段→DETECTING
- 详见 [PHASE_FLOW_SPEC.md](PHASE_FLOW_SPEC.md)

### 4.3 项目与需求的关系

项目与需求是**单向关联 + 内容快照**模型：

- **创建项目时**：可通过引入模式关联已有需求（设置 `requirementId`），也可不关联
- **进入需求阶段时**：有关联需求→复制内容到 `project.requirementContent`；无关联→触发 AI 任务 `PROJECT_REQUIREMENT_GENERATE`
- **进入需求阶段后**：项目和需求再无关联，后续阶段均从 `project.requirementContent` 读取
- `requirementSource` 标记 `REFERENCE`（引入已有需求）或 `SYSTEM_GENERATE`（AI生成）
- `tb_requirement` 表**无 `project_id` 字段**

### 4.4 AI任务进度与终态规则

AI任务通过 `status` + `result_synced` 双维度判断真实进度：

**状态流转**：
```
PENDING → PROCESSING → COMPLETED → (result_synced: 0→1/2)
                     → FAILED / AI_UNAVAILABLE / SKIPPED
```

**进度映射**（前端 `getTaskProgress`）：

| status | result_synced | 进度 | 含义 |
|--------|--------------|------|------|
| PENDING | — | 10% | 已入队待处理 |
| PROCESSING | — | 60% | AI正在执行 |
| COMPLETED | 0 | 90% | AI完成，等待结果同步到业务表 |
| COMPLETED | 1 | 100% | 同步成功，业务数据可读 |
| COMPLETED | 2 | 0% | 同步失败，等同于任务失败 |
| FAILED/AI_UNAVAILABLE/SKIPPED | — | 0% | 终态失败 |

**关键判断**：
- `isTaskSucceeded(task)`：`status=COMPLETED && resultSynced=1`
- `isTaskTerminal(task)`：FAILED/AI_UNAVAILABLE/SKIPPED 或 COMPLETED+resultSynced≠0
- 前端 `onTaskSucceeded` 回调仅在 `resultSynced=1` 时触发

**检测场景例外**：`DetectionProgress` 使用独立的 `/v1/detection/progress/{projectId}` API，COMPLETED 直接为 100%。

### 4.5 评审项三级结构

- 第一级：`level=1`, `parent_id=0`
- 第二级：`level=2`, `parent_id=一级ID`
- 第三级：`level=3`, `parent_id=二级ID`
- 删除父级时必须检查是否存在子级

### 4.6 评审项模板配置（review_config）

评审项的生成受模板 `review_config` JSON 字段控制，配置存储在 `sup_template` 和 `tb_project_template` 中：

```json
{
  "reviewTypes": [
    {"reviewType": "COMPLIANCE", "enabled": true, "generateStandard": true},
    {"reviewType": "TECHNICAL", "enabled": true, "generateStandard": false},
    {"reviewType": "CREDIT", "enabled": false, "generateStandard": true},
    {"reviewType": "COMMERCIAL", "enabled": true, "generateStandard": false}
  ]
}
```

| 字段 | 作用 |
|------|------|
| `enabled` | 控制该评审类型是否启用 |
| `generateStandard` | false时，AI不生成该类型的评审项，插入占位一级节点 |

**向后兼容**：`review_config` 为 null 时全链路回退到"全部启用"默认行为。

**DTO**：`ReviewConfig`（fromJson/isEnabled/isGenerateStandard/getEnabledTypes/defaultConfig）、`ReviewTypeConfig`

### 4.7 AI流式响应

- SSE接口超时时间推荐60秒
- 必须处理客户端断开连接的情况
- AI生成内容必须有人工审核机制

### 4.8 知识库向量化

- 文档分块大小：500-1000字/块
- 向量检索返回 Top-K（推荐K=5）
- 向量化过程异步执行

### 4.9 Token管理

- 每个 AI 模型配置独立的 Token 用量统计
- 支持按用户/项目设置 Token 上限
- Token 使用量缓存到 Redis：`ai:token:limit:{user_id}:{date}`

### 4.10 项目创建校验

`ProjectServiceImpl.create()` 通过 `validateProjectRequiredFields()` 校验：

- **项目编号 `projectCode` 必填**：不再允许空编号时自动生成，由前端填写（不可重复）
- **评审方式 `reviewType` 必填**：前端默认 `MANUAL`（人工评审），预算 ≥ 500 万或工程/货物类自动推荐人工评审
- **编号唯一性含已删除记录**：`TbProjectMapper.countByProjectCodeIncludingDeleted()` 直接查全表（绕过 MyBatis-Plus 逻辑删除过滤），避免与已删除项目编号冲突
- **并发安全**：`insert` 捕获 `DataIntegrityViolationException`，检测 `uk_project_code` 唯一索引冲突并转为友好业务异常

> 预算 `budget` 后端不强制必填（`DECIMAL(15,2) DEFAULT NULL`），必填由前端表单校验保证。

### 4.11 政策文件适用类别多选筛选

`applicable_category` 字段（`tb_policy_file` / `sup_policy_file`）从单选改为**逗号分隔多选**，配套 `CommaSeparatedFieldSql` 工具类（`common/util`）：

- `contains(columnName)`：生成参数化 SQL 片段 `CONCAT(',', REPLACE(col, ' ', ''), ',') LIKE CONCAT('%,', {0}, ',%')`，精确匹配逗号分隔的某个 token，避免部分匹配
- `containsValue(fieldValue, expectedValue)`：Java 端按逗号 split + trim 后精确比较

`PolicyFileServiceImpl.getPage()` / `getAllAvailable()` 用 `wrapper.apply(CommaSeparatedFieldSql.contains("applicable_category"), category)` 筛选；`getAllAvailable()` 额外匹配 `NULL` 或空字符串（不限类别）。`getAllAvailable` 按项目类别返回适用政策文件，供检测阶段使用。

## 5. Redis 数据结构

```
ai:task:queue:{task_id}        - Hash  AI任务队列
ai:task:status:{task_id}       - String 任务状态
ai:assistant:session:{id}      - Hash  AI助手会话上下文
ai:assistant:history:{id}      - List  对话历史
ai:detection:result:{proj_id}  - Hash  检测结果缓存
ai:token:limit:{user_id}:{date} - String 当日Token使用量
```

## 6. AI模型配置

### 6.1 模型类型

- `LOCAL`: 本地微调模型（处理大批量生成任务）
- `CLOUD`: 云端大模型如 DeepSeek（处理优化和检测任务）
- `PRIVATE`: 私有化部署模型

### 6.2 使用场景

AiTaskType 枚举定义了所有AI任务类型：
- `REQUIREMENT_GENERATE`: 需求生成
- `PROJECT_REQUIREMENT_GENERATE`: 项目需求生成
- `REVIEW_ITEM_GENERATE`: 评审项生成
- `DOCUMENT_INTEGRATION`: 文档集成
- `DETECTION_SENSITIVE_WORD`: 敏感词检测
- `DETECTION_TYPO`: 错别字检测
- `DETECTION_POLICY_REVIEW`: 政策文件审查
- `DETECTION_FORMAT_CHECK`: 格式规范检测
- `TEXT_OPTIMIZE`: 文本优化

AiUsageScenario 枚举定义模型使用场景：
- `GENERATION`: 内容生成
- `OPTIMIZATION`: 文本优化
- `DETECTION`: 智能检测

### 6.3 模型路由

```
任务类型 → ModelRouter → DynamicChatClientFactory → 本地模型(LOCAL) / 云端模型(CLOUD) / 私有化模型(PRIVATE)
```

- 模型路由由 `ModelRouter` + `DynamicChatClientFactory` 动态选择
- `ModelConfigCacheService` 缓存模型配置，定期从 `sup_model_config` 刷新
- 路由规则通过 `sup_model_route_rule` 配置主备模型

## 7. 文档生成流程

```
Word模板上传 → WordStructureParser解析模板结构 → DocumentDataAssembler组装数据 → poi-tl填充Word模板 → 导出.docx
```

- 数据组装由 core 模块的 `DocumentDataAssembler` 完成
- Word生成使用 file 模块的 `WordTemplateEngine`(poi-tl)
- core 模块的 `WordDocumentGenerator` + `MarkdownTemplateEngine` 用于需求导出等场景
- 生成完成后固化版本快照到 `tb_project_version`

## 8. 排障原则

- **项目状态异常** → 查 `tb_project.status`，检查状态流转是否合法
- **阶段推进失败** → 查 PhaseFlowController 日志，详见 [PHASE_FLOW_SPEC.md](PHASE_FLOW_SPEC.md)
- **需求阶段内容为空** → 查 `tb_project.requirement_id`：有值→查需求 content；无值→查 AI 任务状态
- **AI生成失败** → 查 `sup_model_config` 配置，检查 Token 用量是否超限
- **评审项结构错误** → 查 `tb_project_review_item.parent_id` 和 `level`
- **检测相关问题** → 详见 [DETECTION_FLOW_SPEC.md](DETECTION_FLOW_SPEC.md)

## 相关规范

- [CODE_CONVENTIONS.md](CODE_CONVENTIONS.md) — 通用编码规范
- [PHASE_FLOW_SPEC.md](PHASE_FLOW_SPEC.md) — 阶段流程控制器规范
- [DETECTION_FLOW_SPEC.md](DETECTION_FLOW_SPEC.md) — 检测全链路规范
- [AI_MODULE_SPEC.md](AI_MODULE_SPEC.md) — AI服务模块规范
- [FILE_SERVICE_SPEC.md](FILE_SERVICE_SPEC.md) — 文件服务模块规范
