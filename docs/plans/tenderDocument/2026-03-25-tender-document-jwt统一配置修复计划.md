# 2026-03-25 tender-document JWT 统一配置修复计划

1. 新增失败测试（RED）
- 在 `tender-document` 模块新增 `JwtRuntimeConfig` 相关测试。
- 先运行该测试，确认失败。

2. 实现最小修复（GREEN）
- 新增 `JwtRuntimeConfig`，在模块启动时调用 `JwtUtil.configure(...)`。
- 在 `application.yml` 增加 `jwt.secret` 可配置项（兼容 `APP_JWT_SECRET`）。

3. 回归验证
- 运行新增测试与模块最小相关测试，确认通过。
- 输出变更与上线验证建议。
