# 2026-03-25 tender-document JWT 统一配置需求整理

## 背景
- 测试环境接口 `POST /tender-api/api/tender-documents/entry` 返回 `code=401`。
- 同一外部 token 访问 `support-api/external/userinfo` 成功，说明 token 本身可用。

## 目标
- 统一 `tender-document` 服务与 `support` 的 JWT 运行时配置逻辑。
- 确保 `tender-document` 使用与环境一致的 `jwt.secret` 进行验签。

## 影响范围
- 模块：`ele-tender-system/ele-tender-tender-document`
- 代码：配置类 + 配置文件 + 最小回归测试

## 验证标准
- 新增测试覆盖 `JwtUtil.configure` 在 tender-document 模块中的初始化行为。
- 相关模块测试通过。
