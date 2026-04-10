# 独立 Demo 业务系统与交互闭环设计文档

> **For Claude:** 设计确认后，下一步使用 `superpowers:writing-plans` 产出实施计划，再进入实现。

**Goal:** 为电子标系统补齐真实业务系统交互闭环，并提供一个独立 Git 仓库形式的 demo 业务系统，作为业务系统接入 starter 的参考样例。

**Architecture:** 主仓库只承载电子标系统正式能力与接入文档；demo 目录作为嵌套独立 Git 仓库，承载 starter 接入样例。`tender-document` 通过 interaction client 正式访问 demo/业务系统固定接口，demo 通过 starter 暴露 5 个 SPI 接口并提供一个业务侧主动发起“获取 token + 生成编制页地址”的样例入口。

**Tech Stack:** Java 21、Spring Boot 3.2.2、Spring MVC、Maven、JUnit 5、MockMvc、内存仓库（ConcurrentHashMap / List）

---

## 1. 主仓库设计

### 1.1 `tender-document` 正式 Gateway 替换

- 用正式实现替换：
  - `DefaultTenderDocumentSyncGateway`
  - `DefaultTenderDocumentGenerationGateway`
  - `DefaultTenderDocumentCallbackGateway`
- 正式实现通过 interaction client 访问业务系统固定接口与文件服务接口，替代本地假数据与伪造文件元信息。

### 1.2 正式交互职责

- 同步：
  - 入口与基本信息页从业务系统拉取 `ProjectBasicInfoResponse`
  - 标录页从业务系统拉取标录方案
- 生成：
  - 真实调用文件服务查询元信息 / 下载文件
  - 生成数据包和编制回执时不再伪造 `fileId` / `sha256`
- 回传：
  - 按签章文件、数据包两条链路调用业务系统 starter 暴露的回调接口
  - 失败结果、失败码、重试次数继续按现有表结构留痕

### 1.3 主仓库配套改动

- 补齐统一错误处理与明确异常码映射
- 为交互链路补日志，确保 `traceId` 与业务键可串联
- 更新接入文档，指向 demo 目录的 README
- 在主仓库 `.gitignore` 中忽略 demo 目录

## 2. Demo 仓库设计

### 2.1 仓库边界

- 目录建议：`demo-business-system/`
- 物理位置在当前仓库下，但独立 `git init`
- 不纳入主仓库 Maven reactor，不参与主仓库构建
- 通过 Maven 依赖引入已安装到本地仓库的 starter 制品

### 2.2 应用职责

- 引入 `ele-tender-interaction-spring-boot-starter`
- 通过 5 个 SPI 暴露业务系统固定接口
- 提供一个主动调用电子标系统获取 token / 生成编制页地址的样例接口
- 提供一个查看回传记录的样例接口

### 2.3 数据模型

- `DemoProject`
- `DemoTender`
- `DemoBidRecordScheme`
- `DemoCallbackRecord`

全部使用内存数据，启动时初始化一组公开类项目样例和一组邀请类项目样例。

### 2.4 注释策略

- SPI 实现类中直接注明“真实项目应替换为数据库/领域服务调用”
- 业务侧主动发起调用的 service 中注明 token 与 URL builder 的标准用法
- README 中写清楚：
  - 前置安装步骤
  - 关键配置项
  - 5 个 SPI 各自职责
  - 真实项目通常需要替换哪些代码

## 3. 主链路

### 3.1 业务系统主动进入电子标

1. demo 业务系统接收进入编制请求
2. 组装外部用户请求
3. 调电子标 `/api/external/token`
4. 调 `buildTenderDocumentEntryUrl`
5. 返回前端跳转地址

### 3.2 电子标系统反向调用业务系统

1. `POST /api/tender-documents/entry`
2. `tender-document` 通过正式 Gateway 拉取项目基本信息
3. 后续基本信息同步、标录同步都走业务系统 starter 固定接口
4. 生成完成后，签章 PDF / 数据包分别回传业务系统
5. demo 业务系统记录回传结果，供接口查询

## 4. 非目标

- 不实现 demo 数据库持久化
- 不把 demo 做成生产可用业务系统
- 不在 demo 中引入复杂认证、权限或多租户机制
- 不把 demo 纳入主仓库 CI 构建主路径
