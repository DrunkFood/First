## tender-document-tool 招标文件AI编制工具完整项目

电子标系统主仓库，包含支撑中心前端、后端多模块服务、交互 starter、招标文件编制系统和投标文件加解密服务。

## 仓库结构

- `ele-tender-support-frontend`
  支撑中心前端，Vue 3 + TypeScript + Vite
- `ele-tender-system`
  后端 Maven 多模块工程
  - `ele-tender-common`
  - `ele-tender-common-interaction`
  - `ele-tender-support`
  - `ele-tender-file`
  - `ele-tender-interaction`
  - `ele-tender-tender-document`
  - `ele-tender-crypto`
- `docs`
  项目文档
  - `docs/rules`：系统规范与模块规范
  - `docs/guides`：接入指南、工具说明、OpenAPI
  - `docs/projects`：需求整理、分析记录
  - `docs/plans`：实施计划

## 服务与端口

- `ele-tender-support`：`8080`
- `ele-tender-file`：`8081`
- `ele-tender-tender-document`：`8082`
- `ele-tender-crypto`：`8083`
- `ele-tender-support-frontend` 开发端口：`3000`

## 关键能力

- 支撑中心：认证、用户、角色、菜单、版本、外部系统、访问日志、crypto 管理
- 文件服务：文件上传、下载、信息查询、删除、签章上传
- 交互层：业务系统接入协议、SPI、固定回调接口、统一 client
- 招标文件编制：项目/标段级编制、步骤流转、评审规则、生成回传
- 加解密服务：投标文件预存、解密请求、状态查询、异步回调

## 前端代理

`ele-tender-support-frontend` 开发环境代理如下：

- `/support-api -> http://localhost:8080/api`
- `/file-api -> http://localhost:8081`
- `/file-esign-api -> http://localhost:8081`
- `/crypto-api -> http://localhost:8083/api/crypto`

## 快速启动

前端：

```bash
cd ele-tender-support-frontend
npm install
npm run dev
```

后端：

```bash
cd ele-tender-system
mvn -pl ele-tender-support -am spring-boot:run
mvn -pl ele-tender-file -am spring-boot:run
mvn -pl ele-tender-tender-document -am spring-boot:run
mvn -pl ele-tender-crypto -am spring-boot:run
```

测试或打包：

```bash
cd ele-tender-system
mvn clean test
mvn -pl ele-tender-support -am package
```

## 关键规范入口

- 项目总规范：`docs/rules/PROJECT_SPEC_FINAL.md`
- 支撑中心规范：`docs/rules/SUPPORT_SYSTEM_SPEC.md`
- 文件服务规范：`docs/rules/FILE_SERVICE_SPEC.md`
- 交互集成规范：`docs/rules/INTERACTION_INTEGRATION_SPEC.md`
- 招标文件编制规范：`docs/rules/TENDER_DOCUMENT_PROJECT_SPEC.md`
- 加解密模块规范：`docs/rules/ELE_TENDER_CRYPTO_SPEC.md`

## 接入与开发文档

- 业务系统接入 starter：`docs/guides/业务系统接入手册.md`
- 业务系统接入解密服务：`docs/guides/业务系统接入解密服务指南.md`
- 本地加解密工具：`docs/guides/本地加解密工具指南（Java CLI）.md`
- Electron 客户端加解密：`docs/guides/Electron 客户端加解密指南.md`
