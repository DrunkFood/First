# ele-tender-tender-document

## 模块定位

`ele-tender-tender-document` 是招标文件编制系统后端模块，负责项目或标段级编制入口、步骤流转、评审规则、文件产物和生成回传。
该模块可独立启动，对前端统一暴露 `/api/tender-documents` 下的页面型接口。

## 核心能力

- 提供编制入口、概览和步骤页面接口
- 管理基本信息、采购文件、开标标录、评审规则、检查项、生成回传流程
- 支持采购文件签章文件绑定
- 通过业务系统同步基础数据，并通过文件服务绑定文件引用
- 固化编制单状态、步骤状态、项目锁和回传留痕

## 关键接口分组

- 入口与概览：`/entry`、`/overview`、`/recompile`
- 基本信息：`/basic-info`、`/basic-info/sync`
- 采购文件：`/purchase-file`、`/purchase-file/bind`
- 签章文件：`/purchase-file/signed`、`/purchase-file/signed/bind`
- 开标标录：`/bid-record`、`/bid-record/sync`
- 评审规则：`/evaluation-rules`、`/evaluation-rules/item`、`/evaluation-rules/copy`、`/evaluation-rules/score-type`
- 检查项与步骤流转：`/check-items`、`/complete-and-next`
- 生成与回传：`/generate`、`/generate/records`、`/generate/callback`

所有接口统一返回 `Result<T>`，并通过 `@RequireLogin` 要求登录态。

## 业务约定

- 统一入口 `POST /api/tender-documents/entry` 只接收 `bizType`、`bizId`、`projectId`、`tenderId`
- 公开类项目按项目级编制，邀请类项目按标段级编制
- 步骤固定为 `BASIC_INFO`、`PURCHASE_FILE`、`BID_RECORD`、`EVALUATION_RULE`、`CHECK_ITEMS`、`GENERATE_PACKAGE`
- 编制单状态固定为 `DRAFT`、`GENERATING`、`COMPLETED`
- 页面保存与“完成并进入下一步”拆开，推进步骤前必须做完整校验
- 评审规则采用“头表 + 节点表”，前后端交互使用树形 `children`
- 评审规则分值采用范围模式：仅 `lowest/highest`
- `scoreType` 为编制单级统一配置
- `POST /generate` 只负责产物生成与状态固化，不自动回传业务系统
- 统一回传仅回传 `SIGNED_PDF` 与 `FINAL_PACKAGE_FILE`
- `COMPILE_INFO_PDF` 仅用于下载/打印

## 关键表

- `td_project_lock`
- `td_tender_document`
- `td_tender_document_step`
- `td_tender_document_snapshot`
- `td_tender_rule_header`
- `td_tender_rule_node`
- `td_tender_rule_score_config`
- `td_tender_document_file`
- `td_tender_document_version`
- `td_tender_document_callback`
- `td_tender_document_callback_counter`
- `td_tender_document_generation_record`
- `td_tender_ca_keys_snapshot`

## 依赖关系

- 依赖 `ele-tender-common` 和 `ele-tender-common-interaction`
- 通过交互能力与业务系统同步基础数据、回传产物
- 通过文件服务绑定采购文件、签章文件和生成数据包
- 启动类为 `com.jy.eletender.tenderdocument.TenderDocumentApplication`

## 常用命令

```bash
cd ele-tender-system
mvn -pl ele-tender-tender-document -am spring-boot:run
mvn -pl ele-tender-tender-document -am package
```

本地配置入口：
- `src/main/resources/application.yml`
- `src/main/resources/ele-tender-tender-document-local.yml`

关键配置项：
- `tender-document.version.app-version`
- `tender-document.version.format-version`
- `tender-document.interaction.final-package-suffix`
- `tender-document.interaction.final-package-encrypt-algorithm`
- `tender-document.interaction.final-package-aes-key-base64`
- `tender-document.interaction.compile-info-watermark-text`

## 本地加解密调试

模块内提供本地调试 CLI：`com.jy.eletender.tenderdocument.support.generation.FinalPackageCryptoCli`

## 注意事项

- 页面保存与“完成并进入下一步”是拆开的，步骤推进前必须先做完整校验
- 编制入口会复用基本信息同步能力，同步失败不创建编制单
- 评审规则的 `reviewMode` 由后端根据 `evalMethod + nodeCategory` 固定推导
- 分数类分类仍保留标段级节点、总分和权重，但 `scoreType` 为编制单级统一配置
- `COMPILE_INFO_PDF` 不参与统一回传
- 生成完成后必须固化版本、允许重复回传，并单独保留回传历史记录
