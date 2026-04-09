# ele-tender-interaction

## 模块定位

`ele-tender-interaction` 是电子标系统面向业务系统的交互 starter 聚合模块，不作为独立服务启动。
它用于把电子标系统与外部业务系统之间的协议、客户端、自动装配和标准回调接口封装成可复用依赖。

## 核心能力

- 提供统一客户端 `EleTenderInteractionClient`
- 封装 token 获取、当前外部用户信息、文件信息查询、文件下载和编制入口 URL 构造
- 封装投标文件预存、解密提交、解密状态查询
- 通过自动装配为业务系统暴露固定交互接口
- 统一承载业务系统接入所需的 SPI、协议 DTO、签名校验和异常处理
- 以 starter 形式对外发布，兼容 `JDK 8 + Spring Boot 2.7.x`

## 模块组成

- `ele-tender-interaction-core`
- `ele-tender-interaction-autoconfigure`
- `ele-tender-interaction-spring-boot-starter`

## 固定接口

- `GET /api/eleTender/interaction/identity/current`
- `POST /api/eleTender/interaction/projects/basic-info`
- `POST /api/eleTender/interaction/bid-record-schemes/query`
- `POST /api/eleTender/interaction/ca-keys/query`
- `POST /api/eleTender/interaction/callbacks/tender-pdf`
- `POST /api/eleTender/interaction/callbacks/tender-package`
- `POST /api/eleTender/interaction/callbacks/bid-document-result`
- `POST /api/eleTender/interaction/callbacks/bid-decrypt-result`

## 业务系统必须实现的 SPI

- `InteractionIdentityService`
- `InteractionProjectInfoService`
- `InteractionBidRecordSchemeService`
- `InteractionCaKeysInfoService`
- `InteractionTenderPdfReceiveService`
- `InteractionTenderPackageReceiveService`
- `InteractionBidDocumentResultReceiveService`
- `InteractionBidDecryptResultReceiveService`

## 出站能力

- `getExternalToken`
- `getCurrentExternalUser`
- `getFileInfo`
- `downloadFile`
- `pushBidDocument`
- `submitDecrypt`
- `queryDecryptStatus`
- `buildTenderDocumentEntryUrl`

## 常用命令

```bash
cd ele-tender-system
mvn -pl ele-tender-common-interaction,ele-tender-interaction/ele-tender-interaction-core,ele-tender-interaction/ele-tender-interaction-autoconfigure,ele-tender-interaction/ele-tender-interaction-spring-boot-starter -am test
mvn -pl ele-tender-common-interaction,ele-tender-interaction/ele-tender-interaction-core,ele-tender-interaction/ele-tender-interaction-autoconfigure,ele-tender-interaction/ele-tender-interaction-spring-boot-starter -am package -DskipTests
```

## 注意事项

- starter 的公开 API 和编译目标必须保持 `JDK 8` 兼容
- 协议 DTO 单源放在 `ele-tender-common-interaction`
- 业务系统实现层通过显式 mapper 与内部模型转换
- `ele-tender.interaction.controller.base-path` 默认值为 `/api/eleTender/interaction`
- starter 配置项以 `EleTenderInteractionProperties` 为准
- 详细接入步骤以 `docs/guides/业务系统接入手册.md` 为准
