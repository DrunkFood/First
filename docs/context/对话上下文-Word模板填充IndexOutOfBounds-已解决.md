# 对话上下文：Word模板填充 IndexOutOfBounds 异常

> 生成时间：2026-04-23 17:35 | 状态：**进行中**

---

## 📋 问题背景

**项目**：招标文件AI编制工具（ele-ai-tender）
**当前状态**：文档集成功能（`DocumentIntegrationServiceImpl.integrate`）调用文件服务生成 Word 文档时崩溃
**涉及组件**：file 模块（8081端口）的 `WordTemplateEngine` + poi-tl 1.12.0

### 调用链路
```
DocumentIntegrationServiceImpl.integrate(projectId)
  → DocumentDataAssembler.assemble(projectId)   // 组装扁平化Map数据
  → InternalFileServiceClient.generateDocument()  // HTTP调用file服务
    → WordDocumentServiceImpl.generateDocument()
      → WordTemplateEngine.render(templateStream, data)  // 💥 此处崩溃
        → XWPFTemplate.compile(templateStream, config)    // 编译阶段就挂了
```

---

## 🔴 核心问题

### 错误表现
```
2026-04-23 17:35:09.230 ERROR --- [http-nio-8081-exec-9] c.j.e.file.engine.WordTemplateEngine - Word模板填充失败
java.lang.IndexOutOfBoundsException: null
    at org.apache.xmlbeans.impl.store.Xobj.removeElement(Xobj.java:2099)
    at org.apache.xmlbeans.impl.store.Xobj.remove_element(Xobj.java:2130)
    at org.openxmlformats.schemas.wordprocessingml.x2006.main.impl.CTPImpl.removeR(CTPImpl.java:757)
    at com.deepoove.poi.xwpf.XWPFParagraphWrapper.removeRun(XWPFParagraphWrapper.java:382)
    at com.deepoove.poi.xwpf.ParagraphContext.removeRun(ParagraphContext.java:90)
    at com.deepoove.poi.resolver.RunningRunBody.refactorRun(RunningRunBody.java:101)
    at com.deepoove.poi.resolver.TemplateResolver.resolveParagraph(TemplateResolver.java:171)
    at com.deepoove.poi.resolver.TemplateResolver.resolveBodyElements(TemplateResolver.java:102)
    at com.deepoove.poi.resolver.TemplateResolver.resolveDocument(TemplateResolver.java:78)
    at com.deepoove.poi.XWPFTemplate.compile(XWPFTemplate.java:167)
    at com.jy.eleaitender.file.engine.WordTemplateEngine.render(WordTemplateEngine.java:32)
```

### 根源分析（Phase 1 调查中，尚未完全确认）

错误发生在 poi-tl 的 **compile 阶段**（不是 render 阶段），具体在 `RunningRunBody.refactorRun`：
- poi-tl 编译模板时，发现 `{{占位符}}` 被 Word 拆分到多个 XML Run 中
- `refactorRun` 尝试合并这些分散的 Run → 先合并文本到第一个 Run，然后删除多余 Run
- 删除多余 Run 时调用 `removeR`，底层 XMLBeans 的 `Xobj.removeElement` 抛出 IndexOutOfBoundsException

### 两种可能根因（待验证）

**假设1：模板标签被拆分到多个 Run（最可能）**
- Word/WPS 编辑模板时，会将 `{{projectName}}` 拆分为 `{{`、`project`、`Name`、`}}` 等多个 Run
- 原因：拼写检查标记、格式变化、语言标记、修订追踪等
- poi-tl 的 `refactorRun` 合并 Run 后删除时，XML 索引与 Java 对象列表不同步

**假设2：poi-tl 1.12.0 版本 Bug**
- `refactorRun` 方法对某些 XML 结构（如段落中混入书签、批注等非 Run 元素）处理不当
- 删除 Run 时索引计算错误

---

## 🎯 实现目标

- 修复 Word 模板填充时的 `IndexOutOfBoundsException`
- 确保各种来源（MS Word、WPS）创建的模板都能正常填充
- 保持 `WordTemplateEngine` 的简洁性，不过度设计

---

## 🔧 技术约束

| 约束 | 说明 |
|------|------|
| poi-tl 版本 | 1.12.0，父POM统一管理 |
| 必须用原生引擎 | 已有踩坑记录：不能用 SpringEL，原生引擎通过 `Map.get()` 访问 |
| Configure 配置 | 当前 `Configure.builder().build()` 无自定义插件 |
| 模板来源 | 用户上传的 .docx 文件，可能用 Word 或 WPS 编辑 |
| 前端 | 双前端架构，AI编制前端（5173）触发文档集成 |

### 关键文件清单

| 文件 | 路径 | 作用 |
|------|------|------|
| WordTemplateEngine | `ele-ai-tender-file/.../engine/WordTemplateEngine.java` | **出错位置**，poi-tl 渲染入口 |
| WordDocumentServiceImpl | `ele-ai-tender-file/.../service/impl/WordDocumentServiceImpl.java` | 调用模板引擎 |
| DocumentIntegrationServiceImpl | `ele-ai-tender-core/.../service/impl/DocumentIntegrationServiceImpl.java` | 文档集成入口 |
| DocumentDataAssembler | `ele-ai-tender-core/.../engine/DocumentDataAssembler.java` | 组装扁平化Map数据 |
| InternalFileServiceClient | `ele-ai-tender-common/.../client/InternalFileServiceClient.java` | core→file HTTP调用 |
| 父POM | `ele-ai-tender-system/pom.xml` | poi-tl版本=1.12.0 |

---

## 🚫 已尝试的方案

| 方案 | 结果 | 原因 |
|------|------|------|
| （尚未尝试任何修复方案） | — | 调查阶段被中断 |

---

## ✅ 当前方案/最终方案

**尚在调查阶段，未确定修复方案。**

可能的修复方向：
1. **升级 poi-tl 版本** — 查看最新版是否已修复此 Bug
2. **模板预处理** — 在 `render` 之前合并拆分的 Run
3. **Configure 配置优化** — 添加自定义的 Run refactor 逻辑或错误处理
4. **修复模板文件** — 提示用户用 MS Word 重新创建模板（不推荐，用户体验差）

---

## 📝 关键代码变更

（尚未修改任何代码）

### 当前 WordTemplateEngine.render 代码
```java
public byte[] render(InputStream templateStream, Map<String, Object> data) {
    Configure config = Configure.builder().build();
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        XWPFTemplate template = XWPFTemplate.compile(templateStream, config).render(data);
        template.writeAndClose(out);
        return out.toByteArray();
    } catch (Exception e) {
        log.error("Word模板填充失败", e);
        throw new RuntimeException("Word模板填充失败: " + e.getMessage(), e);
    }
}
```

---

## 🎯 当前进度

| 阶段 | 状态 | 说明 |
|------|------|------|
| Phase 1: 根因调查 | 🔄 进行中 | 已读取所有相关代码，已确认 poi-tl 版本和出错链路 |
| Phase 1: 模板文件检查 | ⏳ 未开始 | 需要检查触发错误的 .docx 模板的 XML 结构 |
| Phase 1: 在线搜索已知问题 | ⏳ 已发起但被中断 | 需要搜索 poi-tl 官方 issue 和社区讨论 |
| Phase 2: 模式分析 | ⏳ 未开始 | |
| Phase 3: 假设验证 | ⏳ 未开始 | |
| Phase 4: 实施修复 | ⏳ 未开始 | |

---

## 💡 使用方法

### 恢复上下文后应做什么
1. 继续搜索 poi-tl 的 `IndexOutOfBoundsException removeRun` 已知问题
2. 检查仓库中的 .docx 模板文件（`test_template.docx` 或 `.playwright-mcp/` 下的文件）
3. 用 Apache POI 解压模板，检查 `{{占位符}}` 是否被拆分到多个 Run
4. 根据验证结果确定修复方案

### 验证修复的方法
```bash
# 启动 file 服务
cd ele-ai-tender-system/ele-ai-tender-file && mvn spring-boot:run

# 通过 API 测试文档生成
curl -X POST http://localhost:8081/api/file/generate-doc \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"templateFileId": <id>, "data": {...}, "fileName": "test.docx"}'
```

---

## 🐛 已知问题和待解决

1. **核心问题**：poi-tl compile 阶段 `removeRun` 抛 `IndexOutOfBoundsException`
2. **待验证**：是否为模板文件本身的问题（Run 拆分）还是 poi-tl 版本 Bug
3. **待确认**：poi-tl 最新版本是否已修复此问题
4. **模板文件位置**：需确认触发错误的模板文件 ID 及其物理路径

---

## 🚀 下一步计划

1. **继续 Phase 1**：搜索 poi-tl 官方 GitHub Issues 中的相关 Bug 报告
2. **检查模板 XML**：用 POI 或解压 .docx 查看 `word/document.xml` 中占位符的 Run 结构
3. **测试升级 poi-tl**：查看 1.12.1+ 的 changelog，确认是否有相关修复
4. **实施修复**：根据验证结果选择最简方案（升级版本 / 预处理模板 / Configure 调整）

---

## 📝 备注

### 经验教训
- poi-tl 的 `refactorRun` 是 compile 阶段的核心逻辑，负责合并被 Word 拆分的模板标签
- Word/WPS 倾向于将文本拆成多个 Run，这是 .docx 格式的"特性"而非 Bug
- 项目已有 poi-tl 踩坑记录（memory 中 `poi-tl-engine-pitfall.md`）：必须用原生引擎

### 参考资料
- poi-tl 官方文档：https://github.com/Sayi/poi-tl
- poi-tl 版本：1.12.0
- 项目 memory：poi-tl必须用原生引擎禁止SpringEL
