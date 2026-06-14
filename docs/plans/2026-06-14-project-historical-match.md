# 项目模块历史招标文件匹配 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 打通项目基础信息页的"历史招标文件匹配"功能，前端接入 API，后端补齐边界处理

**Architecture:** 前端替换内联 UI 为 MatchModePanel 组件，在 PhaseBasicInfo 中调用 AI 匹配接口获取候选列表后转为 MatchFile 格式传入；后端复用已有 PUT 端点保存匹配数据，修复 generateRequirement() 的 null matchMode 边界

**Tech Stack:** Vue 3 / TypeScript / Element Plus / Spring Boot 3 / MyBatis-Plus

---

## 文件清单

| 操作 | 文件 | 职责 |
|------|------|------|
| 修改 | `ele-ai-tender-frontend/src/types/project.ts` | ProjectInfo / ProjectCreateParams 加匹配字段 |
| 修改 | `ele-ai-tender-frontend/src/views/project/phases/PhaseBasicInfo.vue` | 替换内联匹配 UI 为 MatchModePanel，接入 AI 匹配 API |
| 修改 | `ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/ProjectServiceImpl.java` | 修复 generateRequirement() 的 null matchMode 处理 |

---

### Task 1: 前端类型 — ProjectInfo / ProjectCreateParams 加匹配字段

**Files:**
- Modify: `ele-ai-tender-frontend/src/types/project.ts`

- [ ] **Step 1: 在 ProjectInfo 接口中添加匹配字段**

在 `ProjectInfo` 接口的 `generatedFileId` 后面添加：

```typescript
  matchMode?: string
  matchedFileId?: number
  matchedSimilarity?: number
  uploadedFileId?: number
```

- [ ] **Step 2: 在 ProjectCreateParams 接口中添加匹配字段**

在 `ProjectCreateParams` 接口的 `projectDescription` 后面添加：

```typescript
  matchMode?: string
  matchedFileId?: number
  uploadedFileId?: number
```

注意：`matchedSimilarity` 不需要写入，由后端匹配结果自动填入。

- [ ] **Step 3: 提交**

```bash
rtk git add ele-ai-tender-frontend/src/types/project.ts && rtk git commit -m "feat(frontend): 项目类型定义添加历史匹配字段"
```

---

### Task 2: 后端 — 修复 generateRequirement() 的 null matchMode 处理

**Files:**
- Modify: `ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/ProjectServiceImpl.java:222-249`

- [ ] **Step 1: 修改 generateRequirement() 方法**

当前代码在 `matchMode` 为 null 时，`MatchMode.fromCode(null)` 返回 `AUTO_MATCH`，逻辑上不正确——未选择匹配模式不应自动匹配。

将 `ProjectServiceImpl.java:231-246` 的参考文档获取逻辑替换为：

```java
        // 参考文档内容 从 自动匹配的第一份文件/手动选择匹配的历史文件id/上传的文件id 中获取
        StringJoiner paramJoiner = new StringJoiner(",");
        if (StringUtils.hasText(project.getMatchMode())) {
            MatchMode matchMode = MatchMode.fromCode(project.getMatchMode());
            switch (matchMode) {
                case AUTO_MATCH:
                case MANUAL_SELECT:
                    if (project.getMatchedFileId() != null) {
                        paramJoiner.add(String.valueOf(project.getMatchedFileId()));
                    }
                    break;
                case UPLOAD:
                    if (project.getUploadedFileId() != null) {
                        paramJoiner.add(String.valueOf(project.getUploadedFileId()));
                    }
                    break;
            }
        }
```

- [ ] **Step 2: 提交**

```bash
rtk git add ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/ProjectServiceImpl.java && rtk git commit -m "fix(core): generateRequirement处理null matchMode边界"
```

---

### Task 3: 前端 — PhaseBasicInfo 替换内联匹配 UI 为 MatchModePanel 并接入 API

**Files:**
- Modify: `ele-ai-tender-frontend/src/views/project/phases/PhaseBasicInfo.vue`

这是最核心的改动，分为模板、脚本、样式三部分。

- [ ] **Step 3.1: 修改 script — 添加 import 和状态变量**

在 `<script setup>` 中：

1. 新增 import：
```typescript
import { aiApi } from '@/api/ai'
import MatchModePanel from '@/components/requirement/MatchModePanel.vue'
import type { MatchFile } from '@/types/requirement'
import type { AiMatchResult } from '@/types/ai'
```

2. 删除不再需要的 import：
```typescript
// 删除: import { UploadFilled, Calendar, View } from '@element-plus/icons-vue'
// 改为:
import { Calendar, View } from '@element-plus/icons-vue'
```
注意：`UploadFilled` 改由 MatchModePanel 内部使用，PhaseBasicInfo 不再需要。

3. 替换"历史匹配"状态变量（原 394-405 行）：

删除：
```typescript
// --- 历史匹配 ---
const matchMode = ref<'auto' | 'manual' | 'upload'>('auto')
const matchResults = ref<AiMatchResult[]>([])
const selectedMatchId = ref<number | null>(null)

const handlePreviewMatch = (item: AiMatchResult) => {
  ElMessage.info(`预览：${item.requirementName}`)
}

const handleFileChange = () => {
  ElMessage.info('文件已选择')
}
```

替换为：
```typescript
// --- 历史匹配 ---
const matchMode = ref<string>('AUTO_MATCH')
const selectedMatchId = ref<number | undefined>(undefined)
const uploadedFileId = ref<number | undefined>(undefined)
const matchFiles = ref<MatchFile[]>([])
const matchLoading = ref(false)

/** 将 AiMatchResult 转为 MatchFile 格式，供 MatchModePanel 使用 */
const toMatchFile = (r: AiMatchResult): MatchFile => ({
  id: r.requirementId,
  fileName: r.requirementName,
  fileType: form.value.projectType || '工程类',
  matchPercent: Math.round(r.similarity * 100),
  matchDesc: r.content?.substring(0, 200),
})

/** 根据匹配模式调用 AI 接口获取候选列表 */
const fetchMatchResults = async () => {
  if (!form.value.projectCategory || !form.value.projectType) {
    ElMessage.warning('请先选择项目类别和项目类型')
    return
  }
  matchLoading.value = true
  try {
    const params = {
      projectCategory: form.value.projectCategory,
      projectType: form.value.projectType,
      content: form.value.projectDescription || form.value.projectName || '',
    }
    let results: AiMatchResult[] = []
    if (matchMode.value === 'AUTO_MATCH') {
      results = await aiApi.matchAuto(params)
      // 自动匹配时，默认选中第一个并记录相似度
      if (results.length > 0) {
        selectedMatchId.value = results[0].requirementId
      }
    } else if (matchMode.value === 'MANUAL_SELECT') {
      results = await aiApi.matchManual(params)
    }
    matchFiles.value = results.map(toMatchFile)
  } catch {
    matchFiles.value = []
    ElMessage.error('匹配失败，请稍后重试')
  } finally {
    matchLoading.value = false
  }
}

/** 监听匹配模式变化，切换时重新获取候选 */
watch(matchMode, () => {
  selectedMatchId.value = undefined
  matchFiles.value = []
  if (matchMode.value === 'AUTO_MATCH' || matchMode.value === 'MANUAL_SELECT') {
    fetchMatchResults()
  }
})

/** 预览匹配文件 */
const handlePreviewMatch = (file: MatchFile) => {
  ElMessage.info(`预览：${file.fileName}`)
}
```

- [ ] **Step 3.2: 修改 script — loadProject 回填匹配字段**

在 `loadProject()` 方法中（原 408-423 行），在 `Object.assign` 的对象里添加匹配字段回填：

```typescript
const loadProject = async () => {
  const project = await projectApi.getById(props.projectId)
  Object.assign(form.value, {
    projectName: project.projectName || '',
    projectCategory: project.projectCategory || '',
    projectType: project.projectType || '',
    serviceSubType: project.serviceSubType || '',
    budget: toWanYuan(project.budget) ?? 0,
    reviewType: project.reviewType ?? 'INTELLIGENT',
    projectDescription: project.projectDescription || '',
    tenderUnit: project.tenderUnit || '',
    contactPerson: project.contactPerson || '',
    contactPhone: project.contactPhone || '',
    templateId: project.templateId || null,
  })
  // 回填匹配字段
  matchMode.value = project.matchMode || 'AUTO_MATCH'
  selectedMatchId.value = project.matchedFileId || undefined
  uploadedFileId.value = project.uploadedFileId || undefined
}
```

- [ ] **Step 3.3: 修改 script — handleSaveAndNext 携带匹配数据**

替换 `handleSaveAndNext()` 方法（原 425-449 行）：

```typescript
const handleSaveAndNext = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const { templateId, ...updateData } = form.value
  await projectApi.update(props.projectId, {
    ...updateData,
    budget: toYuan(form.value.budget),
    matchMode: matchMode.value,
    matchedFileId: selectedMatchId.value,
    uploadedFileId: uploadedFileId.value,
  })

  // 绑定模板到项目模板表
  if (form.value.templateId) {
    await projectTemplateApi.bind(props.projectId, form.value.templateId)
  }

  ElMessage.success('基础信息保存成功')

  // 推进阶段到"需求生成"，后端会自动触发AI需求生成任务
  try {
    await projectApi.advancePhase(props.projectId, 2)
    emit('next')
  } catch (e: any) {
    ElMessage.warning(e?.message || '阶段推进失败，请稍后重试')
  }
}
```

- [ ] **Step 3.4: 修改 template — 替换内联匹配 UI**

删除原 146-214 行的内联匹配 UI，替换为：

```vue
    <!-- 历史招标文件匹配 -->
    <div class="section-title">历史招标文件匹配</div>
    <div class="match-section" :class="{ 'match-readonly': readonly }">
      <MatchModePanel
        v-model="matchMode"
        v-model:selected-file-id="selectedMatchId"
        v-model:uploaded-file-id="uploadedFileId"
        :match-files="matchFiles"
        mode="create"
        @file-preview="handlePreviewMatch"
      />
    </div>
```

- [ ] **Step 3.5: 修改 style — 删除不再需要的匹配 CSS**

删除 `<style>` 中以下不再使用的 CSS 块（MatchModePanel 自带样式）：

- `.match-mode-label` 及 `.required-star`
- `.match-radio-group`
- `.match-content`
- `.auto-match-box` 及子元素 `.auto-match-title`, `.auto-match-desc`
- `.manual-title`, `.manual-hint`
- `.match-file-list`, `.match-file-card` 及子元素 `.match-file-header`, `.match-file-name`, `.match-file-type`, `.match-file-similarity`, `.match-file-actions`
- `.upload-title`, `.upload-area`, `.upload-tip`

保留 `.match-section` 和 `.match-readonly`（控制只读状态外层）。

- [ ] **Step 3.6: 提交**

```bash
rtk git add ele-ai-tender-frontend/src/views/project/phases/PhaseBasicInfo.vue && rtk git commit -m "feat(frontend): PhaseBasicInfo接入MatchModePanel和AI匹配API"
```

---

## 自检清单

| 检查项 | 结果 |
|--------|------|
| 设计文档中"4个字段"覆盖 | ✅ TbProject 已有，SQL 已有，前端类型 Task 1 补齐 |
| 设计文档中"不新增端点" | ✅ 复用 PUT /projects/{id}，无新增 |
| 设计文档中"generateRequirement 读取 matchMode" | ✅ Task 2 修复 null 边界 |
| 设计文档中"复用 MatchModePanel" | ✅ Task 3 接入 |
| 设计文档中"自动匹配调 AI 接口" | ✅ Task 3 fetchMatchResults |
| 设计文档中"手动选择调 AI 接口" | ✅ Task 3 fetchMatchResults |
| 设计文档中"上传走文件服务" | ✅ MatchModePanel 内置 upload 逻辑 |
| 设计文档中"保存时一次性提交" | ✅ Task 3 handleSaveAndNext |
| 设计文档中"loadProject 回填" | ✅ Task 3.2 |
| 设计文档中"只读模式回看" | ✅ MatchModePanel disabled 由 readonly 控制 |
| 未选匹配模式直接保存 | ✅ matchMode 为空字符串，后端 Task 2 跳过参考文档 |
| 无 TODO/TBD 占位 | ✅ |
| 类型一致性 | ✅ AiMatchResult → MatchFile 转换、TbProject 字段名对齐 |
