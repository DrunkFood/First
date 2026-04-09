# ele-tender-common

## 模块定位

`ele-tender-common` 是后端公共基础模块，不作为独立服务启动。
它承载非交互专用的公共实体、统一返回结构、异常体系、安全组件、日志组件和通用工具。

## 核心能力

- 提供统一返回结构 `Result<T>`
- 提供基础实体 `BaseEntity`
- 提供通用异常、响应码和业务异常
- 提供登录态、安全上下文、JWT 解析与 `@RequireLogin`
- 提供 HTTP 入站日志、`traceId` 透传和访问日志落库能力
- 提供 `SignatureUtil`、`PasswordUtil`、`JwtUtil` 等公共工具
- 提供 `file_*`、`sup_*`、`bdc_*` 等跨模块共享实体

## 边界与归属

- 仅非交互项目使用的通用类型放在本模块
- 跨模块复用但不面向业务系统协议的枚举优先放在本模块
- 不承载对外交互协议 DTO、SPI、固定交互路径
- 不承载模块业务 service 逻辑

## 依赖关系

- 被 `ele-tender-support`、`ele-tender-file`、`ele-tender-tender-document` 直接依赖
- 被 `ele-tender-crypto` 直接依赖
- 被 `ele-tender-interaction` 间接复用

## 常用命令

```bash
cd ele-tender-system
mvn -pl ele-tender-common -am test
mvn -pl ele-tender-common -am package
```

## 注意事项

- 全系统 HTTP 接口统一返回 `Result<T>`
- 认证采用 JWT + Redis 双校验，公共登录校验能力由本模块提供
- 全系统统一透传 `X-Trace-Id`，MDC 键固定为 `traceId`
- 外部系统签名统一使用 `SignatureUtil` 的 `HMAC-SHA256`
- 密码统一使用 `PasswordUtil` 的 `BCrypt`
- 所有 `String.getBytes()` / `new String(bytes)` 必须显式指定 UTF-8

## 参考规范

本模块是以下规范的实现基础，修改本模块时须对照阅读：

- [CODE_CONVENTIONS.md](../../../docs/rules/CODE_CONVENTIONS.md) — 数据库字段、接口格式、分层约束、异常处理、安全规范
- [PROJECT_SPEC_FINAL.md](../../../docs/rules/PROJECT_SPEC_FINAL.md) — 全局模块边界与安全约束
