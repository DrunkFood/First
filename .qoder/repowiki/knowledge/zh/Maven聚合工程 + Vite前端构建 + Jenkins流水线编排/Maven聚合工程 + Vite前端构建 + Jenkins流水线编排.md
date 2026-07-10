---
kind: build_system
name: Maven聚合工程 + Vite前端构建 + Jenkins流水线编排
category: build_system
scope:
    - '**'
source_files:
    - ele-ai-tender-system/pom.xml
    - ele-ai-tender-system/ele-ai-tender-core/pom.xml
    - ele-ai-tender-frontend/package.json
    - ele-ai-tender-support-frontend/package.json
    - ele-ai-tender-frontend/vite.config.ts
    - ele-ai-tender-support-frontend/vite.config.ts
    - Jenkinsfile-core.groovy
    - Jenkinsfile-ai.groovy
    - Jenkinsfile-support-web.groovy
---

## 构建系统概述

该项目采用**多语言混合构建体系**：后端基于 Maven 聚合工程，前端使用 Vite + Vue3 独立构建，通过 Jenkinsfile 流水线统一编排部署。

## 后端构建系统（Maven）

### 聚合工程结构
- **根 POM**: `ele-ai-tender-system/pom.xml` 作为聚合父工程，统一管理版本和依赖
- **模块划分**: common → interaction → support/file/core/ai 六个子模块
- **Java 21 + Spring Boot 3.2.2**: 通过 properties 集中管理所有依赖版本

### 构建配置特点
- **Spring Boot Maven Plugin**: 每个可执行模块都配置了 repackage 目标，生成 `-exec.jar` 可执行包
- **依赖管理**: 使用 `<dependencyManagement>` 统一声明内部模块和第三方依赖版本
- **编译配置**: maven-compiler-plugin 启用参数名称保留、Lombok 注解处理器
- **测试插件**: maven-surefire-plugin 3.2.5 用于单元测试

### 模块化依赖关系
```
common (基础公共) ← interaction (SPI接口) ← core/support/file/ai (业务服务)
```

## 前端构建系统（Vite）

### 双前端架构
- **ele-ai-tender-frontend**: 主业务前端，Vue3 + TypeScript + Element Plus
- **ele-ai-tender-support-frontend**: 支撑管理前端，独立部署

### 构建脚本约定
- `npm run dev`: 开发模式启动
- `npm run build`: 生产构建（先 vue-tsc 类型检查，再 vite build）
- `npm run build:test`: 测试环境构建
- `npm run preview`: 本地预览构建产物

### Vite 配置策略
- **API 代理**: 开发时通过 proxy 转发到不同后端服务（core-api/ai-api/file-api/support-api）
- **代码分割**: element-plus 和 vue-vendor 单独分包，优化加载性能
- **环境变量**: 通过 `.env.*` 文件区分不同环境 API 地址
- **输出目录**: 分别输出到 `ele-ai-tender-web/` 和 `ele-ai-tender-support-web/`

## CI/CD 流水线（Jenkins）

### 后端流水线模式
每个后端服务都有独立的 Jenkinsfile：
- `Jenkinsfile-core.groovy`: 核心业务服务
- `Jenkinsfile-ai.groovy`: AI 能力服务  
- `Jenkinsfile-file.groovy`: 文件处理服务
- `Jenkinsfile-support.groovy`: 支撑管理服务

### 流水线标准流程
1. **Checkout**: 设置目标服务器和环境变量
2. **Build**: `mvn clean package -pl ${APP_NAME} -am -DskipTests` 增量构建
3. **Deploy**: SSH 传输 JAR 包并调用 `process-jdk21.bat` 重启服务
4. **Check**: 轮询 `/actuator/health` 健康检查，最多重试5次

### 前端流水线模式
- `Jenkinsfile-support-web.groovy`: 支撑前端构建部署
- 流程：`npm ci` → `npm run build` → 7z 压缩 → SSH 部署到 Nginx

### 部署策略
- **Windows 环境**: 所有构建和部署在 Windows 服务器执行
- **进程管理**: 通过批处理脚本实现服务的启停和备份
- **健康检查**: Spring Boot Actuator 提供 `/actuator/health` 端点
- **灰度发布**: 支持通过端口号区分不同实例

## 构建规范与约束

### 版本管理
- 后端统一使用 `1.0.0-SNAPSHOT` 快照版本
- 前端使用 `0.0.0` 占位版本，由 CI 动态替换
- 所有第三方依赖版本集中在父 POM 的 properties 中管理

### 构建产物
- 后端: `target/*-exec.jar` 可执行 JAR 包
- 前端: 静态资源目录 `dist/ele-ai-tender-web/` 或 `ele-ai-tender-support-web/`

### 环境配置
- 后端: `application-{dev,test,wuyx}.yml` 多环境配置
- 前端: `.env.development`, `.env.production`, `.env.test` 等环境文件
- 构建时通过 `--mode` 参数切换不同环境配置

### 质量保障
- 构建前自动进行 TypeScript 类型检查（vue-tsc）
- 跳过测试以加速 CI 流程（`-DskipTests`）
- 构建日志详细记录每个阶段状态