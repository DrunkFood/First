# CLAUDE.md

本文件定义在本仓库内工作的 Agent 协作规范。除非用户明确要求，否则优先遵循本文档。

## 项目概览

招标文件AI编制工具平台，前端 SPA + 后端 Spring Boot 微服务。

**技术基线**: JDK 21 · Spring Boot 3.2.2 · MyBatis-Plus 3.5.5 · MySQL 8.4.0

**前端**: `ele-tender-support-frontend/` — Vue 3 + TypeScript + Vite + Element Plus + Pinia + Vue Router
→ 详见 [前端 README](ele-tender-support-frontend/README.md)

**后端**: `ele-tender-system/` — Maven 多模块

| 模块 | 端口 | 职责 | 详细文档 |
|------|------|------|----------|
| `ele-tender-common` | — | 公共实体、工具类、异常、统一响应 | [README](ele-tender-system/ele-tender-common/README.md) |
| `ele-tender-common-interaction` | — | 交互协议 DTO/SPI/路径常量（JDK8兼容） | [README](ele-tender-system/ele-tender-common-interaction/README.md) |
| `ele-tender-support` | 8080 | 认证、用户、角色、菜单、版本、外部系统、crypto管理 | [README](ele-tender-system/ele-tender-support/README.md) |
| `ele-tender-file` | 8081 | 文件上传/下载/查询/删除 | [README](ele-tender-system/ele-tender-file/README.md) |
| `ele-tender-tender-document` | 8082 | 招标文件编制、步骤流转、生成回传 | [README](ele-tender-system/ele-tender-tender-document/README.md) |
| `ele-tender-crypto` | 8083 | 投标文件预存、解密调度、Redis Stream 消费 | [README](ele-tender-system/ele-tender-crypto/README.md) |
| `ele-tender-interaction` | — | 业务系统接入 Starter（JDK8兼容） | [README](ele-tender-system/ele-tender-interaction/README.md) |
| `ele-tender-dev-tools` | — | 开发联调 CLI（generate-tender/bid, decrypt-tender/bid），非生产服务 | [使用说明](docs/guides/开发联调CLI工具使用说明.md) |

## 术语规范

| 中文 | 代码术语 |
|------|----------|
| 电子标 | ele-tender |
| 招标文件 | TenderDocument |
| 投标文件 | BidDocument |
| 招标方 | Tenderer |
| 投标方 | Bidder |
| 项目 | project |
| 标段 | tender |
| 标段标的金额 | tenderAmount |
| 开标标录 | bidRecord |
| 标录方案 | schemeContent |
| 标录数据 | bidFormData |
| 资格审查 | qualifications |
| 资信/技术/商务评分 | credit_score / tec_score / business_score |

**评标办法**:
- 最低评标价法 — 评审节点：资格审查、符合性评审、详细评审
- 综合评分法 — 评审节点：资格审查、符合性评审、资信评审、技术评审、商务评审

**文件后缀**:
- 招标文件默认 `.HzctZbs`，投标文件默认 `.HzctTbs`
- 后缀可按接入系统独立配置（`sup_access_system.tender_document_suffix` / `bid_document_suffix`）
- 默认值常量：`FileConstants.DEFAULT_TENDER_DOCUMENT_SUFFIX` / `DEFAULT_BID_DOCUMENT_SUFFIX`
- file 模块 `allowed-types` 需手动包含自定义后缀

## 启动命令

```bash
# 前端（端口 3000）
cd ele-tender-support-frontend && npm run dev

# 后端（在 ele-tender-system/ 目录下执行）
mvn clean test                                          # 运行所有测试
mvn -pl ele-tender-support -am spring-boot:run          # 支撑中心 :8080
mvn -pl ele-tender-file -am spring-boot:run             # 文件服务 :8081
mvn -pl ele-tender-tender-document -am spring-boot:run  # 招标文件 :8082
mvn -pl ele-tender-crypto -am spring-boot:run           # 加解密   :8083
mvn -pl ele-tender-support -am package                  # 打包单模块

# 开发联调 CLI（ele-tender-dev-tools，在 ele-tender-system/ 目录下执行）
# 1. 先构建（首次或代码变更后）
mvn -pl ele-tender-dev-tools -am install -DskipTests -q

# 2. 拼接 classpath 运行（$(...) 适用于 bash/zsh）
DEV_CP="ele-tender-dev-tools/target/ele-tender-dev-tools-2.0.0.jar:$(mvn -pl ele-tender-dev-tools -q dependency:build-classpath -Dmdep.outputFile=/dev/stdout 2>/dev/null)"
java -cp "$DEV_CP" com.jy.eletender.devtools.cli.EleTenderDevCli generate-tender -o ./ele-tender-dev-tools/dev-output
java -cp "$DEV_CP" com.jy.eletender.devtools.cli.EleTenderDevCli generate-bid   -o ./ele-tender-dev-tools/dev-output
java -cp "$DEV_CP" com.jy.eletender.devtools.cli.EleTenderDevCli decrypt-tender  -i <file> -k <key>
java -cp "$DEV_CP" com.jy.eletender.devtools.cli.EleTenderDevCli decrypt-bid     -i <file> -p <pwd>
```

> 使用 `-pl` 时必须加 `-am`，确保依赖模块先构建。
> Spring Boot 模块使用 `<classifier>exec</classifier>` 打包，原始 JAR 作为主 artifact 安装到 local repo，fat JAR 以 `*-exec.jar` 形式保留供部署使用。因此 `dependency:build-classpath` 可直接解析所有依赖，无需手动引用 `.jar.original`。

**提交前检查**: 前端 `npm run build`；后端 `mvn clean test`

## 请求链路

```
浏览器 → 前端 (3000)
  /support-api/*    → rewrite(/api/*)   → 支撑中心 :8080  → MySQL + Redis
  /file-api/*       → 直通              → 文件服务 :8081  → MySQL + 本地磁盘
  /file-esign-api/* → 直通              → 文件服务 :8081  （Esign 专用）
  /crypto-api/*     → rewrite(/api/crypto/*) → 加解密 :8083 → MySQL + Redis + 文件服务

业务系统 → Interaction Starter → /api/eleTender/interaction/*
```

认证双轨：内部用户 JWT (`type=INTERNAL`) / 外部系统 JWT (`type=EXTERNAL`)
→ 详见 [支撑中心 README](ele-tender-system/ele-tender-support/README.md)

## 编码规范

详细规范见 [CODE_CONVENTIONS.md](docs/rules/CODE_CONVENTIONS.md)，以下为高频要点速查：

- **返回结构**: `Result.success(data)` / `Result.fail(code, msg)`；交互接口用 `InteractionResult<T>`
- **实体**: 所有实体继承 `BaseEntity`（自动填充 create/modify 时间、ver、is_delete）
- **权限**: `@RequireLogin` / `@RequirePermission("xxx")`
- **命名**: Service 接口 `I*Service`；DB 表前缀 `sup_*` / `file_*` / `td_*` / `bdc_*`
- **依赖**: 新依赖版本声明在父 POM `<dependencyManagement>`
- **Interaction**: 公开 API 保持 JDK 8 兼容；协议 DTO 只放 `ele-tender-common-interaction`
- **安全**: 签名用 `SignatureUtil`(HMAC-SHA256)、密码用 `PasswordUtil`(BCrypt)、字符集必须显式 UTF-8
- **前端**: Vue 3 `<script setup>`；状态走 Pinia；API 调用放 `src/api/`

## 基础设施

```
MySQL : 10.11.20.50:15005/ele_tender（初始化运行各模块 db/init.sql）
Redis : 10.11.20.50:16879  db=5  password=test123
文件存储: /data/ele-tender/files（文件服务宿主机本地磁盘）
默认管理员: admin / admin123
```

**环境变量覆盖**: `SPRING_DATASOURCE_PASSWORD` · `SPRING_REDIS_PASSWORD` · `APP_JWT_SECRET`

**本地配置覆盖**（各服务均支持）:
```yaml
spring.config.import: optional:file:${user.home}/.ele-tender/{module}-local.yml
```

**服务专属覆盖**: `FILE_STORAGE_BASE_PATH` · `TENDER_DOCUMENT_FINAL_PACKAGE_AES_KEY_BASE64` · `CRYPTO_RSA_PRIVATE_KEY` · `CRYPTO_BIDDER_PWD_FINGERPRINT_SECRET` · `CRYPTO_ORGANIZED_FILE_PATH`

**链路追踪**: 所有服务传播 `X-Trace-Id`，MDC 键 `traceId`

## 文档规范

每次执行任务前整理需求 → `docs/projects/`；生成实施计划 → `docs/plans/`

**重要**: 所有文档必须严格放到以下目录，禁止创建 `docs/superpowers/`、`docs/specs/` 等非规范路径。

| 目录 | 用途 |
|------|------|
| `docs/projects/` | 需求整理、问题分析（包括 brainstorming/spec 产出） |
| `docs/plans/` | 实施计划（对应 projects/） |
| `docs/guides/` | 集成指南、接口文档 |
| `docs/rules/` | 系统规范（权威参考） |

**规范文档索引**（需要深入了解时阅读）:

| 规范 | 文件 |
|------|------|
| **编码规范**（数据库字段、接口格式、分层、异常、安全） | [CODE_CONVENTIONS.md](docs/rules/CODE_CONVENTIONS.md) |
| 全局项目规范（模块、端口、安全、日志、文档治理） | [PROJECT_SPEC_FINAL.md](docs/rules/PROJECT_SPEC_FINAL.md) |
| 招标文件编制规范 | [TENDER_DOCUMENT_PROJECT_SPEC.md](docs/rules/TENDER_DOCUMENT_PROJECT_SPEC.md) |
| 投标文件加解密规范 | [ELE_TENDER_CRYPTO_SPEC.md](docs/rules/ELE_TENDER_CRYPTO_SPEC.md) |
| **招标文件 JSON 格式规范**（明文结构、baseInfo/tenders/bidEvalRules/caKeysInfo/versionInfo） | [TENDER_DOCUMENT_FORMAT_SPEC.md](docs/rules/TENDER_DOCUMENT_FORMAT_SPEC.md) |
| **投标文件 JSON 格式规范**（明文结构、projectInfoRSA、与招标文件对应关系） | [BID_DOCUMENT_FORMAT_SPEC.md](docs/rules/BID_DOCUMENT_FORMAT_SPEC.md) |
| **开标流程规范**（文件预处理、CA锁校验、信封解密、标录回传） | [BID_OPENING_FLOW_SPEC.md](docs/rules/BID_OPENING_FLOW_SPEC.md) |
| 文件服务规范 | [FILE_SERVICE_SPEC.md](docs/rules/FILE_SERVICE_SPEC.md) |
| 支撑中心规范 | [SUPPORT_SYSTEM_SPEC.md](docs/rules/SUPPORT_SYSTEM_SPEC.md) |
| 交互集成规范 | [INTERACTION_INTEGRATION_SPEC.md](docs/rules/INTERACTION_INTEGRATION_SPEC.md) |
| Superpowers 使用规范 | [USE_SUPERPOWER_SEPC.md](docs/rules/USE_SUPERPOWER_SEPC.md) |

**使用指南索引**（开发工具、联调使用）:

| 指南 | 文件 |
|------|------|
| **开发联调 CLI 工具**（generate-tender/bid, decrypt-tender/bid） | [开发联调CLI工具使用说明.md](docs/guides/开发联调CLI工具使用说明.md) |
| 本地加解密工具（crypto 模块内置 Java CLI） | [本地加解密工具指南（Java CLI）.md](docs/guides/本地加解密工具指南（Java CLI）.md) |
| Native 测试向量导出工具 | [NativeVectorExportCli-使用说明.md](docs/guides/NativeVectorExportCli-使用说明.md) |
| Electron 客户端加解密指南 | [Electron 客户端加解密指南.md](docs/guides/Electron%20客户端加解密指南.md) |
| Electron Native 交付清单 | [Electron-native-交付清单.md](docs/guides/Electron-native-交付清单.md) |
| 业务系统接入手册 | [业务系统接入手册.md](docs/guides/业务系统接入手册.md) |

> 代码实现 > `docs/rules/` > 其他文档，三者冲突时以代码为准。
