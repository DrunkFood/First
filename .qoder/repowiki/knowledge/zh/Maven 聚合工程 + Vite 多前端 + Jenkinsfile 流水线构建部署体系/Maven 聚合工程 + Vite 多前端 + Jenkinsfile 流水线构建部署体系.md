---
kind: build_system
name: Maven 聚合工程 + Vite 多前端 + Jenkinsfile 流水线构建部署体系
category: build_system
scope:
    - '**'
source_files:
    - ele-ai-tender-system/pom.xml
    - ele-ai-tender-system/ele-ai-tender-core/pom.xml
    - ele-ai-tender-frontend/package.json
    - ele-ai-tender-frontend/vite.config.ts
    - ele-ai-tender-support-frontend/package.json
    - Jenkinsfile-core.groovy
    - Jenkinsfile-ai.groovy
    - Jenkinsfile-core-web.groovy
    - Jenkinsfile-support-web.groovy
---

## 构建系统概览

本项目采用后端 Maven 聚合工程加双前端独立 Vite 项目加 Jenkinsfile 流水线的混合构建架构，通过统一的 CI 流程编排所有服务的构建、打包与部署。

### 1. 后端构建：Maven 聚合工程

聚合根位于 ele-ai-tender-system/pom.xml，使用 packaging pom 管理子模块。JDK 版本为 Java 21，Spring Boot 3.2.2，通过 spring-boot-dependencies BOM 统一管理依赖版本。

核心模块划分如下：
- ele-ai-tender-common：公共依赖、工具类、实体定义
- ele-ai-tender-interaction：前后端交互 SPI 抽象层（含 common、core、autoconfigure、starter 四个子模块）
- ele-ai-tender-core：核心业务服务（端口 28082）
- ele-ai-tender-ai：AI 能力服务（端口 28083）
- ele-ai-tender-file：文件处理服务（端口 28081）
- ele-ai-tender-support：支撑管理服务（端口 28080）

依赖管理策略是在父 POM 的 dependencyManagement 中集中声明所有第三方库版本，子模块仅声明 groupId 和 artifactId，避免版本冲突。每个服务模块通过 spring-boot-maven-plugin 配置 classifier exec，生成 *-exec.jar 可执行包。编译配置统一启用 Lombok 注解处理器、UTF-8 编码、参数名保留。

### 2. 前端构建：Vite 加 Vue 3 双前端

两个前端项目结构完全一致，均基于 Vite 7.x、Vue 3.5 和 TypeScript 5.9。

ele-ai-tender-frontend 是招标文件编制主前端，输出到 ele-ai-tender-web 目录。ele-ai-tender-support-frontend 是系统支撑管理前端，输出到 ele-ai-tender-support-web 目录。

构建脚本约定如下：
- npm run dev：开发模式（端口 5173）
- npm run build：生产构建（TypeScript 类型检查加 Vite 打包）
- npm run build:test：测试环境构建（--mode test）
- npm run preview：本地预览构建产物

Vite 构建优化包括手动分包，将 element-plus、vue/vue-router/pinia 抽离为独立 chunk。开发时通过 /core-api、/ai-api、/file-api、/support-api 前缀代理到不同后端服务。环境变量通过 .env.development、.env.production、.env.test 等文件区分环境配置。

### 3. CI/CD 流水线：Jenkinsfile 统一编排

仓库根目录提供 6 个独立的 Jenkinsfile，每个对应一个服务的前后端组合。

Jenkinsfile-core.groovy 构建 core 服务（28082），使用 Maven 和 JDK21，通过 SSH 推送 JAR 并调用进程脚本启动。
Jenkinsfile-ai.groovy 构建 ai 服务（28083），同样使用 Maven 和 JDK21，部署方式相同。
Jenkinsfile-file.groovy 构建 file 服务（28081），部署方式相同。
Jenkinsfile-support.groovy 构建 support 服务（28080），部署方式相同。
Jenkinsfile-core-web.groovy 构建主前端静态资源，使用 NodeJS 22.21.1，通过 7-Zip 压缩后部署到 Nginx 目录替换。
Jenkinsfile-support-web.groovy 构建支撑前端静态资源，使用 NodeJS 22.21.1，部署方式相同。

流水线通用阶段包括 Checkout、Init 或 Build、Deploy 和 Check。Checkout 阶段解析目标服务器 IP、分支名、环境标识。Build 阶段后端执行 mvn clean package -pl <module> -am -DskipTests，前端执行 npm ci 和 npm run build。Deploy 阶段通过 sshPublisher 插件传输构建产物到远程服务器。Check 阶段调用 /actuator/health 健康检查接口，最多重试 5 次，间隔 30 秒。

部署机制方面，后端通过 process-jdk21.bat 脚本启动 JAR，支持热重启和备份回滚。前端通过 web.bat 脚本解压 zip 到 Nginx 的 html/<app-name>/ 目录，实现零停机发布。

### 4. 关键设计决策

当前直接部署 JVM 进程和静态文件，未引入容器化方案。Jenkins 运行在 Windows 节点上，路径均为 E:\java\...，批处理脚本用于进程管理。每个服务独立端口、独立流水线，便于灰度发布和故障隔离。ele-ai-tender-interaction 模块通过 SPI 定义前后端契约，使前端不感知后端具体实现。后端通过 Spring Profile（dev/test/wuyx），前端通过 Vite mode 和环境变量文件进行环境配置分层。

### 开发者须知

新增后端服务需在聚合 POM 注册模块，编写独立 Jenkinsfile，遵循现有端口分配规则。新增前端应用需复制任一前端模板，修改 vite.config.ts 中的输出目录和代理配置。依赖升级应统一在父 POM 的 properties 中修改版本号，避免各模块版本不一致。本地调试可通过 IDEA 直接运行各模块 Application，前端通过 npm run dev 配合 Vite 代理联调。