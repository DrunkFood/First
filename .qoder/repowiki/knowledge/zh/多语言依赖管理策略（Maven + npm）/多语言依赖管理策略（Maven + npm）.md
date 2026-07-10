---
kind: dependency_management
name: 多语言依赖管理策略（Maven + npm）
slug: dependency_management
category: dependency_management
scope:
    - '**'
---

本仓库采用前后端分离的多语言架构，分别使用 Maven 和 npm 进行依赖管理，并通过聚合根 POM 与 lockfile 实现版本集中管控。

## 后端：Maven 多模块聚合
- 聚合根 ele-ai-tender-system/pom.xml 作为父 POM，声明所有子模块（common、core、ai、file、support、interaction 等），并通过 dependencyManagement 统一收敛第三方库版本。
- 版本集中化：Spring Boot 3.2.2、MyBatis Plus 3.5.5、JJWT 0.12.5、Hutool 5.8.25、Redisson 3.27.2、SpringDoc 2.3.0、POI-TL 1.12.2、Flexmark 0.64.0 等关键依赖均在父 POM 的 properties 中统一定义，子模块仅引用版本号变量而不重复声明版本。
- 内部模块依赖：通过 com.jy.eleaitender groupId 下的 artifactId 引用，如 core 模块依赖 common、interaction-core 等，形成清晰的领域分层。
- 构建插件：spring-boot-maven-plugin 负责打包可执行 jar；maven-compiler-plugin 配置 Lombok annotation processor；maven-surefire-plugin 用于单元测试。
- 无私有仓库配置：未发现 .m2/settings.xml 或 pom 中的 repositories 自定义镜像，默认使用 Maven Central。

## 前端：npm + package-lock.json
- 双前端应用：ele-ai-tender-frontend 与 ele-ai-tender-support-frontend 各自维护独立的 package.json，基于 Vue3 + Vite + Element Plus 技术栈。
- 版本锁定：两个前端均生成 package-lock.json，确保团队与 CI 环境安装一致的依赖树。
- 共享依赖对齐：两个前端在 vue、element-plus、axios、pinia、sass、jsencrypt 等核心库上保持相同版本范围，便于后续抽取公共包。
- 开发工具链：TypeScript ~5.9.3、Vite 7.x、vue-tsc 3.x、@vitejs/plugin-vue 6.x，均通过 devDependencies 管理。
- 无私有 npm 源配置：未发现 .npmrc 文件，默认使用 npmjs.org 官方源。

## 构建流水线集成
- Jenkins 流水线脚本（Jenkinsfile-*.groovy）覆盖各模块独立构建，CI 环境中依赖下载由 Maven/npm 默认源完成。
- 前端构建产物输出至 dist/ele-ai-tender-web 与 dist/ele-ai-tender-support-web 目录，供静态资源服务部署。

## 开发者约定
- 新增后端依赖时，优先在父 POM 的 properties 中声明版本，并在 dependencyManagement 中注册，子模块仅引入 artifactId。
- 前端升级依赖需同步更新两个 package.json 对应条目，并重新生成 package-lock.json 提交到版本库。
- 不直接修改 node_modules 或 target 目录，这些已在 .gitignore 中排除。