# ele-tender-file

## 模块定位

`ele-tender-file` 是电子标系统的文件服务模块，提供统一的文件上传、下载、查询和删除能力。
该模块可独立启动，对外接口前缀固定为 `/api/file`，前端联调时通常通过 `/file-api` 或 `/file-esign-api` 代理到本服务。

## 核心能力

- 提供通用文件上传接口，请求中必须包含 `file` 和 `bizType`
- 提供 Esign 专用上传接口，请求中必须包含 `file`，并通过请求参数 `token` 完成认证
- 提供文件下载、文件信息查询、文件删除接口
- 统一落库存储文件元信息，业务模块只绑定 `fileId` 和文件元数据

## 关键接口

- `POST /api/file/upload`
- `POST /api/file/esign/upload`
- `GET /api/file/download/{fileId}`
- `GET /api/file/info/{fileId}`
- `DELETE /api/file/delete/{fileId}`

## 上传白名单

- `.jar`
- `.war`
- `.zip`
- `.tar.gz`
- `.pdf`
- `.HzctZbs`（默认招标文件后缀）
- `.HzctTbs`（默认投标文件后缀）

> 后缀可按接入系统配置。若使用自定义后缀，需在 `file.storage.allowed-types` 中手动添加。

## 依赖关系

- 依赖 `ele-tender-common`
- 被 `ele-tender-support`、`ele-tender-tender-document`、`ele-tender-crypto` 和交互链路使用
- 启动类为 `com.jy.eletender.file.FileApplication`

## 常用命令

```bash
cd ele-tender-system
mvn -pl ele-tender-file -am spring-boot:run
mvn -pl ele-tender-file -am package
```

本地配置入口：
- `src/main/resources/application.yml`
- `src/main/resources/ele-tender-file-local.yml`

## 注意事项

- 文件服务只处理文件能力，不承接业务页面逻辑
- `POST /api/file/esign/upload` 只负责上传签章产物，不负责业务绑定
- Esign 专用接口认证 token 走请求参数 `token`，不走 `Authorization` 请求头
- 默认最大文件大小为 `500MB`
- 日志中不得记录文件二进制内容
- 文件摘要字段统一使用 `fileSha256` / `file_sha256`
- 修改文件服务接口路径或参数时，需要同步前端文件 API 调用和交互文档
