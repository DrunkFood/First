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
| `WordTemplateEngine` | Word 模板(poi-tl)填充与渲染 |
| `WordStructureParser` | Word 模板结构解析，提取占位符和结构定义 |

> **注意**: `MarkdownTemplateEngine` 和 `WordDocumentGenerator` 已移入 core 模块（`com.jy.eleaitender.core.engine`），用于需求导出等 core 内部文档生成场景。

### poi-tl 引擎约束

- **必须使用原生引擎**（`Configure.builder().build()`），**禁止 `useSpringEL()`**
  - SpringEL 通过反射访问 JavaBean 属性，对 `Map<String, Object>` 类型数据不友好
  - 缺失字段会抛 `SpelEvaluationException: Property or field 'xxx' cannot be found`
  - 原生引擎通过 `Map.get()` 访问数据，缺失字段返回空字符串，天然容错
- **占位符语法**: `{{变量名}}`，支持中英文变量名（正则: `\{\{([\w一-龥]+)}}`）
- **模板数据**: DocumentDataAssembler 组装扁平 Map，评审项转为 `List<Map<String, String>>`
- **结构解析时机**: Support 模块创建/更新模板时自动调用 File 服务解析 Word 结构，结果存入 `structureDefinition`

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
- **新增端点**: `/api/file/structure/{fileId}`（GET）、`/api/file/generate-doc`（POST）需在 `InternalFileServiceClient` 中同步添加解析逻辑

## 7. 排障原则

- **文件上传失败** → 检查文件扩展名是否在白名单内、文件大小是否超过 500MB、`file.storage.base-path` 目录是否有写权限
- **下载 404** → 确认 `file_info.file_path` 对应文件在磁盘上实际存在，检查 `file.storage.base-path` 配置是否正确
- **fileSha256 校验失败** → 确认调用方计算方式与服务端一致（SHA-256 十六进制字符串，字节序列使用 UTF-8）

日志与链路追踪规范见 [PROJECT_SPEC_FINAL.md](PROJECT_SPEC_FINAL.md)（日志中不得记录文件二进制内容，需透传 `X-Trace-Id`）。

## 相关规范

- [CODE_CONVENTIONS.md](CODE_CONVENTIONS.md) — 通用编码规范（字段命名、日志脱敏）
- [PROJECT_SPEC_FINAL.md](PROJECT_SPEC_FINAL.md) — 全局日志约束与链路追踪规范
