# 文件服务模块规范

## 1. 定位

`ele-tender-file` 提供统一文件能力，只负责文件上传、下载、查询和删除，不承接业务页面逻辑。

## 2. 当前接口

- `POST /api/file/upload`

> `ele-tender-interaction` starter 已通过 `FileClient` 封装此接口，业务系统无需自行对接。

- `POST /api/file/esign/upload`
- `GET /api/file/download/{fileId}`
- `GET /api/file/info/{fileId}`
- `DELETE /api/file/delete/{fileId}`

## 3. 当前表

### `file_info` 表关键字段

| 字段 | 说明 |
|------|------|
| `id` | 文件主键（bigint，自增，对外暴露为 `fileId`） |
| `file_name` | 原始文件名 |
| `file_size` | 文件大小（bytes） |
| `file_type` | 文件扩展名（如 `.pdf`） |
| `biz_type` | 业务类型（上传时由调用方传入，用于分类存储） |
| `storage_path` | 服务端存储路径（相对路径） |
| `file_sha256` | 文件 SHA-256 摘要（十六进制字符串） |
| `upload_user_id` | 上传人 ID |
| `upload_user_name` | 上传人姓名 |

继承 `BaseEntity` 基础字段（`create_time`、`modify_time`、`is_delete` 等）。

## 4. 上传约束

- 通用上传必须提供 `file` 和 `bizType`
- Esign 上传必须提供 `file`
- Esign 上传认证 token 走请求参数 `token`，不走 `Authorization` 头
- 默认最大文件大小 `500MB`

### 当前白名单

- `.jar`
- `.war`
- `.zip`
- `.tar.gz`
- `.pdf`
- `.HzctZbs`（默认招标文件后缀，可按接入系统配置）
- `.HzctTbs`（默认投标文件后缀，可按接入系统配置）

> 若接入系统配置了自定义后缀，需在 `file.storage.allowed-types` 中手动添加对应后缀。

## 5. 文件字段约束

- 文件主键统一命名为 `fileId`
- 文件摘要统一使用 `fileSha256` / `file_sha256`（SHA-256 十六进制字符串，UTF-8 编码）
- 文件元信息统一保存在 `file_info`

## 6. 排障原则

- **文件上传失败** → 检查文件扩展名是否在白名单内、文件大小是否超过 500MB、`FILE_STORAGE_BASE_PATH` 目录是否有写权限
- **下载 404** → 确认 `file_info.storage_path` 对应文件在磁盘上实际存在，检查 `FILE_STORAGE_BASE_PATH` 环境变量配置是否正确
- **Esign 上传报 401** → 确认 `token` 请求参数非空且在有效期内（Esign token 走请求参数，不走 `Authorization` 头）
- **fileSha256 校验失败** → 确认调用方计算方式与服务端一致（SHA-256 十六进制字符串，字节序列使用 UTF-8）

日志与链路追踪规范见 [PROJECT_SPEC_FINAL.md](PROJECT_SPEC_FINAL.md)（日志中不得记录文件二进制内容，需透传 `X-Trace-Id`）。

## 相关规范

- [CODE_CONVENTIONS.md](CODE_CONVENTIONS.md) — 通用编码规范（字段命名、日志脱敏）
- [PROJECT_SPEC_FINAL.md](PROJECT_SPEC_FINAL.md) — 全局日志约束与链路追踪规范
