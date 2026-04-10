# 2026-03-24 originalCryptoSystem 业务掌握需求整理

## 1. 本次需求

- 对历史 Java 项目 `/Users/loucy/Documents/jyWorkspace/CodeXWorkSpace/originalCryptoSystem` 进行业务逻辑掌握。
- 输出可读文档，作为后续在 `ele-tender-total` 中重实现的输入材料。

## 2. 目标范围

- 聚焦 `information-biddingfile-decrypt` 模块。
- 识别核心业务链路：
  - 待处理标段获取
  - 投标文件解密
  - 投标文件解析与校验
  - 标录/签到落库
  - 结构化信息推送评标系统
  - 异常补偿与状态回写
- 识别关键外部依赖：主体库、评标系统、认证/权限、Redis。
- 提炼在 `ele-tender-total` 的重实现边界与分层建议。

## 3. 非目标

- 本次不做代码迁移与接口改造。
- 本次不做数据库结构变更。
- 本次不做性能压测与容量评估。

## 4. 产出物

- `docs/plans/2026-03-24-originalCryptoSystem业务拆解与迁移计划.md`
- `docs/projects/2026-03-24-originalCryptoSystem业务逻辑分析报告.md`

