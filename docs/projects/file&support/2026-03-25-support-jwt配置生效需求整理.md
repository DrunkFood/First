# 2026-03-25 support JWT 配置生效需求整理

## 背景
support 模块 `application.yml` 已声明：

```yaml
jwt:
  secret: ${APP_JWT_SECRET:}
  expiration: 7200
  external-expiration: 604800
```

现状排查发现：
- `jwt.external-expiration` 已在 `AuthServiceImpl` 中通过 `@Value` 使用。
- `jwt.secret` 未进入 JWT 签发/校验链路，实际仍使用 `JwtUtil` 默认常量密钥。
- `jwt.expiration` 未生效，内部 Token 过期与 Redis TTL 仍使用常量值。

## 目标
1. 使 `jwt.secret` 在 support 系统中真实生效（签发、校验、解析链路一致）。
2. 使 `jwt.expiration` 在内部登录流程真实生效（JWT claim 过期时间、Redis TTL、返回给前端的 expireIn 保持一致）。
3. 保持 `jwt.external-expiration` 现有行为。
4. 补充回归测试并完成完整测试验证。

## 影响范围（预估）
- `ele-tender-common`：`JwtUtil`（密钥配置能力）及相关单测。
- `ele-tender-support`：JWT 配置接入、`AuthServiceImpl` 登录逻辑、相关单测。

## 验收标准
- 设置自定义 `jwt.secret` 后，基于该密钥签发的 token 可被系统正常校验；不匹配密钥 token 被拒绝。
- 设置自定义 `jwt.expiration` 后，内部登录 token 的 exp 与 Redis TTL、`expireIn` 一致。
- `mvn clean test` 在 `ele-tender-system` 下通过。
