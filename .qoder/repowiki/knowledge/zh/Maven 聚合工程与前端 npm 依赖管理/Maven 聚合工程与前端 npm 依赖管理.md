---
kind: dependency_management
name: Maven 聚合工程与前端 npm 依赖管理
category: dependency_management
scope:
    - '**'
source_files:
    - ele-ai-tender-system/pom.xml
    - ele-ai-tender-system/ele-ai-tender-core/pom.xml
    - ele-ai-tender-system/ele-ai-tender-ai/pom.xml
    - ele-ai-tender-system/ele-ai-tender-common/pom.xml
    - ele-ai-tender-frontend/package.json
    - ele-ai-tender-support-frontend/package.json
---

## 1. 使用的系统与工具

- **后端（Java）**：采用 Maven 多模块聚合工程，以 `ele-ai-tender-system/pom.xml` 为聚合根，统一管理 Spring Boot 3.2.x、MyBatis Plus、Spring AI、Milvus SDK、POI-TL、Hutool、JWT、Redisson 等第三方依赖版本。
- **前端（Vue 3 + Vite）**：两个独立的前端工程 `ele-ai-tender-frontend` 与 `ele-ai-tender-support-frontend`，各自通过 `package.json` 声明 Vue、Element Plus、Axios、Pinia、Vite、TypeScript 等依赖，并使用 `package-lock.json` 锁定版本。

## 2. 关键文件与包

- 后端聚合 POM：`ele-ai-tender-system/pom.xml`
- 核心业务模块 POM：`ele-ai-tender-system/ele-ai-tender-core/pom.xml`
- AI 服务模块 POM：`ele-ai-tender-system/ele-ai-tender-ai/pom.xml`
- 公共模块 POM：`ele-ai-tender-system/ele-ai-tender-common/pom.xml`
- 主前端依赖清单：`ele-ai-tender-frontend/package.json`
- 支撑前端依赖清单：`ele-ai-tender-support-frontend/package.json`

## 3. 架构与约定

### 后端 Maven 聚合工程

- **统一版本管理**：聚合 POM 的 `<properties>` 集中定义所有第三方库版本号（如 `spring-boot.version=3.2.2`、`mybatis-plus.version=3.5.5`、`spring-ai.version=1.1.0` 等），子模块仅引用 artifactId，不写 version。
- **dependencyManagement 收敛**：在聚合 POM 的 `<dependencyManagement>` 中引入 `spring-boot-dependencies` BOM，并显式声明内部模块、MyBatis Plus、JJWT、Hutool、Lombok、SpringDoc、Apache Commons、POI-TL、Flexmark、jsoup、MySQL 驱动、Redisson 等依赖的版本，确保全仓库一致。
- **子模块最小化声明**：各业务模块（core、ai、file、support、common）只声明所需依赖的 groupId/artifactId，由父 POM 解析版本；对运行时才需要的依赖使用 `<scope>runtime</scope>`（如 MySQL 驱动），对编译期注解处理器使用 `<scope>provided</scope>`（如 Lombok）。
- **内部模块分层**：`common` 提供通用 DTO、枚举、工具类、安全组件；`interaction` 拆分为 common-interaction、interaction-core、autoconfigure、spring-boot-starter 四个子模块，通过 SPI 暴露交互能力；其他模块（core、ai、file、support）均依赖 common 与 interaction 相关模块。
- **构建插件统一**：聚合 POM 的 `<pluginManagement>` 统一 spring-boot-maven-plugin、maven-compiler-plugin、maven-surefire-plugin 版本与配置，子模块按需复用。

### 前端 npm 依赖管理

- **双前端并列**：主业务前端与管理后台前端各自维护独立的 `package.json`，共享相同的 Vue 3、Element Plus、Axios、Pinia、Sass、Vite、TypeScript 技术栈，但作为独立项目分别安装与构建。
- **依赖锁定**：每个前端目录包含 `package-lock.json`，确保团队成员与 CI 环境安装完全一致的依赖树。
- **脚本与环境**：通过 `dev`、`build`、`preview` 等标准脚本组织开发流程，并通过 `--mode localdev/test` 切换不同环境变量（`.env.*`）。

## 4. 开发者应遵循的规则

- **新增后端依赖时**：先在聚合 POM 的 `<properties>` 中定义版本号，再在 `<dependencyManagement>` 中声明该依赖，最后在各子模块中以无版本方式引用。禁止在子模块中直接写死版本。
- **控制传递依赖**：对仅用于编译期注解处理的依赖（如 Lombok）使用 `<scope>provided</scope>`；对仅在运行时需要 JAR 的依赖（如 MySQL 驱动）使用 `<scope>runtime</scope>`，避免打包冗余。
- **保持 Spring AI 生态一致性**：所有 Spring AI 相关 starter（openai、zhipuai、tika-document-reader）必须使用统一的 `${spring-ai.version}`，避免多版本冲突。
- **前端新增依赖时**：在对应前端的 `package.json` 中声明，提交后同步更新 `package-lock.json`，确保团队与 CI 拉取到相同版本。
- **内部模块依赖方向**：业务模块只能向上依赖 `common` 与 `interaction` 相关模块，禁止跨层反向依赖，维持清晰的模块边界。
- **CI 构建约束**：Jenkinsfile 流水线按模块拆分构建，新增模块时需同步添加对应的 Jenkinsfile，确保依赖变更能在 CI 中被验证。
