# 对话上下文：需求导出 Word 文档接口

> 生成时间：2026-04-23 | 状态：待确认

---

## 📋 问题背景

**项目**：招标文件AI编制工具（ele-ai-tender）
**模块**：ele-ai-tender-core（端口 8082）
**涉及组件**：RequirementController / IRequirementService / MarkdownTemplateEngine / WordDocumentGenerator

前端 `RequirementGenerate.vue` 调用 `GET /core-api/v1/requirements/{id}/export` 期望下载 .docx 文件，但后端无此端点，返回 404。

## 🔴 核心问题

后端缺少需求导出接口。前端已有完整的下载逻辑（Blob 处理、文件名解析），只需要后端提供正确的 REST 端点。

## 🎯 实现目标

- 提供 `GET /api/v1/requirements/{id}/export` 端点
- 返回 .docx 文件二进制流，带正确的 Content-Type 和 Content-Disposition
- 需求不存在时返回 404，内容为空时返回仅含标题的空文档
- 不新增类，复用已移入 core 模块的 MarkdownTemplateEngine + WordDocumentGenerator

## 🔧 技术约束

- core 模块直接生成，不经过 file 模块
- 不修改 common 模块（避免重依赖污染）
- 前端无需改动（API 路径和返回格式已正确）
- 编译需 JDK 21：`JAVA_HOME="D:/Users/admin/.jdks/openjdk-21.0.5"`

## ✅ 最终方案

**数据流**：

```
前端 GET → Controller.exportDocument → Service.exportDocument
  → MarkdownTemplateEngine.markdownToHtml(content)
  → WordDocumentGenerator.generate(data, html)
  → ResponseEntity<byte[]> → 前端下载 .docx
```

**设计文档**：`docs/projects/2026-04-23-需求导出Word文档接口设计.md`

## 📝 关键代码变更

### 1. IRequirementService（新增接口方法）

```java
byte[] exportDocument(Long id);
```

### 2. RequirementServiceImpl（注入引擎 + 实现）

- 新增注入：`MarkdownTemplateEngine`、`WordDocumentGenerator`
- 实现逻辑：`getById(id)` → `markdownToHtml(content)` → `generate(data, html)` → 返回 byte[]

```java
@Override
public byte[] exportDocument(Long id) {
    TbRequirement req = getById(id);
    String content = req.getContent();
    if (content == null) content = "";
    String html = markdownTemplateEngine.markdownToHtml(content);
    Map<String, Object> data = Map.of("projectName", req.getRequirementName());
    return wordDocumentGenerator.generate(data, html);
}
```

### 3. RequirementController（新增端点）

```java
@GetMapping("/{id}/export")
@RequireLogin
@Operation(summary = "导出需求文档")
public ResponseEntity<Resource> exportDocument(@PathVariable Long id) {
    byte[] bytes = requirementService.exportDocument(id);
    TbRequirement req = requirementService.getById(id);
    String fileName = URLEncoder.encode(req.getRequirementName(), StandardCharsets.UTF_8) + ".docx";
    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
            .body(new ByteArrayResource(bytes));
}
```

### 涉及文件清单

| 文件 | 变更类型 |
|------|----------|
| `core/service/IRequirementService.java` | 新增方法 |
| `core/service/impl/RequirementServiceImpl.java` | 注入 + 实现 |
| `core/controller/RequirementController.java` | 新增端点 + import |
| `docs/projects/2026-04-23-需求导出Word文档接口设计.md` | 更新数据流和方案 |

## 🎯 当前进度

| 步骤 | 状态 |
|------|------|
| 设计文档更新（复用已有引擎） | ✅ 已完成 |
| IRequirementService 接口新增 | ✅ 已完成 |
| RequirementServiceImpl 实现 | ✅ 已完成 |
| RequirementController 端点 | ✅ 已完成 |
| 编译验证（JDK21 mvn compile） | ✅ 已通过 |
| 启动服务 + 前端联调测试 | ⬜ 待确认 |

## 💡 使用方法

1. 启动 core 服务：`cd ele-ai-tender-system/ele-ai-tender-core && JAVA_HOME="D:/Users/admin/.jdks/openjdk-21.0.5" mvn spring-boot:run`
2. 前端调用：`GET /core-api/v1/requirements/{id}/export`，Bearer Token 认证
3. 返回 .docx 文件流，前端自动触发下载

## 🐛 已知问题和待解决

1. **exportDocument 中 getById 调用了两次**：一次在 service 层生成文档，一次在 controller 层获取文件名。可考虑优化为返回包含文件名和字节数组的 VO，减少一次 DB 查询。
2. **中文文件名兼容性**：仅做了 URLEncoder 编码，未添加 RFC 5987 `filename*` 头，部分浏览器可能显示乱码。
3. **前端联调未验证**：需要实际启动 core 服务 + 前端，确认下载流程端到端可用。

## 🚀 下一步计划

1. 启动 core 服务 + 前端，联调测试导出功能
2. 验证 Markdown 内容（标题/表格/列表/引用等）在 Word 中的渲染效果
3. 修复中文文件名兼容性（添加 `filename*` 头）
4. 优化双重 getById 查询

## 📝 备注

- `MarkdownTemplateEngine` 和 `WordDocumentGenerator` 原本在 file 模块，现已移入 core 模块（`com.jy.eleaitender.core.engine` 包）
- file 模块保留了不同的 `WordTemplateEngine`（基于 poi-tl 模板填充），与本次实现无关
- `WordDocumentGenerator.generate()` 接受 `Map<String, Object> documentData`，其中 `projectName` key 用于生成文档标题
