# 执行计划（2026-03-20）- 回传失败对前端可见 + 根因定位

1. **先写失败测试（RED）**
- 在 `TenderDocumentGenerationControllerTest` 新增用例：service 返回 `success=false` 时，断言 controller 返回 `Result.fail`。

2. **最小改动修复（GREEN）**
- 修改 `TenderDocumentGenerationController#callbackAll`：根据 `TenderDocumentUnifiedCallbackResponse.success` 决定 `Result.success` / `Result.fail`。
- 失败时优先透传统一回传 message；为空时给兜底文案。

3. **补充可定位性**
- 校验 `TenderDocumentGenerationServiceImpl#callbackAll` 在部分失败场景 message 是否携带子项错误信息。
- 如信息不足，补充拼接失败子项的 `responseCode/responseMessage`。

4. **验证**
- 运行 tender-document 模块相关单测（至少 controller + service callback 相关测试）。

5. **失败原因排查**
- 查询目标单据 `tenderDocumentId=48006` 的回传记录与出站交互日志（按最新时间倒序）。
- 输出失败层级（签章/数据包）、错误码、错误文案、traceId（如有）。
