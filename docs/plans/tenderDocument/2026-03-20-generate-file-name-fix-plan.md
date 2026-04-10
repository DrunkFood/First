# 执行计划（2026-03-20）- 生成文件命名规则修正

1. 先改测试（RED）
- 将两个 generation gateway 测试改为断言“优先使用 SIGNED_PDF 文件名 + 时间戳”。

2. 实现修复（GREEN）
- 调整 `resolveSourcePdfFileName` 选择策略：签章 PDF 优先（或仅取签章 PDF）。
- 保持数据包命名规则不变。

3. 回归验证
- 运行 `DefaultTenderDocumentGenerationGatewayTest` 与 `InteractionTenderDocumentGenerationGatewayTest`。
- 再运行 `TenderDocumentGenerationServiceTest` 保证主流程无回归。
