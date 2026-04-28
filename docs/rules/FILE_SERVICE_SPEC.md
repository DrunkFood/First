# 文件服务模块规范

## 1. 定位

`ele-ai-tender-file` 提供统一文件能力，负责文件上传、下载、查询、删除，以及文档生成引擎（Markdown→Word / Word模板→Word）。

## 2. 当前接口

- `POST /api/file/upload`

> `ele-ai-tender-interaction` starter 已通过 `FileClient` 封装此接口，业务系统无需自行对接。

- `GET /api/file/download/{fileId}`
- `GET /api/file/info/{fileId}`
- `DELETE /api/file/delete/{fileId}`

## 2.1 文档生成引擎

文件模块内置文档生成引擎，供 core 模块通过内部接口调用：

| 类 | 职责 |
|----|------|
| `WordTemplateEngine` | Word 模板(poi-tl)填充与渲染，支持 markdownKeys 绑定 DocumentRenderPolicy |
| `WordStructureParser` | Word 模板结构解析，提取占位符和结构定义 |
| `WordDocumentFixEngine` | Word 文档文本替换引擎，基于 Apache POI 实现检测问题修复 |
| `MarkdownToDocumentConverter` | Markdown AST → DocumentRenderData 转换器（flexmark 解析，支持标题/加粗/斜体/列表/引用/代码） |
| `TableGenerator` | POI 编程生成表格（含单元格合并+边框），绕开 poi-tl 循环标签 |

> **注意**: `MarkdownTemplateEngine` 和 `WordDocumentGenerator` 已移入 core 模块（`com.jy.eleaitender.core.engine`），用于需求导出等 core 内部文档生成场景。

### poi-tl 引擎约束

- **必须使用原生引擎**（`Configure.builder().build()`），**禁止 `useSpringEL()`**
  - SpringEL 通过反射访问 JavaBean 属性，对 `Map<String, Object>` 类型数据不友好
  - 缺失字段会抛 `SpelEvaluationException: Property or field 'xxx' cannot be found`
  - 原生引擎通过 `Map.get()` 访问数据，缺失字段返回空字符串，天然容错
- **占位符语法**: `{{变量名}}`，支持中英文变量名（正则: `\{\{([\w一-龥]+)}}`）
- **完整标签语法表**:

  | 标签类型 | 语法 | 数据类型 | 示例 |
  |----------|------|----------|------|
  | 文本 | `{{var}}` | String / TextRenderData | `{{title}}` |
  | 循环（区块对） | `{{?items}}...{{/items}}` | `List<?>` | `{{?sections}}...{{/sections}}` |
  | 图片 | `{{@var}}` | PictureRenderData | `{{@logo}}` |
  | 包含（子模板） | `{{+var}}` | Include | `{{+header}}` |
  | 条件（区块对） | `{{var}}...{{var}}` | Boolean | `{{showDetail}}...{{showDetail}}` |

  > **区块对规则**: 开始标签以 `?` 标识（循环）或变量名标识（条件），结束标签以 `/` 标识。循环必须用 `{{?}}`，条件用同名变量包裹。

- **AI 常见标签错误**（生成模板/代码时务必避免）:

  | 错误写法 | 正确写法 | 错因 |
  |----------|----------|------|
  | `{{#items}}...{{/items}}` | `{{?items}}...{{/items}}` | `#` 是 Jinja2/Thymeleaf 语法，poi-tl 不认 |
  | `{{/list}}` | `{{/items}}` | 结束标签必须与开始标签同名 |
  | `{{image}}` | `{{@image}}` | 图片缺少 `@` 前缀 |
  | `{% for item in items %}` | `{{?items}}` | Jinja2 语法，poi-tl 不认 |

- **模板数据**: DocumentDataAssembler 组装 `List<FillData>`（TEXT/TABLE/IMAGE/MARKDOWN 四种类型），按类型分派渲染
- **两阶段渲染**: 阶段一 poi-tl 渲染文本/图片/Markdown（TABLE 渲染为占位文本 `__TABLE_PLACEHOLDER__key`）；阶段二 POI 扫描占位段落用 TableGenerator 替换为真实表格
- **Markdown 渲染**: `FillData.markdown()` → `MarkdownToDocumentConverter`（flexmark AST）→ `DocumentRenderData` → poi-tl `DocumentRenderPolicy` 渲染为 Word 格式化段落
- **表格生成约束**: `insertNewTbl(XmlCursor)` 创建的表格**默认无边框**，必须通过 `CTTblBorders` 设置 6 种边框（top/bottom/left/right/insideH/insideV）
- **poi-tl 相邻标签合并**: refactorRun 会将 `{{?items}}{{col}}` 合并为一个标签导致 `Mismatched start/end tags`，此为引擎内部行为无法绕开，TABLE 类型已通过 POI 编程生成彻底绕开循环标签
- **结构解析时机**: Support 模块创建/更新模板时自动调用 File 服务解析 Word 结构，结果存入 `structureDefinition`
- **模板修订标记预处理**: `WordTemplateEngine.render()` 在传给 poi-tl 之前，必须先执行 `acceptAllRevisions()` 清除修订标记
  - Word/WPS 编辑模板时可能开启修订追踪，产生的 `<w:ins>` 会包裹 `<w:r>`，使其不再是 `<w:p>` 直接子元素
  - poi-tl compile 阶段的 `refactorRun` 合并 Run 时 `removeRun` 索引错乱，抛 `IndexOutOfBoundsException`
  - `acceptAllRevisions()` 通过 DOM 操作解包 `<w:ins>`/`<w:moveTo>`（保留子节点）、删除 `<w:del>`/`<w:moveFrom>` 及属性变更标记（`rPrChange`/`pPrChange`/`sectPrChange` 等），等效于 Word 的"接受所有修订"
- **poi-tl 版本**: 必须 ≥ 1.12.2（1.12.0 的 `removeRun` 有已知 Bug，1.12.2 修复了普通多 Run 拆分场景但不覆盖修订标记场景）

### WordDocumentFixEngine 约束

- **职责**: 接收 .docx 字节 + 替换列表(`FixReplacement`)，基于 Apache POI XWPFDocument 执行文本替换，返回修复后字节数组 + 成功/失败计数
- **多 Run 替换策略**: Word 同一段落内同一词组可能被拆分到多个 Run，替换时先合并段落全文 → 执行替换 → 清空所有 Run → 将替换后文本写入第一个 Run。此策略会丢失跨 Run 的局部格式（加粗/颜色等），但对检测修复场景（替换短词组）可接受
- **遍历范围**: 遍历段落、表格单元格、页眉页脚，确保所有文本区域均被覆盖
- **文件命名**: 修复后文件名格式 `fixed_时间戳_原始文件名`，若原文件名已有 `fixed_数字_` 前缀则先去除再重新拼接
- **接口**: `POST /api/file/fix-doc`，接收 `{fileId, replacements}`，返回 `WordFixResultVO { fileId, fixedCount, failedCount }`

## 3. 当前表

### `file_info` 表关键字段

| 字段 | 说明 |
|------|------|
| `id` | 文件主键（bigint，自增，对外暴露为 `fileId`） |
| `file_name` | 原始文件名 |
| `file_path` | 服务端存储路径 |
| `file_size` | 文件大小（bytes） |
| `file_type` | 文件扩展名（如 `.pdf`） |
| `file_sha256` | 文件 SHA-256 摘要（十六进制字符串） |
| `biz_type` | 业务类型（上传时由调用方传入，用于分类存储） |

继承 `BaseEntity` 基础字段（`create_time`、`modify_time`、`create_id`、`modify_id`、`ver`、`is_delete` 等）。

继承 `BaseEntity` 基础字段（`create_time`、`modify_time`、`is_delete` 等）。

## 4. 上传约束

- 通用上传必须提供 `file` 和 `bizType`
- 默认最大文件大小 `500MB`

### 当前白名单

- `.doc`、`.docx`、`.pdf`
- `.txt`、`.md`
- `.jar`、`.war`、`.zip`、`.tar.gz`

> 若接入系统配置了自定义后缀，需在 `file.storage.allowed-types` 中手动添加对应后缀。

## 5. 文件字段约束

- 文件主键统一命名为 `fileId`
- 文件摘要统一使用 `fileSha256` / `file_sha256`（SHA-256 十六进制字符串，UTF-8 编码）
- 文件元信息统一保存在 `file_info`

## 6. 内部服务调用

core/support 模块通过 `InternalFileServiceClient` 调用 File 服务，关键约束：

- **错误响应处理**: 客户端必须检查响应 `code` 字段，`code != 200` 时提取 `message` 抛出具体错误，避免误导性的"数据为空"
- **服务间认证**: 使用 `TOKEN_TYPE_SERVICE` 类型的 JWT，密钥复用 `APP_JWT_SECRET`，服务间调用视为管理员权限
- **新增端点**: `/api/file/structure/{fileId}`（GET）、`/api/file/generate-doc`（POST）、`/api/file/fix-doc`（POST）需在 `InternalFileServiceClient` 中同步添加解析逻辑

## 7. 排障原则

- **文件上传失败** → 检查文件扩展名是否在白名单内、文件大小是否超过 500MB、`file.storage.base-path` 目录是否有写权限
- **下载 404** → 确认 `file_info.file_path` 对应文件在磁盘上实际存在，检查 `file.storage.base-path` 配置是否正确
- **fileSha256 校验失败** → 确认调用方计算方式与服务端一致（SHA-256 十六进制字符串，字节序列使用 UTF-8）

日志与链路追踪规范见 [PROJECT_SPEC_FINAL.md](PROJECT_SPEC_FINAL.md)（日志中不得记录文件二进制内容，需透传 `X-Trace-Id`）。

## 相关规范

- [CODE_CONVENTIONS.md](CODE_CONVENTIONS.md) — 通用编码规范（字段命名、日志脱敏）
- [PROJECT_SPEC_FINAL.md](PROJECT_SPEC_FINAL.md) — 全局日志约束与链路追踪规范
