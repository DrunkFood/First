# FillData Markdown→Word 转换能力实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 FillData 体系新增 MARKDOWN 类型，通过 flexmark + poi-tl HtmlRenderPolicy 将 Markdown 内容渲染为 Word 格式化段落。

**Architecture:** 新增 FillType.MARKDOWN 枚举值，file 模块在阶段一渲染时对 MARKDOWN 类型先转 HTML 再通过 HtmlRenderPolicy 渲染为 Word。不需要阶段二占位符机制。

**Tech Stack:** flexmark-java 0.64.0 / jsoup 1.17.2 / poi-tl 1.12.2 HtmlRenderPolicy / JUnit 5

---

## 文件结构

| 操作 | 文件 | 职责 |
|------|------|------|
| 修改 | `ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/dto/FillType.java` | 新增 MARKDOWN 枚举值 |
| 修改 | `ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/dto/FillData.java` | 新增 markdown() 工厂方法 |
| 修改 | `ele-ai-tender-system/pom.xml` | dependencyManagement 新增 flexmark + jsoup |
| 修改 | `ele-ai-tender-system/ele-ai-tender-file/pom.xml` | 新增 flexmark + jsoup 依赖 |
| 修改 | `ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/engine/DocumentDataAssembler.java:54` | requirementContent 改用 FillData.markdown() |
| 修改 | `ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/engine/WordTemplateEngine.java:38-53` | render() 增加 markdownKeys 参数，动态绑定 HtmlRenderPolicy |
| 修改 | `ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/service/impl/WordDocumentServiceImpl.java:71-108` | 处理 MARKDOWN 类型：MD→HTML + 收集 markdownKeys |
| 修改 | `ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/processor/generator/DocumentIntegration.java:88-91` | deserializeFillDataList() switch 加 MARKDOWN 分支 |
| 新增 | `ele-ai-tender-system/ele-ai-tender-file/src/test/java/com/jy/eleaitender/file/engine/MarkdownToWordTest.java` | markdownToHtml + HtmlRenderPolicy 单元测试 |

---

### Task 1: FillType 新增 MARKDOWN 枚举值

**Files:**
- Modify: `ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/dto/FillType.java`

- [ ] **Step 1: 在 FillType 枚举中新增 MARKDOWN**

```java
public enum FillType {

    /** 文本，poi-tl {{key}} 替换 */
    TEXT,

    /** 表格，POI 编程生成，模板中 {{TABLE:key}} 标记位置 */
    TABLE,

    /** 图片，poi-tl {{@key}} 替换 */
    IMAGE,

    /** Markdown文本，flexmark→HTML→poi-tl HtmlRenderPolicy 渲染 */
    MARKDOWN
}
```

---

### Task 2: FillData 新增 markdown() 工厂方法

**Files:**
- Modify: `ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/dto/FillData.java`

- [ ] **Step 1: 在 FillData 中新增 markdown 静态工厂方法**

在 `image()` 方法之后添加：

```java
public static FillData markdown(String key, String value) {
    FillData fd = new FillData();
    fd.setType(FillType.MARKDOWN);
    fd.setKey(key);
    fd.setValue(value);
    return fd;
}
```

---

### Task 3: 安装 common 模块 + 编译验证

**前置：** Task 1 和 Task 2 完成后，common 模块有变更，必须 install 才能被其他模块引用。

- [ ] **Step 1: install common 模块**

```bash
cd ele-ai-tender-system && JAVA_HOME='D:/Users/admin/.jdks/openjdk-21.0.5' mvn install -pl ele-ai-tender-common -am -Dmaven.test.skip=true
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 验证 file/core/ai 模块编译**

```bash
cd ele-ai-tender-system && JAVA_HOME='D:/Users/admin/.jdks/openjdk-21.0.5' mvn clean compile -Dmaven.test.skip=true
```

Expected: BUILD SUCCESS（此时 MARKDOWN 还没被使用，不应有编译错误）

---

### Task 4: 父 POM dependencyManagement 新增 flexmark + jsoup

**Files:**
- Modify: `ele-ai-tender-system/pom.xml`

- [ ] **Step 1: 在 dependencyManagement 中新增 flexmark-all 和 jsoup**

在 `</dependencies>` 闭合标签前（poi-ooxml-full 之后）添加：

```xml
<!-- Markdown处理 -->
<dependency>
    <groupId>com.vladsch.flexmark</groupId>
    <artifactId>flexmark-all</artifactId>
    <version>${flexmark.version}</version>
</dependency>
<!-- jsoup: poi-tl HtmlRenderPolicy 需要 -->
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>${jsoup.version}</version>
</dependency>
```

版本属性已存在：`flexmark.version=0.64.0`，`jsoup.version=1.17.2`

---

### Task 5: file 模块 pom.xml 新增依赖

**Files:**
- Modify: `ele-ai-tender-system/ele-ai-tender-file/pom.xml`

- [ ] **Step 1: 在 file/pom.xml 中新增 flexmark 和 jsoup 依赖**

在 `poi-ooxml-full` 依赖之后添加：

```xml
<!-- Markdown处理 -->
<dependency>
    <groupId>com.vladsch.flexmark</groupId>
    <artifactId>flexmark-all</artifactId>
</dependency>
<!-- jsoup: poi-tl HtmlRenderPolicy 需要 -->
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
</dependency>
```

注意：不写 version，由父 POM dependencyManagement 统一管理。

- [ ] **Step 2: 验证 file 模块依赖解析**

```bash
cd ele-ai-tender-system && JAVA_HOME='D:/Users/admin/.jdks/openjdk-21.0.5' mvn dependency:resolve -pl ele-ai-tender-file
```

Expected: flexmark-all 和 jsoup 出现在 resolved 列表中

---

### Task 6: WordTemplateEngine 改造 — render() 支持 markdownKeys

**Files:**
- Modify: `ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/engine/WordTemplateEngine.java`

- [ ] **Step 1: 修改 render() 方法签名，增加 markdownKeys 参数**

将现有 `render` 方法签名改为：

```java
public byte[] render(InputStream templateStream, Map<String, Object> data, Set<String> markdownKeys) {
```

文件头部新增 import：

```java
import com.deepoove.poi.plugin.HtmlRenderPolicy;
import java.util.Set;
```

- [ ] **Step 2: 修改 Configure 构建，动态绑定 HtmlRenderPolicy**

将原来的 `Configure config = Configure.builder().build();` 替换为：

```java
Configure.ConfigureBuilder builder = Configure.builder();
if (markdownKeys != null) {
    for (String key : markdownKeys) {
        builder.bind(key, new HtmlRenderPolicy());
    }
}
Configure config = builder.build();
```

- [ ] **Step 3: 保留 render(InputStream, Map) 兼容方法**

在修改后的 render 方法下方，增加一个两参数的重载方法，保证已有调用方不受影响：

```java
public byte[] render(InputStream templateStream, Map<String, Object> data) {
    return render(templateStream, data, null);
}
```

---

### Task 7: WordDocumentServiceImpl 改造 — 处理 MARKDOWN 类型

**Files:**
- Modify: `ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/service/impl/WordDocumentServiceImpl.java`

- [ ] **Step 1: 新增 import**

```java
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import java.util.HashSet;
import java.util.Set;
```

- [ ] **Step 2: 新增 markdownToHtml() 私有方法**

在 `toPictureRenderData()` 方法之后添加：

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

- [ ] **Step 3: 修改 generateWithFillData() 中的数据分离逻辑**

在 `Map<String, TableData> tableDataMap = new LinkedHashMap<>();` 之后添加：

```java
Set<String> markdownKeys = new HashSet<>();
```

在 switch 中，`case IMAGE` 之后添加 MARKDOWN 分支：

```java
case MARKDOWN -> {
    String html = markdownToHtml((String) fd.getValue());
    poiData.put(fd.getKey(), html);
    markdownKeys.add(fd.getKey());
}
```

- [ ] **Step 4: 修改 templateEngine.render() 调用，传入 markdownKeys**

将：

```java
byte[] rendered = templateEngine.render(templateStream, poiData);
```

改为：

```java
byte[] rendered = templateEngine.render(templateStream, poiData, markdownKeys);
```

- [ ] **Step 5: 修改 deserializeFillDataList()，MARKDOWN 分支和 TEXT 相同**

在 switch 的 `case TEXT` 之后添加：

```java
case MARKDOWN -> fd.getValue() instanceof String s ? s : String.valueOf(fd.getValue());
```

---

### Task 8: DocumentDataAssembler — requirementContent 改用 FillData.markdown()

**Files:**
- Modify: `ele-ai-tender-system/ele-ai-tender-core/src/main/java/com/jy/eleaitender/core/engine/DocumentDataAssembler.java:54`

- [ ] **Step 1: 将 requirementContent 从 FillData.text() 改为 FillData.markdown()**

```java
// 改前（第54行）
fillDataList.add(FillData.text("requirementContent", nullSafe(project.getRequirementContent())));

// 改后
fillDataList.add(FillData.markdown("requirementContent", nullSafe(project.getRequirementContent())));
```

---

### Task 9: ai 模块 DocumentIntegration — 反序列化加 MARKDOWN 分支

**Files:**
- Modify: `ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/processor/generator/DocumentIntegration.java:88-91`

- [ ] **Step 1: 在 deserializeFillDataList() 的 switch 中添加 MARKDOWN 分支**

在 `case TEXT` 之后添加：

```java
case MARKDOWN -> fd.getValue() instanceof String s ? s : String.valueOf(fd.getValue());
```

---

### Task 10: 全模块编译验证

- [ ] **Step 1: 全模块 clean compile**

```bash
cd ele-ai-tender-system && JAVA_HOME='D:/Users/admin/.jdks/openjdk-21.0.5' mvn clean compile -Dmaven.test.skip=true
```

Expected: BUILD SUCCESS

---

### Task 11: 编写单元测试

**Files:**
- Create: `ele-ai-tender-system/ele-ai-tender-file/src/test/java/com/jy/eleaitender/file/engine/MarkdownToWordTest.java`

- [ ] **Step 1: 编写 markdownToHtml 转换测试**

```java
package com.jy.eleaitender.file.engine;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownToWordTest {

    private String markdownToHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) return "";
        MutableDataSet options = new MutableDataSet();
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }

    @Nested
    @DisplayName("Markdown→HTML 转换测试")
    class MarkdownToHtmlTest {

        @Test
        @DisplayName("标题转换")
        void heading() {
            String html = markdownToHtml("# 招标需求");
            assertTrue(html.contains("<h1>招标需求</h1>"));
        }

        @Test
        @DisplayName("二级标题转换")
        void heading2() {
            String html = markdownToHtml("## 技术要求");
            assertTrue(html.contains("<h2>技术要求</h2>"));
        }

        @Test
        @DisplayName("加粗文本转换")
        void bold() {
            String html = markdownToHtml("**重要说明**");
            assertTrue(html.contains("<strong>重要说明</strong>"));
        }

        @Test
        @DisplayName("斜体文本转换")
        void italic() {
            String html = markdownToHtml("*备注信息*");
            assertTrue(html.contains("<em>备注信息</em>"));
        }

        @Test
        @DisplayName("无序列表转换")
        void unorderedList() {
            String html = markdownToHtml("- 项目一\n- 项目二");
            assertTrue(html.contains("<ul>"));
            assertTrue(html.contains("<li>项目一</li>"));
            assertTrue(html.contains("<li>项目二</li>"));
        }

        @Test
        @DisplayName("有序列表转换")
        void orderedList() {
            String html = markdownToHtml("1. 第一步\n2. 第二步");
            assertTrue(html.contains("<ol>"));
            assertTrue(html.contains("<li>第一步</li>"));
        }

        @Test
        @DisplayName("引用转换")
        void blockquote() {
            String html = markdownToHtml("> 这是引用内容");
            assertTrue(html.contains("<blockquote>"));
            assertTrue(html.contains("这是引用内容"));
        }

        @Test
        @DisplayName("混合内容转换")
        void mixedContent() {
            String md = "# 招标需求\n\n## 技术要求\n\n- **性能要求**：响应时间<3s\n- *安全性*：符合国标\n\n> 本项目为限额以下工程";
            String html = markdownToHtml(md);
            assertTrue(html.contains("<h1>招标需求</h1>"));
            assertTrue(html.contains("<h2>技术要求</h2>"));
            assertTrue(html.contains("<strong>性能要求</strong>"));
            assertTrue(html.contains("<em>安全性</em>"));
            assertTrue(html.contains("<blockquote>"));
        }

        @Test
        @DisplayName("null 返回空串")
        void nullReturnsEmpty() {
            assertEquals("", markdownToHtml(null));
        }

        @Test
        @DisplayName("空白返回空串")
        void blankReturnsEmpty() {
            assertEquals("", markdownToHtml("   "));
        }
    }
}
```

- [ ] **Step 2: 运行测试**

```bash
cd ele-ai-tender-system && JAVA_HOME='D:/Users/admin/.jdks/openjdk-21.0.5' mvn test -pl ele-ai-tender-file -Dtest=MarkdownToWordTest
```

Expected: 全部 PASS

---

### Task 12: 端到端验证

- [ ] **Step 1: 重启 file 和 ai 服务**

```bash
# 先 install common
cd ele-ai-tender-system && JAVA_HOME='D:/Users/admin/.jdks/openjdk-21.0.5' mvn install -pl ele-ai-tender-common -am -Dmaven.test.skip=true

# 重启各服务
cd ele-ai-tender-system/ele-ai-tender-file && JAVA_HOME='D:/Users/admin/.jdks/openjdk-21.0.5' mvn spring-boot:run
cd ele-ai-tender-system/ele-ai-tender-ai && JAVA_HOME='D:/Users/admin/.jdks/openjdk-21.0.5' mvn spring-boot:run
cd ele-ai-tender-system/ele-ai-tender-core && JAVA_HOME='D:/Users/admin/.jdks/openjdk-21.0.5' mvn spring-boot:run
```

- [ ] **Step 2: 调用文档集成接口，验证生成的 Word 文档中 Markdown 内容渲染为格式化段落**

验证要点：
1. `requirementContent` 中的 `# 标题` 渲染为 Word 标题样式
2. `**加粗**` 渲染为粗体
3. `- 列表` 渲染为 Word 列表
4. `> 引用` 渲染为 Word 引用段落
5. 纯 TEXT 字段（projectName 等）不受影响

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat(fill-data): 新增 MARKDOWN 类型支持，Markdown→HTML→Word 格式化渲染"
```
