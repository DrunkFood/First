# 需求导出 Word 文档接口设计

## 问题

前端 `RequirementGenerate.vue` 调用 `GET /core-api/v1/requirements/{id}/export`，期望下载 .docx 文件（Blob），但后端 `RequirementController` 中没有此端点，接口 404。

## 决策

- **导出方式**：Markdown→HTML→Word（复用 flexmark + POI + jsoup 技术栈）
- **导出内容**：仅需求正文 Markdown 内容，不含元信息
- **实现方案**：core 模块直接生成（方案 A），不经过 file 模块

## 数据流

```mermaid
flowchart LR
    A["前端 GET /requirements/{id}/export"] --> B["RequirementController"]
    B --> C["IRequirementService.exportDocument"]
    C --> D["MarkdownToWordConverter.convert"]
    D --> D1["flexmark: MD→HTML"]
    D1 --> D2["POI+jsoup: HTML→Word"]
    D2 --> E["ResponseEntity byte数组"]
    E --> F["前端下载 .docx"]
```

## 代码变更

### 1. core 模块 pom.xml — 新增依赖

```xml
<!-- Markdown处理 -->
<dependency>
    <groupId>com.vladsch.flexmark</groupId>
    <artifactId>flexmark-all</artifactId>
    <version>${flexmark.version}</version>
</dependency>

<!-- Word生成 -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
</dependency>

<!-- HTML解析 -->
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>${jsoup.version}</version>
</dependency>
```

### 2. 新增 MarkdownToWordConverter

位置：`com.jy.eleaitender.core.engine.MarkdownToWordConverter`

职责：Markdown → HTML → Word 字节数组

```java
@Component
public class MarkdownToWordConverter {
    public byte[] convert(String requirementName, String markdownContent) { ... }
}
```

内部逻辑：
1. flexmark `Parser` + `HtmlRenderer` 将 Markdown 转 HTML
2. jsoup 解析 HTML
3. POI `XWPFDocument` 生成 Word，渲染 h1-h6 / p / table / ul-ol / blockquote / hr / strong-em-u-code-a
4. 文档标题用 requirementName，居中 22 号宋体加粗
5. 返回 byte[]

### 3. RequirementController — 新增端点

```java
@GetMapping("/{id}/export")
@RequireLogin
@Operation(summary = "导出需求文档")
public ResponseEntity<Resource> exportDocument(@PathVariable Long id) {
    // 调用 requirementService.exportDocument(id)
    // 返回 ResponseEntity.ok()
    //   .header(Content-Disposition, "attachment; filename=xxx.docx")
    //   .header(Content-Type, "application/vnd.openxmlformats-officedocument...")
    //   .body(new ByteArrayResource(bytes))
}
```

### 4. IRequirementService / RequirementServiceImpl

```java
// 接口
byte[] exportDocument(Long id);

// 实现
public byte[] exportDocument(Long id) {
    TbRequirement req = getById(id); // 不存在抛异常
    String content = req.getContent();
    if (content == null) content = "";
    return markdownToWordConverter.convert(req.getRequirementName(), content);
}
```

## 接口定义

```
GET /api/v1/requirements/{id}/export
Authorization: Bearer <token>

→ 200 OK
  Content-Type: application/vnd.openxmlformats-officedocument.wordprocessingml.document
  Content-Disposition: attachment; filename="<requirementName>.docx"
  Body: Word 文件二进制流

→ 需求不存在
  { "code": 404, "msg": "需求不存在" }

→ 内容为空
  返回仅含标题的空 Word 文档（200 OK）
```

## 不涉及的变更

- common 模块不变（避免引入重依赖污染其他模块）
- file 模块不变（其引擎有项目文档特定逻辑，保持独立）
- 前端无需改动（API 路径和返回格式已正确）
