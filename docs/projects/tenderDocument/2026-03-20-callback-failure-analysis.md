# 需求整理（2026-03-20）- 回传失败可见性与根因排查

## 用户反馈
- 调用接口：`POST /api/tender-documents/generate/callback?tenderDocumentId=48006`
- 现象：文件回传失败。
- 诉求：
  1. 只要有一个回传动作失败，接口应明确告诉前端“调用失败”。
  2. 排查失败原因。

## 当前问题判断
- 统一回传返回模型 `TenderDocumentUnifiedCallbackResponse` 已包含 `success` 字段。
- 但控制器当前固定返回 `Result.success(...)`，导致外层统一响应码仍是成功。
- 前端如果按统一响应 `Result.code` 判断，会误判为成功。

## 预期行为
- 当 `signedFileResult` 或 `packageFileResult` 任一失败时：
  - 外层统一响应应为 `Result.fail(...)`。
  - message 需包含可定位失败信息（至少包含失败子项的错误信息）。
- 当全部成功时：
  - 外层统一响应继续返回 `Result.success(...)`。

## 根因排查目标
- 从 `callbackAll -> callbackFile -> TenderDocumentCallbackGateway -> BusinessSystemRemoteClient` 链路确认失败发生层。
- 优先通过 `td_tender_document_callback`（回传历史）与 `sup_access_log`（交互出站日志）定位失败原因。
