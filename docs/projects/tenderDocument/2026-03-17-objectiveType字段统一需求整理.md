# 2026-03-17 objectiveType 字段统一需求整理

## 需求来源
- 用户要求将 `scoreRules` 示例中的 `jectiveType` 直接统一为 `objectiveType`。
- 明确要求：不做兼容，直接修改。

## 目标范围
- 后端代码：评审规则树节点 DTO 与相关业务逻辑。
- SQL：补充数据库历史数据修复脚本，将存量 JSON 中 `jectiveType` 替换为 `objectiveType`。
- 文档：接口文档与联调文档示例同步。
- 协作文档：检查 `AGENTS.md` 与 `README.md` 是否需要同步，若无该字段无需改动。

## 非目标
- 不变更 `td_tender_rule_node.score_attribute` 等结构化字段名（该字段语义不变）。
- 不新增新旧字段并存或别名兼容逻辑。

## 风险
- 存量调用方若仍传 `jectiveType` 将失败（符合“直接修改”要求）。
- 历史快照 JSON 如未迁移，可能影响回显一致性。
