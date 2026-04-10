# originalCryptoSystem 业务拆解与迁移计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不拷贝历史实现细节的前提下，完成 `information-biddingfile-decrypt` 业务在 `ele-tender-total` 的可重建方案。

**Architecture:** 以“任务获取 -> 任务执行 -> 文件处理 -> 业务校验 -> 数据落库 -> 对外推送”的流程分层为主线，拆分为调度层、领域服务层、基础设施层和接口适配层；对历史系统的并发控制与状态机进行等价迁移，补齐统一异常和日志规范。

**Tech Stack:** Java 21, Spring Boot 3.2.2, MyBatis-Plus 3.5.5, MySQL 8.4.0, Redis, JWT

---

### Task 1: 业务边界与契约冻结

**Files:**
- Review: `/Users/loucy/Documents/jyWorkspace/CodeXWorkSpace/originalCryptoSystem/information-biddingfile-decrypt/src/main/java/com/jy/information/biddingfiledecrypt/controllers/BidDocumentDownloaderController.java`
- Review: `/Users/loucy/Documents/jyWorkspace/CodeXWorkSpace/originalCryptoSystem/information-biddingfile-decrypt/src/main/java/com/jy/information/biddingfiledecrypt/controllers/EvaluationInfoSynchronousController.java`
- Review: `/Users/loucy/Documents/jyWorkspace/CodeXWorkSpace/originalCryptoSystem/information-biddingfile-decrypt/src/main/java/com/jy/information/biddingfiledecrypt/services/BiddingFileHandlerProcessor.java`

- [ ] 梳理对外接口、输入输出、鉴权与容错规则。
- [ ] 形成“必须兼容行为清单”（含失败语义与可观测性语义）。

### Task 2: 状态机与数据库映射

**Files:**
- Review: `/Users/loucy/Documents/jyWorkspace/CodeXWorkSpace/originalCryptoSystem/information-biddingfile-decrypt/src/main/resources/mappers/trade/NetBidRecordMapper.xml`
- Review: `/Users/loucy/Documents/jyWorkspace/CodeXWorkSpace/originalCryptoSystem/information-biddingfile-decrypt/src/main/resources/mappers/trade/TenderMapper.xml`
- Review: `/Users/loucy/Documents/jyWorkspace/CodeXWorkSpace/originalCryptoSystem/information-biddingfile-decrypt/src/main/resources/mappers/trade/TradeMapper.xml`

- [ ] 固化 `FILE_DECRYPT_STATE` 状态流转与幂等条件。
- [ ] 固化“待处理标段/标录”查询筛选条件与排序策略。
- [ ] 梳理落库对象（标录、临时标录、签到、联合体签到、广播队列）。

### Task 3: 并发模型迁移

**Files:**
- Review: `/Users/loucy/Documents/jyWorkspace/CodeXWorkSpace/originalCryptoSystem/information-biddingfile-decrypt/src/main/java/com/jy/information/biddingfiledecrypt/tasks/TendersTaskAcquireManager.java`
- Review: `/Users/loucy/Documents/jyWorkspace/CodeXWorkSpace/originalCryptoSystem/information-biddingfile-decrypt/src/main/java/com/jy/information/biddingfiledecrypt/tasks/TendersTaskHandleManager.java`
- Review: `/Users/loucy/Documents/jyWorkspace/CodeXWorkSpace/originalCryptoSystem/information-biddingfile-decrypt/src/main/java/com/jy/information/biddingfiledecrypt/tasks/OneTenderTaskHandler.java`

- [ ] 拆分“项目级并发”与“标录级并发”两个维度。
- [ ] 设计可替代 `BlockingDeque + Semaphore + 固定线程池` 的实现（可先保守迁移）。
- [ ] 增加安全停机与任务恢复策略。

### Task 4: 文件解密与解析链路迁移

**Files:**
- Review: `/Users/loucy/Documents/jyWorkspace/CodeXWorkSpace/originalCryptoSystem/information-biddingfile-decrypt/src/main/java/com/jy/information/biddingfiledecrypt/services/BiddingFileHandlerProcessor.java`
- Review: `/Users/loucy/Documents/jyWorkspace/CodeXWorkSpace/originalCryptoSystem/information-biddingfile-decrypt/src/main/java/com/jy/information/biddingfiledecrypt/services/JsonBidDocumentProcessor.java`
- Review: `/Users/loucy/Documents/jyWorkspace/CodeXWorkSpace/originalCryptoSystem/information-biddingfile-decrypt/src/main/java/com/jy/information/biddingfiledecrypt/utils/DecryptUtils.java`

- [ ] 迁移“双重解密 + MD5 校验 + JSON 流式解析 + 附件 Base64 落盘”流程。
- [ ] 迁移三类业务校验：归属校验、版本校验、主体名称校验。
- [ ] 迁移联合体投标校验规则。

### Task 5: 外部系统适配与失败补偿

**Files:**
- Review: `/Users/loucy/Documents/jyWorkspace/CodeXWorkSpace/originalCryptoSystem/information-biddingfile-decrypt/src/main/java/com/jy/information/biddingfiledecrypt/services/EvaluationInfoSynchronousService.java`
- Review: `/Users/loucy/Documents/jyWorkspace/CodeXWorkSpace/originalCryptoSystem/information-biddingfile-decrypt/src/main/java/com/jy/information/biddingfiledecrypt/services/BiddingFileHandlerProcessor.java`

- [ ] 迁移主体库企业信息校验调用。
- [ ] 迁移推送评标系统前缓存与失败回写逻辑。
- [ ] 统一异常包装为 `Result`，满足 ele-tender 规范。

### Task 6: 在 ele-tender-total 的落地设计

**Files:**
- Create: `/Users/loucy/Documents/jyWorkspace/CodeXWorkSpace/ele-tender-total/docs/projects/2026-03-24-originalCryptoSystem业务逻辑分析报告.md`

- [ ] 给出模块落位建议（`ele-tender-tender-document` 为主，必要时拆 `common-interaction` DTO）。
- [ ] 给出分层与接口边界建议（controller/facade/service/mapper）。
- [ ] 给出最小可用迁移里程碑与回归清单。

