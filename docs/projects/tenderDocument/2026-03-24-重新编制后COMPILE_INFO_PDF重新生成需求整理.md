# 需求整理（2026-03-24）

## 背景
招标文件编制系统在执行“重新编制”后，用户再次生成最终产物时，需要确保编制信息文件（`COMPILE_INFO_PDF`）是针对当前版本重新生成的，不应继续展示或复用上一个版本的生成结果信息。

## 目标
- 重新编制后进入新版本（`versionNo + 1`）时：
  - 生成页不应展示旧版本的编制完成时间。
  - 生成页不应展示旧版本的最新生成记录。
- 待用户再次点击生成后，基于当前版本重新产出 `COMPILE_INFO_PDF`。

## 影响范围
- 模块：`ele-tender-system/ele-tender-tender-document`
- 重点接口：`GET /api/tender-documents/generate`
- 重点服务：`TenderDocumentGenerationServiceImpl#getGeneratePage`

## 验收要点
- 重新编制完成但未再次生成时，`compileCompleteTime` 为空。
- 重新编制完成但未再次生成时，`latestGenerateRecord` 为空。
- 再次生成成功后，生成页可返回当前版本的编制信息文件与生成记录。
