# 2026-03-25 support JWT 配置生效修复计划

## 目标
修复 support 模块中 `jwt.secret` 与 `jwt.expiration` 配置未生效问题，保证 JWT 签发与校验策略可配置，且与 Redis TTL 一致。

## 实施步骤
1. **先写失败测试（TDD-RED）**
   - 在 `ele-tender-common` 增加 `JwtUtil` 配置能力相关测试（密钥切换后签发/校验行为）。
   - 在 `ele-tender-support` 增加 `AuthServiceImpl.login` 过期时间配置相关测试（token exp、Redis TTL、返回值一致）。
2. **最小实现（TDD-GREEN）**
   - 为 `JwtUtil` 增加可配置密钥能力（默认值回退）。
   - 在 support 中增加 JWT 配置初始化，将 `jwt.secret` 注入 `JwtUtil`。
   - 在 `AuthServiceImpl` 接入 `jwt.expiration`，并统一用于内部 token 过期策略。
3. **回归验证**
   - 先跑新增/受影响测试。
   - 再在 `ele-tender-system` 目录执行 `mvn clean test` 完整验证。
4. **交付说明**
   - 输出变更文件、配置生效点、测试结果与潜在风险说明。

## 风险与控制
- 风险：`JwtUtil` 为静态工具，测试之间可能存在状态污染。
- 控制：提供测试专用重置入口，并在测试 `@AfterEach` 回滚默认状态。
