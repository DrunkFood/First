# 2026-03-24 originalCryptoSystem 业务逻辑分析报告

## 1. 项目定位

- 目标系统：`information-biddingfile-decrypt`
- 业务职责：在开标进行中批量处理待解密投标文件，完成解密、解析、校验、落库、签到，并向评标系统推送结构化数据。
- 技术形态：Spring Boot 单服务 + MyBatis + 外部 Feign + Redis 缓存。

## 2. 代码结构与入口

- 启动类：`StartApplication`
  - 开启 Mapper 扫描、Feign、缓存。
- 运行入口：`MainThreadCreator implements CommandLineRunner`
  - 启动后自动拉起两个常驻线程：
    - `TendersTaskAcquireManager`（任务获取）
    - `TendersTaskHandleManager`（任务执行）

## 3. 对外接口

### 3.1 下载已解密文件

- `GET /bidDocument/download`
- 参数：`tenderNo`、`fileType`、`fileName`
- 行为：从 `${decrypt-file-path}/{tenderNo}/Origin/{fileType}/{fileName}` 读取文件并下载。

### 3.2 查询招标文件版本号

- `POST /sync/biddingFileConstructInfo`
- 参数：`tenderId`
- 行为：查询 `COMMON_MAIN_RECORD` 最新通过版本（`EXAMINE_STATUS=4 and TYPE in (21,22)`）并返回。

## 4. 核心业务主流程

1. 任务获取线程循环拉取“待处理标段”（限制数量 = 队列剩余容量）。
2. 标段进入共享阻塞队列后，由任务处理线程消费。
3. 每个标段创建 `OneTenderTaskHandler`，并发处理该标段下若干 `NET_BID` 记录。
4. 每条标录先 CAS 抢占处理权限：`FILE_DECRYPT_STATE 0 -> 1`。
5. 进入 `BiddingFileHandlerProcessor.handle()` 执行完整链路：
   - 企业信息校验（主体库）
   - 一级解密（`.upload -> .decrypt`）
   - MD5 校验
   - （综合交易）二级解密（`.decrypt -> .origin`）
   - JSON 流式解析
   - 归属/版本/企业名/联合体校验
   - 保存临时标录
   - 推送评标系统结构化信息
   - 企业签到
6. 任一步骤异常会触发“广播 + 状态回写 + 错误标录 +（尽力）签到”的补偿逻辑。

## 5. 数据状态机与幂等控制

### 5.1 `NET_BID.FILE_DECRYPT_STATE`

- `0`：待处理
- `1`：处理中（通过 `update ... where state=0` 抢占）
- `2`：处理成功
- `3`：处理失败

### 5.2 幂等关键点

- 抢占处理权限 SQL 保证同一标录只被一个线程成功改为 `1`。
- 成功/失败更新都要求当前状态是 `1`，避免并发覆盖。
- `setNetBidDocumentImportInfo` 仅在 `FILE_IMPORT_STATE=INIT` 时更新，避免重复失败回写。

## 6. 任务筛选业务规则（SQL 实际条件）

### 6.1 待处理标段

`TenderMapper.getNeedDecryptTenders` 条件：
- 开标时间 `START_TIME` 在“今天零点前 1 天”到“当前时间”区间。
- 标段在线投标：`IS_BID_ONLINE=1`。
- 至少存在一条满足条件的 `NET_BID`：
  - `KEY_DECRYPT_STATE=2`
  - `FILE_DECRYPT_STATE=0`
  - `BID_STATE=1`
  - `DEPLOY_CONFIG_ID` 匹配当前部署
  - 对应开标流程 `NET_OPENBID_PROCESS` 已开始未结束（`IS_START=1 and IS_END=0`）。

### 6.2 待处理标录

`NetBidRecordMapper.getPartNeedHandleNetBidRecords` 条件：
- 指定 `tenderId + arrangeId`
- `FILE_DECRYPT_STATE=0`
- `KEY_DECRYPT_STATE=2`
- `BID_STATE=1`
- `DEPLOY_CONFIG_ID` 匹配

## 7. 文件处理与解析规则

### 7.1 双重解密

- 一级：`FileDecryptUtils.decryptFileEx` 使用记录中的 `ENCRYPTED_KEY`。
- 二级（综合交易场景）：使用配置的 AES-CBC 参数：
  - `originEncryptKey`（Base64 解码后作为 key）
  - `originEncryptIv`

### 7.2 一致性校验

- 一级解密后必须通过 MD5 校验（与 `ENCRYPT_FILE.SOURCE_FILE_MD5` 一致）。

### 7.3 JSON 解析

- 采用 Gson `JsonReader` 流式解析，读取：
  - `tenderInfo`
  - `tenderFile.versionNumber`
  - `recordingInfo`
  - `biddingFile`（含附件 Base64）
  - `biddinguserInfo`
  - `unionEnterprises`
  - `rulePageList`
- `biddingFile.fileBase` 会落盘为实际文件。

### 7.4 三类业务校验开关（配置项）

- `checkBidDocumentBelongs`：投标文件归属项目校验（tenderNo）。
- `checkTenderFileVersion`：投标文件版本与招标文件版本一致校验。
- `checkBidDocumentNameWithSubject`：投标文件企业名与主体库企业名一致校验。

### 7.5 联合体校验

当项目允许联合体且当前标录为联合体投标时：
- 必须能查到联合体成员。
- 成员数量必须与投标文件声明一致。
- 每个成员企业名必须在投标文件联合体列表中存在。

## 8. 落库与外部交互

### 8.1 落库对象

- 直播/广播：`OPENBID_QUEUE`
- 错误标录：`OPENBID_RECORD` + `OPENBID_RECORD_TEMP`
- 正常标录：`OPENBID_RECORD_TEMP`（商务标场景会更新报价）
- 企业签到：`OPENBID_SIGN`
- 联合体签到：`OPENBID_UNION_SIGN`

### 8.2 外部系统

- 主体库：查询企业详情（用于企业信息校验）。
- 评标系统：接收结构化投标信息推送。
- Redis：先缓存 `EvaluationSyncInfoDTO`（保留 24h），key 规则：`bidding-file-decrypt:tender:{tenderId}:netId:{netBidId}`。

## 9. 异常处理策略

- 设计思路：主流程尽量不中断线程，错误尽量“记录并继续后续记录处理”。
- 两类补偿入口：
  - 解密前异常：广播 + 导入失败 + 解密失败 + 错误标录 + 签到
  - 解密后异常：广播 + 导入失败 + 错误标录 + 签到
- 风险点：
  - 控制器层无统一异常包装（依赖框架默认与局部 try-catch）。
  - 线程内异常吞掉后继续，可能导致“部分失败但外层感知不足”。

## 10. 与 ele-tender-total 的重实现映射建议

### 10.1 模块落位

- 建议主落位：`ele-tender-system/ele-tender-tender-document`
- 公共协议/DTO：若需跨模块复用，放 `ele-tender-common-interaction`，并通过 mapper 与内部 command/view 显式转换。

### 10.2 分层建议

- `controller/facade`：仅参数接收、鉴权、响应包装（`Result<T>`）。
- `service`：编排完整业务链路（解密、校验、落库、推送、补偿）。
- `domain service`：拆分企业校验、文件解密、JSON解析、联合体校验、推送器。
- `repository/mapper`：只承载 SQL 与持久化映射。

### 10.3 并发与调度建议

- 初期可保持“双层并发”模型以降低迁移风险。
- 中期可升级为可观测任务框架（任务表 + 可恢复消费 + 优雅停机）。

### 10.4 必补治理项（按你们规范）

- 全局异常处理统一 `Result` 结构。
- 接口鉴权改造为 `@RequireLogin` / `@RequirePermission`。
- 全链路日志补 `traceId`（MDC）并确保可落库查询。
- 明文敏感配置改环境变量覆盖。

## 11. 迁移最小切片建议

1. 先做“单标录同步处理链路”（不启多线程），跑通：解密 -> 校验 -> 落库 -> 推送。
2. 再加“标段级任务获取 + 标录级并发处理”。
3. 最后补齐下载接口、联调辅助接口与运维可观测性。

## 12. 当前识别的已知技术债

- `MainThreadCreator` 包含大量测试/演示方法，与生产入口耦合。
- `acquirePersonnelBid` 仍为 TODO。
- 异常分层与错误码体系较弱。
- 与 Windows 路径和本地目录结构耦合较深。

