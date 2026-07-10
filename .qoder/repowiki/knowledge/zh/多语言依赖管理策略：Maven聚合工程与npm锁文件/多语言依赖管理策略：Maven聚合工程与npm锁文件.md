---
kind: dependency_management
name: 多语言依赖管理策略：Maven聚合工程与npm锁文件
category: dependency_management
scope:
    - '**'
source_files:
    - ele-ai-tender-system/pom.xml
    - ele-ai-tender-system/ele-ai-tender-core/pom.xml
    - ele-ai-tender-system/ele-ai-tender-common/pom.xml
    - ele-ai-tender-frontend/package.json
    - ele-ai-tender-support-frontend/package.json
    - ele-ai-tender-frontend/package-lock.json
    - Jenkinsfile-core.groovy
---

## 依赖管理系统概述

该项目采用双栈依赖管理策略，后端使用Maven聚合工程，前端使用npm包管理器，通过不同的工具链实现跨语言的依赖版本控制。

## 后端依赖管理（Maven）

### 核心架构
- 聚合工程结构：根目录 ele-ai-tender-system/pom.xml 作为父POM，统一管理6个子模块
- 版本集中管理：通过 properties 标签定义所有第三方库版本，包括Spring Boot 3.2.2、MyBatis Plus 3.5.5、Hutool 5.8.25等
- 依赖治理：在 dependencyManagement 中声明所有依赖版本，子模块仅引用不指定版本

### 关键特性
- Spring Boot BOM导入：通过 spring-boot-dependencies POM类型导入统一管理Spring生态版本
- 内部模块版本同步：所有内部模块统一使用 ${project.version} (1.0.0-SNAPSHOT)
- 模块化依赖隔离：common模块使用 provided scope避免重复打包
- AI能力集成：集成Spring AI 1.1.0、Milvus SDK 2.3.3等AI相关依赖

### 构建配置
- JDK 21支持：明确指定Java版本和UTF-8编码
- Lombok注解处理：在编译器插件中配置Lombok注解处理器路径
- 测试框架：使用Maven Surefire Plugin 3.2.5执行单元测试

## 前端依赖管理（npm）

### 双前端架构
- 业务前端 (ele-ai-tender-frontend)：包含Vue 3.5.25、Element Plus 2.13.2、TipTap编辑器等
- 支撑前端 (ele-ai-tender-support-frontend)：精简版前端，共享基础依赖
- 锁定文件：使用 package-lock.json 确保依赖版本一致性

### 技术栈统一
- Vue生态：Vue 3.5.25 + Vue Router 4.6.4 + Pinia 3.0.4
- UI组件库：Element Plus 2.13.2 + @element-plus/icons-vue 2.3.2
- 开发工具：Vite 7.3.2 + TypeScript ~5.9.3 + Vue TSC 3.1.5
- 文档处理：docx-preview 0.3.7用于Word文档预览

## CI/CD集成

### Jenkins流水线
- 工具链管理：通过 tools 块声明Maven和JDK21环境
- 增量构建：使用 -pl ${APP_NAME} -am 参数实现按需构建
- 健康检查：构建后自动调用 /actuator/health 端点验证服务状态

## 依赖管理最佳实践

### 版本控制策略
- 语义化版本：第三方库使用精确版本号或兼容范围
- 向后兼容：前端依赖使用 ^ 前缀允许小版本升级
- 锁定文件：前端提交 package-lock.json 确保构建可重现性

### 安全考虑
- 私有仓库：未发现私有Maven仓库配置，依赖从公共源下载
- 依赖审计：未集成依赖漏洞扫描工具
- 最小权限：前端依赖按功能模块拆分，减少攻击面

### 维护建议
- 定期更新：建立依赖更新策略，定期评估新版本安全性
- 兼容性测试：大版本升级前进行完整的回归测试
- 文档化：记录关键依赖的升级影响和迁移步骤