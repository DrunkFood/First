# 智能检测 (Detection)

> 需求检测(2项) + 项目检测(4项) + 建议处理 + 文档修复

## 核心数据结构

**TbDetectionRecord** (`common/entity/core/TbDetectionRecord.java`)

| 字段 | 类型 | 说明 |
|------|------|------|
| `requirementId` | Long | 需求级检测时非空 |
| `projectId` | Long | 项目级检测时非空 |
| `detectionType` | String | SENSITIVE_WORD / TYPO / POLICY_REVIEW / FORMAT_CHECK |
| `contentFileId` | Long | **始终为 null**（检测内容存 content_snapshot） |
| `contentSnapshot` | text | 检测内容快照（纯文本） |
| `result` | text | 检测结果JSON（含issues数组） |
| `status` | String | 同 AiTaskStatus |
| `taskId` | Long | 关联的 ai_task.id |
| `policyFileIds` | String | 政策文件ID列表（逗号分隔） |

**代码位置**:
- 项目检测Service: `core/service/impl/DetectionServiceImpl.java`
- 需求检测Service: `core/service/impl/RequirementServiceImpl.java`（detect/accept/reject/finish方法）
- 检测引擎: `ai/processor/checker/DetectionEngine.java`
- 各检测器: `ai/processor/checker/{SensitiveWord,Typo,PolicyReview,FormatCheck}Detector.java`
- 结果解析: `core/util/DetectionResultParser.java`
- 前端检测页: `frontend/src/views/project/phases/PhaseDetection.vue` / `frontend/src/views/requirement/RequirementDetect.vue`

## 需求检测 vs 项目检测

| 对比维度 | 需求检测 | 项目检测 |
|---------|---------|---------|
| 检测项数 | 2项 | 4项（无政策文件时3项） |
| 敏感词 | ✓ SENSITIVE_WORD | ✓ SENSITIVE_WORD |
| 错别字 | ✓ TYPO | ✓ TYPO |
| 政策审查 | ✗ | ✓ POLICY_REVIEW（需选政策文件） |
| 格式检测 | ✗ | ✓ FORMAT_CHECK |
| 检测内容 | `requirement.content` | `buildDetectionContent()`: 需求内容+评审项标准纯文本 |
| 状态管理 | 无状态机，前端本地管理 | `ProjectStateMachine` 状态机 |
| 重试机制 | 无（需重新提交） | `retry()` 方法 |
| 跳过机制 | 无 | `skip()` + 警告通知 |
| 审核建议 | accept/reject | accept/reject/acceptAll |
| 路由场景 | DETECTION | DETECTION |

## 项目检测提交流程

**DetectionServiceImpl.submit()** (`core/service/impl/DetectionServiceImpl.java`):

```
1. 状态转换: → PENDING_DETECTION
2. 构建 detectionContent = buildDetectionContent(project, reviewItems)
3. 为每种检测类型创建 TbDetectionRecord + AiTask:
   - SENSITIVE_WORD → 总是创建
   - TYPO → 总是创建
   - POLICY_REVIEW → 仅当有政策文件时创建
   - FORMAT_CHECK → 总是创建
4. 状态转换: → DETECTING
```

**buildDetectionContent()** 拼接逻辑:
```
招标需求内容: \n{project.requirementContent}

评审项: 
- [符合性审查] {item.itemStandard}
- [技术标评审] {item.itemStandard}
...
```

> **重要**: `contentFileId` 始终为 null，检测内容存 `contentSnapshot`。不检测模板内容。issue 的 `locationRef` 基于 `generatedFileId` 的 Word 文件定位。

## 建议处理机制

### accept（接受建议）
**DetectionServiceImpl.acceptIssue(recordId, issueIndex)**:
1. 提取 issue 中的 `original` 和 `targeted`
2. 调用 `fileServiceClient.fixDocument(generatedFileId, replacements)` 修复Word文档
3. 修复成功 → 更新 `project.generatedFileId` 为新文件ID，handleStatus=1
4. 修复未找到原文 → handleStatus=3
5. 检查是否所有问题已处理 → 自动转 DETECTION_PASSED

### reject（拒绝建议）
**DetectionServiceImpl.rejectIssue(recordId, issueIndex)**:
1. 标记 handleStatus=2（已拒绝）
2. 检查是否所有问题已处理 → 自动转 DETECTION_PASSED

### acceptAll（批量接受）
**DetectionServiceImpl.acceptAll(projectId)**:
1. 批量标记所有 issue handleStatus=1
2. 收集所有替换项，一次性调用 `fixDocument()` 批量修复
3. 自动转 DETECTION_PASSED

## 自动转 PASSED 机制

**DetectionServiceImpl.tryTransitionToPassed(projectId)**:
- 检查 `DetectionResultParser.hasUnresolvedIssues(records)` — 是否还有未处理的问题
- 所有问题已处理且当前状态为 DETECTION_FAILED → 转为 DETECTION_PASSED
- 自动创建版本备份

**handleStatus 含义**:
| 值 | 含义 |
|----|------|
| 0 | 未处理 |
| 1 | 已接受 |
| 2 | 已拒绝 |
| 3 | 未找到原文（接受但修复失败） |

## 跳过检测

**DetectionServiceImpl.skip(projectId)**:
1. 状态转换: → DETECTION_SKIPPED
2. 发送警告通知到消息中心（`messageHelper.sendWarningNotice`）
3. 仍可发布（含警告标记）

## 重试机制

**DetectionServiceImpl.retry(projectId)**:
```
1. 状态转换: DETECTION_FAILED → IN_PROGRESS
2. 重置所有检测记录: status=PENDING, result=null, contentSnapshot=最新内容
3. 为每条记录重新创建 AiTask
4. 状态转换: IN_PROGRESS → PENDING_DETECTION → DETECTING
```

> **注意**: retry() 会重置**所有**检测记录（不仅是失败的），重新检测全部4项。

## 检测引擎（AI模块）

**DetectionEngine.detect(task)** (`ai/processor/checker/DetectionEngine.java`):
- 根据 `task.taskType` 分发到对应的 Detector
- 每个 Detector 继承 `BaseDetector`，构建特定 Prompt 调用 DETECTION 场景模型
- 返回 JSON: `{"issues": [...], "score": N}`

**Issue 结构**:
```json
{
  "issues": [
    {
      "type": "敏感词",
      "severity": "HIGH/MEDIUM/LOW",
      "original": "原文片段",
      "targeted": "建议替换",
      "description": "问题描述",
      "suggestion": "AI建议",
      "locationRef": {"paragraph": N, "text": "..."}
    }
  ],
  "score": 85
}
```

## 前端检测进度轮询

**PhaseDetection.vue** 轮询 `GET /detection/progress/{projectId}`:
- 每3秒轮询一次
- 返回各检测项状态(PENDING/PROCESSING/COMPLETED/FAILED/AI_UNAVAILABLE) + 问题数 + 评分
- 总体状态: PASSED(全部通过无问题) / FAILED(有问题) / DETECTING(进行中) / AI_UNAVAILABLE

## 注意事项

1. **contentFileId 始终为 null**: 检测内容存在 `contentSnapshot` 字段，不要尝试设置 contentFileId
2. **政策文件审查可选**: 未选择政策文件时跳过 POLICY_REVIEW，结果为 `{"issues":[],"score":100}`
3. **retry 重置所有记录**: 不是只重试失败的，而是全部重新检测
4. **10秒定时同步**: `updateDetectionPendingStatus()` 每10秒扫描 PENDING 检测记录，从 ai_task 同步状态
5. **文档修复生成新文件**: accept 时 `fixDocument()` 返回新文件ID，`project.generatedFileId` 会更新
6. **locationRef 定位**: issue 的 locationRef 基于 Word 文件段落索引，用于精准定位替换位置
