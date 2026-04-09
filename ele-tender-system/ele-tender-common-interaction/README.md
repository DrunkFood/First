# ele-tender-common-interaction

## 模块定位

`ele-tender-common-interaction` 是电子标系统对外交互协议公共模块，不作为独立服务启动。
它承载业务系统接入电子标系统时使用的协议 DTO、SPI、固定路径常量和交互工具，是交互协议单一来源。

## 核心能力

- 提供交互协议 DTO，如 token、用户信息、项目基本信息、开标标录、文件回调、解密回调等模型
- 提供统一交互返回结构 `InteractionResult<T>`
- 提供业务系统必须实现的 SPI 接口
- 提供固定交互路径常量 `InteractionApiPaths`
- 提供交互签名工具 `InteractionSignatureUtil`

## 边界与归属

- 在 `ele-tender-interaction*` 和对外交互链路中使用的协议模型应优先放在本模块
- 协议 DTO 默认不承载 `jakarta/javax.validation` 等宿主框架注解
- 业务层使用内部命令/视图模型，不应直接把协议 DTO 透传到 service
- 新增或变更协议字段时，必须同步更新对应 mapper 和测试
- 面向外部发布的公共 API 必须保持 JDK 8 兼容

## 依赖关系

- 被 `ele-tender-interaction` 直接依赖
- 被 `ele-tender-support`、`ele-tender-tender-document`、`ele-tender-crypto` 用于协议对接

## 常用命令

```bash
cd ele-tender-system
mvn -pl ele-tender-common-interaction -am test
mvn -pl ele-tender-common-interaction -am package
```

## 注意事项

- 交互基础路径固定为 `/api/eleTender/interaction`
- 当前固定接口包含：
  - `/identity/current`
  - `/projects/basic-info`
  - `/bid-record-schemes/query`
  - `/ca-keys/query`
  - `/callbacks/tender-pdf`
  - `/callbacks/tender-package`
  - `/callbacks/bid-document-result`
  - `/callbacks/bid-decrypt-result`
- 协议模型采用“单源 + 显式转换（mapper）”策略
- 与宿主系统交互时统一透传 `X-Trace-Id`
- 交互签名算法需与主系统 `SignatureUtil` 保持一致
