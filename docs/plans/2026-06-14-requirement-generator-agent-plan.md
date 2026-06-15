# 需求生成器 Agent 化改造 - 实施计划

> 日期: 2026-06-14
> 对应设计: docs/projects/2026-06-14-requirement-generator-agent-design.md

## 实施步骤

### Step 1: 新增 DTO 类（common 模块）

**文件**: `ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/dto/ai/OutlineResult.java`

- 新增 `OutlineResult` 类，包含 `projectOverview`(String) 和 `chapters`(List\<RequirementOutline\>)
- 实现 `AiTaskParams` 接口

**文件**: `ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/dto/ai/RequirementOutline.java`

- 新增 `RequirementOutline` 类，包含字段: `chapterKey`(String), `chapterTitle`(String), `corePoints`(String), `estimatedWords`(int)
- 实现 `AiTaskParams` 接口

**验证**: 编译通过

---

### Step 2: 新增 Prompt 模板（ai 模块）

**文件**: `ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/processor/prompt/SystemPromptTemplates.java`

新增 3 个常量:
- `REQUIREMENT_OUTLINE_GENERATE` — 大纲生成 System Prompt
- `REQUIREMENT_CHAPTER_GENERATE` — 分章生成 System Prompt
- `REQUIREMENT_REVIEW` — 审查修订 System Prompt

注意: JSON 格式示例中的花括号必须双花括号转义 `{{` `}}`（Spring AI PromptTemplate 要求）

**文件**: `ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/processor/prompt/UserPromptTemplates.java`

新增 3 个常量:
- `REQUIREMENT_OUTLINE_GENERATE_USER` — 参数: projectName, projectType, projectCategory, budget, description
- `REQUIREMENT_CHAPTER_GENERATE_USER` — 参数: projectOverview, outlineDirectory, chapterTitle, corePoints, estimatedWords
- `REQUIREMENT_REVIEW_USER` — 参数: projectName, projectType, projectCategory, budget, fullContent

**验证**: 编译通过

---

### Step 3: 新增 PromptBuilder 方法（ai 模块）

**文件**: `ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/processor/prompt/PromptBuilder.java`

新增 3 个方法:
- `buildOutline(projectName, projectType, projectCategory, budget, description)` → String
- `buildChapter(projectOverview, outlineDirectory, chapterTitle, corePoints, estimatedWords)` → String
- `buildReview(projectName, projectType, projectCategory, budget, fullContent)` → String

同时新增辅助方法:
- `buildOutlineDirectory(List<RequirementOutline> chapters)` → String — 将章节列表格式化为目录文本

**验证**: 编译通过

---

### Step 4: 重写 RequirementGenerator（ai 模块，核心）

**文件**: `ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/processor/generator/RequirementGenerator.java`

重写 `generate(AiTask)` 方法，实现三步式编排:

```
generate(task):
  1. 解析参数 RequirementGenerateParams
  2. 获取文件内容 fileContent (通过 FileContentService)
  3. Step 1: generateOutline() → OutlineResult
     - 调用AI生成大纲JSON
     - 解析为 OutlineResult
     - 解析失败时降级使用 FallbackOutlineProvider
  4. Step 2: generateChapters() → List<String>
     - CompletableFuture 并行生成所有章节
     - 每个章节: 单独AI调用 + 文件内容注入
     - 单章节失败重试1次，仍失败用占位文本
     - allOf() 等待所有章节完成
  5. 拼接: assembleFullContent() → String
     - 按大纲顺序拼接，章节间 --- 分隔
  6. Step 3: reviewAndRefine() → String
     - AI审查输出修订列表JSON
     - 代码层按 original→revised 逐项替换
     - 替换失败的修订跳过并记录警告
  7. 返回 resultParser.toJsonResult("content", finalContent)
```

新增依赖注入:
- `ObjectMapper` — 解析大纲和修订列表JSON

注意: 不需要额外注入 FileContentService。所有AI调用统一走 `aiCallRecorder.callAndRecord()`，
该方法内部已注入 FileContentService 并自动拼接文件内容到 userPrompt 末尾。
各步骤调用时传入 task.getFileIdList() 即可。

新增私有方法:
- `generateOutline(params, client, fileContent)` → OutlineResult
- `generateChapters(outline, params, client, fileContent)` → List\<String\>
- `generateSingleChapter(chapter, outline, params, client, fileContent)` → String (带重试)
- `assembleFullContent(outline, chapters)` → String
- `reviewAndRefine(fullContent, params, client)` → String
- `applyRevisions(fullContent, revisionsJson)` → String
- `buildFallbackOutline(params)` → OutlineResult (降级大纲)

**验证**: 编译通过

---

### Step 5: 调整超时配置（core 模块）

**文件**: `ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/service/impl/AiTaskServiceImpl.java`

修改 `createTask()` 方法:
- 需求生成类任务(REQUIREMENT_GENERATE / PROJECT_REQUIREMENT_GENERATE)超时设为 30 分钟
- 其他任务保持 10 分钟

```java
// 替换 task.setTimeoutMinutes(10);
AiTaskType taskType = AiTaskType.fromCode(type.getCode());
if (taskType == AiTaskType.REQUIREMENT_GENERATE || taskType == AiTaskType.PROJECT_REQUIREMENT_GENERATE) {
    task.setTimeoutMinutes(30);
} else {
    task.setTimeoutMinutes(10);
}
```

**验证**: 编译通过

---

### Step 6: 清理旧 Prompt（ai 模块）

**文件**: `SystemPromptTemplates.java` — 删除 `REQUIREMENT_GENERATE` 常量（已被3个新Prompt替代）
**文件**: `UserPromptTemplates.java` — 删除 `REQUIREMENT_GENERATE_USER` 常量
**文件**: `PromptBuilder.java` — 删除 `buildRequirementGenerate()` 方法

确认没有其他代码引用这些已删除的常量/方法。

**验证**: 编译通过 + 全文搜索确认无残留引用

---

### Step 7: 编译验证 + install common 模块

- `mvn install -pl ele-ai-tender-common -am -Dmaven.test.skip=true`
- `mvn compile -pl ele-ai-tender-ai -am -Dmaven.test.skip=true`
- `mvn compile -pl ele-ai-tender-core -am -Dmaven.test.skip=true`

---

## 文件变更清单

| 文件 | 操作 | 优先级 |
|------|------|--------|
| `OutlineResult.java` | 新增 | Step 1 |
| `RequirementOutline.java` | 新增 | Step 1 |
| `SystemPromptTemplates.java` | 新增3个 + 删除1个 | Step 2 + Step 6 |
| `UserPromptTemplates.java` | 新增3个 + 删除1个 | Step 2 + Step 6 |
| `PromptBuilder.java` | 新增4个方法 + 删除1个 | Step 3 + Step 6 |
| `RequirementGenerator.java` | 重写 | Step 4 |
| `AiTaskServiceImpl.java` | 小改 | Step 5 |

## 并行执行分组

Step 1（DTO）必须先完成，其他依赖它。
Step 2 + Step 3（Prompt模板和Builder）可以和 Step 1 并行编写，但编译依赖 Step 1。
Step 4（核心重写）依赖 Step 1/2/3 全部完成。
Step 5（超时调整）独立，可与 Step 4 并行。
Step 6（清理旧代码）在 Step 4 完成后执行。
Step 7（编译验证）最后执行。

**Subagent 分工**:
- Agent A: Step 1 + Step 2 + Step 3（DTO + Prompt + Builder）— 串行，有依赖
- Agent B: Step 5（超时调整）— 独立
- 主线程: Step 4（核心重写）— 最关键，自己做
- Step 6 + Step 7: 主线程完成 Step 4 后执行
