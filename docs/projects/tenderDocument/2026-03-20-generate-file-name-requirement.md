# 需求整理（2026-03-20）- 最后一步生成文件命名规则调整

## 用户需求
最后一步“生成文件”产物命名规则调整为：
1. PDF 文件：上传的签章 PDF 文件名不变 + `[yyyyMMddHHmmss]` + `.pdf`
2. 数据包文件：`[项目编号]采购文件电子数据包[yyyyMMddHHmmss].HzctZbs`

## 现状差异
- 数据包命名当前已符合规则。
- PDF 命名当前优先取 `PURCHASE_SOURCE_PDF` 文件名，不是签章文件名，需调整。

## 影响范围
- `DefaultTenderDocumentGenerationGateway`
- `InteractionTenderDocumentGenerationGateway`
- 对应单元测试
