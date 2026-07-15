---
kind: dependency_management
name: Maven 聚合工程与多前端依赖统一治理
category: dependency_management
scope:
    - '**'
source_files:
    - ele-ai-tender-system/pom.xml
    - ele-ai-tender-system/ele-ai-tender-common/pom.xml
    - ele-ai-tender-system/ele-ai-tender-core/pom.xml
    - ele-ai-tender-system/ele-ai-tender-ai/pom.xml
    - ele-ai-tender-frontend/package.json
    - ele-ai-tender-frontend/package-lock.json
    - ele-ai-tender-support-frontend/package.json
    - ele-ai-tender-support-frontend/package-lock.json
---

## 1. 使用的系统与方法
- 后端：基于 Maven 聚合工程，以 ele-ai-tender-system 为根 POM，通过 dependencyManagement + properties 集中声明第三方库版本，子模块仅声明 groupId/artifactId，不写版本号。
- 前端：两个独立 Vite+Vue3 应用（ele-ai-tender-frontend、ele-ai-tender-support-frontend），各自维护 package.json + package-lock.json，使用 npm/yarn 包管理器锁定依赖。
- 无 vendoring 策略，所有依赖均从远程仓库拉取；未发现私有 Maven/NPM 仓库或镜像配置。

## 2. 关键文件与位置
- 后端聚合与版本中心：
  - ele-ai-tender-system/pom.xml — 聚合根，定义 modules、properties 全局版本、dependencyManagement 统一版本管理、pluginManagement 插件版本。
  - 各业务模块 POM：ele-ai-tender-common/pom.xml、ele-ai-tender-core/pom.xml、ele-ai-tender-ai/pom.xml、ele-ai-tender-file/pom.xml、ele-ai-tender-support/pom.xml、ele-ai-tender-interaction/.../pom.xml。
- 前端依赖清单：
  - ele-ai-tender-frontend/package.json、ele-ai-tender-frontend/package-lock.json
  - ele-ai-tender-support-frontend/package.json、ele-ai-tender-support-frontend/package-lock.json

## 3. 架构与约定
### 3.1 Maven 聚合与分层
- 根 POM 的 modules 按能力划分：common → interaction SPI → support/file/core/ai 四个业务服务，形成公共层加领域服务的分层结构。
- 所有第三方依赖在根 POM 的 properties 中集中声明（如 spring-boot.version=3.2.2、mybatis-plus.version=3.5.5、spring-ai.version=1.1.0、redisson.version=3.27.2 等），并在 dependencyManagement 中以 version 引用，子模块引用时省略 version。
- 对可传递依赖采用 scope=provided 控制打包范围（如 common 模块中的 spring-boot-starter-web、spring-boot-starter-aop、lombok 标记为 provided，避免重复打包）。
- 运行时依赖显式标注 scope=runtime（如 mysql-connector-j），编译期注解处理器通过 maven-compiler-plugin.annotationProcessorPaths 注入 Lombok。
- Spring Boot Starter 模式：interaction 模块拆分为 common-interaction / interaction-core / interaction-autoconfigure / interaction-spring-boot-starter，通过 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports 实现自动装配。

### 3.2 前端依赖治理
- 双前端共享相同技术栈（Vue3 + Element Plus + Pinia + Axios + Sass），但作为独立项目分别维护依赖，未使用 Lerna/Yarn Workspaces 做跨包复用。
- 生产依赖使用 ^ 主版本兼容，开发依赖使用 ~ 精确小版本，配合 package-lock.json 保证构建可重现。
- 通过 browserslist 字段约束目标浏览器，与 Vite 构建产物体积控制相关。

## 4. 开发者应遵循的规则
1. 新增或升级第三方依赖必须改根 POM：在 ele-ai-tender-system/pom.xml 的 properties 中声明新版本，在 dependencyManagement 中统一管理，子模块只写 artifactId，禁止自行指定版本。
2. 合理使用 scope：仅被其他模块引用的 starter/web/aop/lombok 等在公共模块中标记 provided；数据库驱动、外部 SDK 等运行时依赖标记 runtime。
3. 内部模块依赖通过聚合管理：子模块间依赖通过父 POM 的 dependencyManagement 引入，保持 com.jy.eleaitender:*:${project.version} 一致。
4. Spring AI 生态集中管理：所有 group.springframework.ai 下的 starter（OpenAI、智谱、Tika 文档解析）统一使用 ${spring-ai.version}，避免版本漂移。
5. 前端依赖更新需同步双端：若涉及 Vue/Element Plus/Axios 等通用库升级，建议同时更新两个 package.json 并重新生成 lock 文件，确保行为一致。
6. 不使用本地 vendoring：所有依赖来自远程仓库，CI 环境需保证网络可达；如需私有仓库，应在 CI 或用户 .m2/settings.xml 中配置而非写入仓库。