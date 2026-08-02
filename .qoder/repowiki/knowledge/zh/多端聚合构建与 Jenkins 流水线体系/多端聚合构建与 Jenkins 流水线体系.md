---
kind: build_system
name: 多端聚合构建与 Jenkins 流水线体系
slug: build_system
category: build_system
scope:
    - '**'
---

## 1. 系统概览

本项目采用**前后端分离 + Maven 多模块后端 + 双前端单页应用**的架构，构建体系围绕以下三条主线展开：

- **后端服务**：Maven 聚合工程（ele-ai-tender-system），按领域拆分为 common、core、ai、file、support、interaction 等子模块，通过 spring-boot-maven-plugin 打包为可执行 JAR。
- **业务前端**：ele-ai-tender-frontend（Vue3 + Vite + Element Plus），输出静态资源到 ele-ai-tender-web 目录。
- **支撑前端**：ele-ai-tender-support-frontend（Vue3 + Vite + Element Plus），输出静态资源到 ele-ai-tender-support-web 目录。

所有产物最终由 Jenkins Pipeline 统一编排，部署到 Windows 服务器并通过 Nginx 对外提供 HTTP 服务。

## 2. 核心构建工具与版本

| 层次 | 工具 | 版本/配置 |
|------|------|-----------|
| JDK | JDK 21 | `java.version=21` |
| 后端构建 | Maven + spring-boot-maven-plugin | Spring Boot 3.2.2, MyBatis Plus 3.5.5, Lombok 1.18.34 |
| 前端构建 | Vite 7.x + Vue TSC | Node 22.21.1 (nvm) |
| CI/CD | Jenkins Pipeline (Jenkinsfile) | agent any, tools: maven/JDK21 / nodejs 22.21.1 |
| 部署目标 | Windows 服务器 (10.11.20.50) | 通过 SSH 推送 + 外部脚本启动/回滚 |

## 3. 后端构建（Maven 多模块）

根 POM (`ele-ai-tender-system/pom.xml`) 使用 `<packaging>pom</packaging>` 作为聚合器，在 `<modules>` 中声明七个子模块，并在 `<dependencyManagement>` 中集中管理所有第三方依赖版本。每个业务模块仅引入自身所需依赖，版本由父 POM 统一管理。

构建命令：
```bash
mvn clean package -pl <module> -am -DskipTests
```
其中 `-pl` 指定目标模块，`-am` 自动构建上游依赖，`-DskipTests` 跳过测试以加速流水线。

打包产物为 `target/<module>-exec.jar`，由 spring-boot-maven-plugin 生成，可直接 `java -jar` 运行。

## 4. 前端构建（Vite 双应用）

两个前端项目结构一致，均基于 Vite + Vue3 + TypeScript：

- **开发模式**：`npm run dev` / `dev:local` / `dev:test`，本地端口分别为 5173（业务前端）和 3060（支撑前端）。
- **生产构建**：`npm run build`，先执行 `vue-tsc -b` 类型检查，再调用 `vite build`。
- **输出目录**：分别输出到 `ele-ai-tender-web/` 和 `ele-ai-tender-support-web/`，并启用手动分包（element-plus、vue-vendor）。
- **代理转发**：开发时通过 Vite proxy 将 `/core-api`、`/ai-api`、`/file-api`、`/support-api` 转发至对应后端服务，同时记录请求日志到 `logs/vite-proxy.log`。

环境变量通过 `.env.*` 文件注入，如 `VITE_CORE_API_URL`、`VITE_AI_API_URL` 等，支持 localdev/test/production 多环境。

## 5. CI/CD 流水线（Jenkins）

仓库根目录包含 6 个 Jenkinsfile，按服务维度拆分：

| 流水线 | 作用 | 关键步骤 |
|--------|------|----------|
| `Jenkinsfile-core.groovy` | 后端 core 服务 | Checkout → Build(Maven) → Deploy(SSH) → Check(/actuator/health) |
| `Jenkinsfile-ai.groovy` | 后端 ai 服务 | 同上，内存参数 `-Xmx2G` |
| `Jenkinsfile-file.groovy` | 后端 file 服务 | 同上，内存参数 `-Xmx768M` |
| `Jenkinsfile-core-web.groovy` | 业务前端静态资源 | Init(npm ci) → Build(vite) → Dryrun(zip) → Deploy(Nginx html) |
| `Jenkinsfile-support-web.groovy` | 支撑前端静态资源 | 同上 |
| `Jenkinsfile-support.groovy` | 后端 support 服务 | 同后端流水线模式 |

通用流水线阶段：
1. **Checkout**：设置 TARGET_SERVER、APP_ENV、APP_DRIVE 等环境变量。
2. **Build**：后端走 Maven，前端走 npm；产物定位策略为扫描 `*-exec.jar` 或 `dist/` 目录。
3. **Deploy**：通过 `sshPublisher` 插件将产物推送到远程服务器的临时目录，然后执行外部批处理脚本完成进程重启/回滚。
4. **Check**（仅后端）：轮询 `/actuator/health` 接口，最多重试 5 次、间隔 30 秒。

部署脚本约定：
- 后端：`E:\java\process-jdk21.bat <app_name> <port> <drive> <repo> <bak_repo> <jar_name> "<jar_opts>"`
- 前端：`E:\20\web.bat <app_name> <drive> <html_repo> <bak_repo>`

## 6. 环境与产物约定

- **后端 JAR 命名**：`${module}-exec.jar`，由 spring-boot-maven-plugin 默认生成。
- **前端 base 路径**：生产模式下分别使用 `/ele-ai-tender-web/` 和 `/ele-ai-tender-support-web/`，需与 Nginx location 匹配。
- **健康检查端点**：所有后端服务暴露 `/actuator/health`，用于流水线健康探测。
- **日志**：Vite 开发代理请求写入 `ele-ai-tender-frontend/logs/vite-proxy.log`；后端使用 Logback/Spring Logging。

## 7. 开发者应遵循的规则

1. **新增后端模块**：在根 POM 的 `<modules>` 中注册，在 `<dependencyManagement>` 中声明版本，编写独立 `pom.xml` 并继承父 POM。
2. **新增前端应用**：复制任一前端项目模板，更新 `package.json` scripts、`vite.config.ts` 中的 base/outDir/proxy 及端口号，并为该应用创建对应的 Jenkinsfile。
3. **环境变量**：前端通过 `.env.*` 文件管理，后端通过 `application-{profile}.yml` 管理，禁止硬编码。
4. **流水线扩展**：新服务的 Jenkinsfile 应复用现有模板，保持 Checkout/Build/Deploy/Check 四阶段结构一致。
5. **健康检查**：后端服务必须暴露 `/actuator/health` 端点，否则流水线 Check 阶段会失败。
6. **构建缓存**：前端使用 `npm ci` + `npm install` 组合确保依赖锁定，后端使用 Maven 本地仓库缓存加速增量构建。
