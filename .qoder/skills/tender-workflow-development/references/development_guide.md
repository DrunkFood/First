# 开发规范 (Development Guide)

> 基于项目实际代码和 CLAUDE.md 规范整理的高频开发要点

## 编码规范

### 后端

- **返回结构**: `Result.success(data)` / `Result.fail(code, msg)`；交互接口用 `InteractionResult<T>`
- **实体**: 所有实体继承 `BaseEntity`（自动填充 create/modify 时间、ver、is_delete）
- **权限**: `@RequireLogin` / `@RequirePermission("xxx")`
- **命名**: Service 接口 `I*Service`；DB 表前缀 `tb_`(核心) / `ai_`(AI服务) / `sup_`(支撑) / `file_`(文件)
- **依赖**: 新依赖版本声明在父 POM `<dependencyManagement>`
- **安全**: 签名用 `SignatureUtil`(HMAC-SHA256)、密码用 `PasswordUtil`(BCrypt)、字符集必须显式 UTF-8
- **AI接口**: 流式响应用 `SseEmitter`；AI生成内容需人工审核机制

### 前端

- **框架**: Vue 3 `<script setup>` + TypeScript
- **状态管理**: Pinia
- **API调用**: 统一放 `src/api/`
- **代理规则**: `/core-api/*` → :8082, `/ai-api/*` → :8083, `/file-api/*` → :8081, `/support-api/*` → :8080

## 跨模块调用规则

| 调用方 → 被调用方 | 调用方式 | 说明 |
|------------------|---------|------|
| core → ai | 通过 `ai_task` 表解耦 | **不直接HTTP调用**，core写入任务 → ai轮询执行 → core同步结果 |
| core → file | 直接HTTP | 文件上传/下载/文档生成/文档修复 |
| core → support | 直接HTTP | 认证/权限校验 |
| ai → file | 直接HTTP | 知识库文件管理 |
| ai → support | 直接HTTP | 查询模型配置和路由规则 |
| 前端 → ai | SSE直连 | AI对话/文本优化（其他走Core转发） |

## 常见陷阱

### 1. common 模块变更后必须 install

修改了 `ele-ai-tender-common` 中的实体、枚举、工具类等，必须先执行：
```bash
mvn install -pl ele-ai-tender-common -AM
```
否则依赖它的 core/ai/support 模块会编译报"找不到符号"。仅 install 不够，还需对各依赖模块执行 `mvn clean compile`。

### 2. 后端启动必须在子模块目录

不能在父POM目录用 `-pl` 启动（会因父POM无main类而报错）：
```bash
# 正确
cd ele-ai-tender-system/ele-ai-tender-core && mvn spring-boot:run

# 错误
cd ele-ai-tender-system && mvn spring-boot:run -pl ele-ai-tender-core
```

### 3. 需求侧无状态机

`TbRequirement.status` 仅有 `IN_PROGRESS` 和 `COMPLETED` 两种值，通过 `update()` 接口自由设置，**无状态机校验**。前端定义的5种状态(DRAFT/GENERATING/PENDING_REVIEW/APPROVED/REJECTED)后端不识别。

### 4. 阶段只能顺序推进

`PhaseFlowController.isValidTransition()` 校验 `target.code == current.code + 1`：
- 不可跳跃（不能从阶段1直接到阶段3）
- 不可倒退（不能从阶段3回到阶段2）
- 推进前检查 `hasActiveTasks()`，有活跃AI任务时禁止推进

### 5. 检测内容不存文件ID

`tb_detection_record.content_file_id` **始终为 null**，检测内容存 `content_snapshot` 字段。不要尝试设置 contentFileId。

### 6. 文档集成不是Markdown转Word

文档集成是通过 **poi-tl 模板引擎** 填充 Word 模板，不是 Markdown → Word 转换。需要项目模板有 Word 模板文件（`TbProjectTemplate.fileId` 不为空）。

### 7. retry 重置所有检测记录

`DetectionServiceImpl.retry()` 会重置**所有**检测记录（不仅是失败的），重新检测全部4项。

### 8. CAS抢占顺序

`AiTaskProcessor` 中必须**先获取并发许可再CAS**：
```java
// 正确顺序
if (!concurrencyManager.tryAcquire(userId)) continue;
int updated = aiTaskMapper.casUpdateStatus(...);
if (updated == 0) {
    concurrencyManager.release(userId); // CAS失败必须归还许可
    continue;
}
```

### 9. mybatis-plus 表前缀

core模块的 `mybatis-plus.table-prefix` 配置为 `ai_`，但核心业务实体(TbProject等)通过 `@TableName` 显式指定 `tb_` 前缀，不受默认前缀影响。

### 10. Interaction 模块 JDK 8 兼容

`ele-ai-tender-interaction` 是 JDK 8 兼容的，公开 API 保持 JDK 8 兼容；协议 DTO 只放在 `ele-ai-tender-common-interaction`。

## 重启服务可靠流程

```bash
# 1. kill 全部 Java 进程
# 2. 重新 install common
cd ele-ai-tender-system && mvn install -pl ele-ai-tender-common -AM
# 3. 各模块 clean compile
mvn clean compile -pl ele-ai-tender-core,ele-ai-tender-ai,ele-ai-tender-support,ele-ai-tender-file
# 4. 按顺序启动（support/file 可并行，core/ai 可并行）
cd ele-ai-tender-support && mvn spring-boot:run  # :8080
cd ele-ai-tender-file    && mvn spring-boot:run  # :8081
cd ele-ai-tender-core    && mvn spring-boot:run  # :8082
cd ele-ai-tender-ai      && mvn spring-boot:run  # :8083
```

## 提交前检查

- 前端: `npm run build`
- 后端: `mvn clean test`

## 规范文档索引

| 规范 | 文件 | 加载时机 |
|------|------|----------|
| 编码规范 | `docs/rules/CODE_CONVENTIONS.md` | 写代码时参考 |
| 前端编码规范 | `docs/rules/FRONTEND_CONVENTIONS.md` | 前端开发时参考 |
| 核心业务模块规范 | `docs/rules/CORE_MODULE_SPEC.md` | 开发 core 模块功能时参考 |
| AI服务模块规范 | `docs/rules/AI_MODULE_SPEC.md` | 开发 ai 模块功能时参考 |
| 检测全链路规范 | `docs/rules/DETECTION_FLOW_SPEC.md` | 开发检测相关功能时参考 |
| 阶段流程控制器规范 | `docs/rules/PHASE_FLOW_SPEC.md` | 开发编制阶段流转时参考 |
| 文件服务规范 | `docs/rules/FILE_SERVICE_SPEC.md` | 开发文件相关功能时参考 |

> 代码实现 > `docs/rules/` > 其他文档，三者冲突时以代码为准。
