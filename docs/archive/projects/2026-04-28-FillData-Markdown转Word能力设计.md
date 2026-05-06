# FillData Markdown→Word 转换能力设计

> 生成时间：2026-04-28 | 状态：已确认

## 问题

`requirementContent`（招标需求内容）字段由 AI 生成，包含 Markdown 格式（标题、加粗、列表、引用）。
当前作为 TEXT 类型传给 poi-tl，只能当纯文本替换，Word 中显示的是原始 Markdown 语法而非格式化内容。

## 方案：Markdown → HTML → Word

复用 flexmark（MD→HTML）+ poi-tl HtmlRenderPolicy（HTML→Word），零自定义渲染代码。

```
Markdown 原文 → flexmark 转 HTML → poi-tl HtmlRenderPolicy 渲染为 Word 段落
```

Markdown 走阶段一（和 TEXT/IMAGE 一起由 poi-tl 渲染），不需要像 TABLE 走阶段二占位符。

## 支持的 Markdown 语法

| 语法 | HTML | Word 渲染 |
|------|------|-----------|
| `# 标题` | `<h1>` | Heading 1 样式 |
| `## 标题` | `<h2>` | Heading 2 样式 |
| `**加粗**` | `<b>` | Bold Run |
| `*斜体*` | `<i>` | Italic Run |
| `- 列表项` | `<ul><li>` | Word 无序列表 |
| `1. 列表项` | `<ol><li>` | Word 有序列表 |
| `> 引用` | `<blockquote>` | Word 引用段落 |

## 数据模型变更

### FillType 新增 MARKDOWN

```java
public enum FillType {
    TEXT,       // 纯文本，poi-tl {{key}} 替换
    TABLE,      // 表格，POI 编程生成
    IMAGE,      // 图片，poi-tl {{@key}} 替换
    MARKDOWN    // Markdown文本，flexmark→HTML→HtmlRenderPolicy 渲染
}
```

### FillData 新增工厂方法

```java
public static FillData markdown(String key, String value) {
    FillData fd = new FillData();
    fd.setType(FillType.MARKDOWN);
    fd.setKey(key);
    fd.setValue(value);
    return fd;
}
```

### 反序列化（ai/file 两处）

MARKDOWN 的 value 是 String，和 TEXT 相同：

```java
case MARKDOWN -> fd.getValue() instanceof String s ? s : String.valueOf(fd.getValue());
```

## 渲染管线

```
core 组装 FillData
  │
  ├─ TEXT     ──────────────────→ poiData["key"] = "纯文本"
  ├─ IMAGE    ──────────────────→ poiData["key"] = PictureRenderData
  ├─ MARKDOWN → flexmark转HTML → poiData["key"] = "<h1>...</h1><p>...</p>"
  │                              markdownKeys.add("key")
  └─ TABLE    ──────────────────→ poiData["key"] = "__TABLE_PLACEHOLDER__key"
                                 tableDataMap["key"] = TableData
  │
  ▼
WordTemplateEngine.render(template, poiData, markdownKeys)
  ├─ Configure 为 markdownKeys 绑定 HtmlRenderPolicy
  └─ poi-tl 一次性渲染：TEXT替换 + IMAGE图片 + MARKDOWN→Word段落 + TABLE占位文本
  │
  ▼
阶段二（仅 TABLE）：替换占位符为真实表格
```

## 文件变更清单

| 模块 | 文件 | 变更 |
|------|------|------|
| common | `FillType.java` | 新增 `MARKDOWN` 枚举值 |
| common | `FillData.java` | 新增 `markdown()` 工厂方法 |
| core | `DocumentDataAssembler.java` | `requirementContent` 改用 `FillData.markdown()` |
| file | `pom.xml` | 新增 flexmark-all 0.64.0 + jsoup 1.17.2 依赖 |
| file | `WordTemplateEngine.java` | `render()` 增加 `Set<String> markdownKeys` 参数，动态绑定 HtmlRenderPolicy |
| file | `WordDocumentServiceImpl.java` | 处理 MARKDOWN 类型：MD→HTML 转换 + 收集 markdownKeys + 传参 |
| ai | `DocumentIntegration.java` | `deserializeFillDataList()` switch 加 MARKDOWN 分支 |

### 不变的文件

- `InternalFileServiceClient` — FillData 序列化已包含 type 字段
- `FileController` — 已有 `List<Map<String, Object>>` 接收
- `TableGenerator` — 阶段二仅处理 TABLE
- `DocumentIntegrationServiceImpl` — 透传 FillData 列表

## 关键实现细节

### WordTemplateEngine.render 改造

```java
public byte[] render(InputStream templateStream, Map<String, Object> data, Set<String> markdownKeys) {
    Configure.ConfigureBuilder builder = Configure.builder();
    if (markdownKeys != null) {
        markdownKeys.forEach(key -> builder.bind(key, new HtmlRenderPolicy()));
    }
    Configure config = builder.build();
    // 后续不变：acceptAllRevisions → XWPFTemplate.compile → render → writeAndClose
}
```

### WordDocumentServiceImpl 新增 markdownToHtml 方法

```java
private String markdownToHtml(String markdown) {
    if (markdown == null || markdown.isBlank()) return "";
    MutableDataSet options = new MutableDataSet();
    Parser parser = Parser.builder(options).build();
    HtmlRenderer renderer = HtmlRenderer.builder(options).build();
    Node document = parser.parse(markdown);
    return renderer.render(document);
}
```

### 数据分离逻辑变更

```java
Set<String> markdownKeys = new HashSet<>();

for (FillData fd : fillDataList) {
    switch (fd.getType()) {
        case TEXT     -> poiData.put(fd.getKey(), fd.getValue());
        case IMAGE    -> poiData.put(fd.getKey(), toPictureRenderData((ImageData) fd.getValue()));
        case MARKDOWN -> {
            String html = markdownToHtml((String) fd.getValue());
            poiData.put(fd.getKey(), html);
            markdownKeys.add(fd.getKey());
        }
        case TABLE -> {
            poiData.put(fd.getKey(), TableGenerator.TABLE_PLACEHOLDER_PREFIX + fd.getKey());
            tableDataMap.put(fd.getKey(), (TableData) fd.getValue());
        }
    }
}

byte[] rendered = templateEngine.render(templateStream, poiData, markdownKeys);
```

### DocumentDataAssembler 变更

```java
// 改前
fillDataList.add(FillData.text("requirementContent", nullSafe(project.getRequirementContent())));

// 改后
fillDataList.add(FillData.markdown("requirementContent", nullSafe(project.getRequirementContent())));
```
