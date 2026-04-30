# 交互集成规范

## 1. 定位

本文档描述 `ele-ai-tender-common-interaction` 与 `ele-ai-tender-interaction` 的对外接入规范。

## 2. 模块组成

- `ele-ai-tender-common-interaction`
- `ele-ai-tender-interaction-core`
- `ele-ai-tender-interaction-autoconfigure`
- `ele-ai-tender-interaction-spring-boot-starter`

## 3. 兼容性

- 公开 API 必须保持 JDK 8 兼容
- 推荐运行环境：Spring Boot 2.7.x + Spring MVC

## 4. 固定路径

统一前缀：`/api/eleAiTender/interaction`

当前固定接口：

- `GET /identity/current`
- `POST /projects/basic-info`
- `POST /bid-record-schemes/query`
- `POST /ca-keys/query`
- `POST /callbacks/tender-pdf`
- `POST /callbacks/tender-package`
- `POST /callbacks/bid-document-result`
- `POST /callbacks/bid-decrypt-result`

## 5. 业务系统必须实现的 SPI

- `InteractionIdentityService`
- `InteractionProjectInfoService`
- `InteractionBidRecordSchemeService`
- `InteractionCaKeysInfoService`
- `InteractionTenderPdfReceiveService`
- `InteractionTenderPackageReceiveService`
- `InteractionBidDocumentResultReceiveService`
- `InteractionBidDecryptResultReceiveService`

## 6. 出站客户端能力

- 获取 external token
- 获取当前外部用户信息
- 查询文件信息
- 下载文件
- 上传文件
- 推送投标文件预存
- 提交解密请求
- 查询解密状态
- 构造招标文件编制入口 URL

## 7. 配置项

统一前缀：`ele-ai-tender.interaction`

关键配置：

- `api-base-url`
- `page-base-url`
- `app-key`
- `app-secret`
- `token-path`
- `user-info-path`
- `file-base-url`
- `file-info-path`
- `file-download-path`
- `file-upload-path`
- `crypto-base-url`
- `bid-document-push-path`
- `bid-decrypt-submit-path`
- `bid-decrypt-status-path`

## 8. 模型约束

- 协议 DTO 单源维护于 `ele-ai-tender-common-interaction`
- controller/facade 边界做显式 mapper 转换

协议 DTO 通用约束（validation 注解限制、service 层不透传协议 DTO 等）见 [CODE_CONVENTIONS.md — DTO/类型归属](CODE_CONVENTIONS.md#34-dto--类型归属)。

## 相关规范

- [CODE_CONVENTIONS.md](CODE_CONVENTIONS.md) — DTO 归属单源原则、分层约束
- [PROJECT_SPEC_FINAL.md](PROJECT_SPEC_FINAL.md) — 全局模块边界
