# EleTender 编码规范

本文档为 EleTender 项目的统一编码规范，覆盖数据库、接口、代码组织、异常处理与安全等约束。

> 裁定顺序：当前代码实现 > 本文档 > 历史设计讨论

---

## 1. 数据库规范

### 1.1 表命名

- 使用 `snake_case`，按模块加前缀：

| 模块     | 前缀 | 示例                                        |
|--------|------|-------------------------------------------|
| 支撑中心   | `sup_` | `sup_user`、`sup_access_log`、`sup_message` |
| 文件服务   | `file_` | `file_info`                               |
| 招标文件编制 | `td_` | `td_project`、`td_requirement`             |
| AI服务   | `ai_` | `ai_task`、`ai_response_log`、`ai_model_config`  |

- 新模块表前缀需先在 `PROJECT_SPEC_FINAL.md` 中登记

### 1.2 基础字段(BaseEntity)

所有业务实体类必须继承 `BaseEntity`，由 `MetaObjectHandlerImpl` 自动填充：

| Java 字段 | 数据库列 | 类型 | 填充时机 | 说明 |
|-----------|----------|------|----------|------|
| `id` | `id` | `BIGINT` | 手动/自增 | 主键，`IdType.AUTO`(自增) |
| `createTime` | `create_time` | `DATETIME` | INSERT | 创建时间 |
| `createId` | `create_id` | `BIGINT` | INSERT | 创建人 ID(未登录时填 0) |
| `createName` | `create_name` | `VARCHAR` | INSERT | 创建人姓名(未登录时填 `system`) |
| `modifyTime` | `modify_time` | `DATETIME` | INSERT + UPDATE | 最后修改时间 |
| `modifyId` | `modify_id` | `BIGINT` | INSERT + UPDATE | 最后修改人 ID |
| `modifyName` | `modify_name` | `VARCHAR` | INSERT + UPDATE | 最后修改人姓名 |
| `ver` | `ver` | `INT` | INSERT(初始为 1) | 乐观锁版本号，`@Version` |
| `isDelete` | `is_delete` | `TINYINT` | INSERT(初始为 0) | 逻辑删除，`@TableLogic`;0=未删除,1=已删除 |

```java
// 实体示例
public class SomeEntity extends BaseEntity {
    // 只需声明业务字段，基础字段由 BaseEntity 提供
    private String name;
}
```

**注意**：
- 乐观锁更新时 MyBatis-Plus 会自动将 `ver+1`，失败抛 `OptimisticLockException`
- 逻辑删除查询时 MyBatis-Plus 自动追加 `AND is_delete = 0`
- `createId`/`createName` 只在 INSERT 时填充，UPDATE 不覆盖

### 1.3 字段命名

- 数据库列：`snake_case`（如 `bid_record_id`）
- Java 实体字段：`camelCase`（如 `bidRecordId`），MyBatis-Plus 自动映射

### 1.4 主键策略

- 默认：`IdType.AUTO`（MySQL 自增）
- 有特殊需要时可改为雪花算法（`IdType.ASSIGN_ID`），需在字段上显式指定

### 1.5 初始化 SQL

- 每个模块的建表 DDL 放在 `sql/init.sql`

## 2. 接口规范

### 2.1 URL 风格

- 路径小写，单词用 `-` 连接（`kebab-case`）
- 资源用复数名词：`/users`、`/roles`
- 层级资源：`/roles/{id}/menus`
- 动作型操作用动词后缀：`/users/{id}/reset-password`、`/versions/{id}/publish`

```
GET    /api/users              # 分页查询
GET    /api/users/{id}         # 查单条
POST   /api/users              # 创建
PUT    /api/users/{id}         # 全量更新
PATCH  /api/users/{id}         # 部分更新（少用）
DELETE /api/users/{id}         # 删除
POST   /api/users/{id}/disable # 动作操作
```

### 2.2 HTTP 方法语义

| 方法 | 语义 | 幂等 |
|------|------|------|
| GET | 查询，不修改状态 | 是 |
| POST | 创建 / 触发动作 | 否 |
| PUT | 全量更新 | 是 |
| DELETE | 删除（逻辑删除） | 是 |

### 2.3 统一返回结构

**内部接口**（HTTP 状态码固定 200，业务状态通过 `code` 字段区分）：

```java
// 结构：{ code, message, data, timestamp }
Result.success(data)              // code=200
Result.success()                  // code=200, data=null
Result.fail("错误信息")            // code=500
Result.fail(code, "错误信息")      // 自定义业务错误码
Result.fail(ResponseCode.xxx)     // 使用枚举
```

**交互协议接口**（业务系统接入，使用 `InteractionResult<T>`，结构相同）：

```java
InteractionResult.success(data)
InteractionResult.fail("错误信息")
```

> 交互接口 **不使用** `Result<T>`，避免两套返回结构混用。

### 2.4 分页规范

**请求**：继承 `BasePageRequest`，字段 `current`（默认 1）和 `size`（默认 10，最大 100）

```java
public class UserQueryRequest extends BasePageRequest {
    private String username;
    private Integer status;
}
```

**响应**：直接返回 MyBatis-Plus 的 `Page<T>`，包含 `records`、`total`、`current`、`size`

```java
Result<Page<UserVO>> result = Result.success(page);
```

### 2.5 错误码分配

| 范围 | 归属 |
|------|------|
| 200 | 成功 |
| 400–405 | 通用参数/HTTP 错误 |
| 500 | 通用失败 |
| 1001–1999 | 用户认证相关 |
| 2001–2999 | 角色权限相关 |
| 3001–3999 | 外部接入系统相关 |
| 4001–4999 | 版本管理相关 |
| 5001–5999 | 文件服务相关 |
| 6001–6999 | 招标文件编制相关 |
| 7001–7999 | 投标文件加解密相关 |
| 8001–8999 | AI 编制系统相关 |

新增业务错误码在 `ele-tender-common` 的 `ResponseCode` 枚举中登记，并注释归属范围。

### 2.6 权限注解

```java
@RequireLogin                          // 需要有效 JWT（内部或外部均可）
@RequirePermission("user:create")      // 需要具体权限码（内部用户）
```

- 所有需要登录的接口**必须**加 `@RequireLogin`
- 不加注解的接口默认公开（仅限登录页、外部认证、健康检查等）

### 2.7 访问日志字段

新增接口时，尽量在过滤器/切面中补充以下字段到 `sup_access_log`：

```
bizType    业务类型（如 TENDER_DOCUMENT）
bizId      业务 ID
projectId  项目 ID
tenderId   标段 ID
fileId     文件 ID（文件相关操作）
```

### 2.8 Swagger 注解

使用 SpringDoc（OpenAPI 3）：

```java
@Tag(name = "用户管理")                    // Controller 级别
@Operation(summary = "分页查询用户")        // 方法级别
@Schema(description = "用户创建请求")       // DTO 级别
```

---

## 3. 代码组织规范

### 3.1 包结构

```
com.jy.eletender.{module}/
├── config/          Spring 配置类
├── controller/      HTTP 控制器
├── service/
│   ├── I*Service    接口
│   └── impl/        实现类
├── mapper/          MyBatis-Plus Mapper 接口
├── entity/          数据库实体（继承 BaseEntity）
├── dto/
│   ├── request/     请求 DTO
│   └── response/    响应 DTO / VO
├── enums/           模块内枚举
├── constant/        模块内常量
├── exception/       模块自定义异常（少用，优先 BusinessException）
├── handler/         GlobalExceptionHandler、MetaObjectHandlerImpl
├── aspect/          AOP 切面
└── util/            模块工具类
```

### 3.2 命名规范

| 元素 | 规范 | 示例 |
|------|------|------|
| 类 | PascalCase | `UserController`、`IUserService` |
| Service 接口 | `I` 前缀 + `Service` | `IUserService` |
| Service 实现 | 接口名去 `I` + `Impl` | `UserServiceImpl` |
| 方法 / 变量 | camelCase | `getUserById`、`pageSize` |
| 常量 | UPPER_SNAKE_CASE | `DEFAULT_PAGE_SIZE` |
| 数据库列 | snake_case | `create_time`、`bid_record_id` |
| 请求 DTO | `*Request` | `UserCreateRequest` |
| 响应 DTO | `*Response` / `*VO` | `UserDetailResponse` |
| 枚举 | PascalCase，枚举值 UPPER_SNAKE | `BidDocumentUploadStatus.PROCESSING` |

### 3.3 分层职责

**Controller**：
- 参数接收与基础校验（`@Valid`、`@NotBlank`）
- 权限注解（`@RequireLogin`、`@RequirePermission`）
- 调用 Service，封装 `Result<T>` 返回
- **禁止**包含业务逻辑

**Service**（`I*Service` + `impl/`）：
- 所有业务逻辑
- 多写操作使用 `@Transactional`
- **禁止**直接透传协议 DTO（先在 controller/facade 边界转换）

**Mapper**：
- 继承 `BaseMapper<T>`
- 简单 CRUD 直接用 MyBatis-Plus，复杂查询写 XML（`src/main/resources/mapper/`）

### 3.4 DTO / 类型归属

| 场景 | 放哪 |
|------|------|
| 跨模块通用类型（非交互协议） | `ele-tender-common` |
| 对外交互协议 DTO / SPI / 路径常量 | `ele-tender-common-interaction` |
| 模块内部 DTO | 所在模块的 `dto/` 包 |
| 模块内部枚举 | 所在模块的 `enums/` 包 |

**单源原则**：严禁在多个模块维护同名同义的 DTO，必须引用唯一来源。

**协议 DTO 约束**：
- `ele-tender-common-interaction` 中的协议 DTO 不带 `jakarta.validation` 注解
- 在 controller / facade 边界做显式转换（mapper），service 层不接收协议 DTO

### 3.5 事务规范

```java
// 多写操作或需要一致性保证时使用
@Transactional(rollbackFor = Exception.class)
public void doSomething() { ... }
```

- 只读查询**不加** `@Transactional`（或加 `readOnly = true`）
- 避免在 `@Transactional` 方法中调用外部 HTTP 接口（影响事务时长）

### 3.6 依赖管理

- 新增 Maven 依赖必须在**父 POM** 的 `<dependencyManagement>` 中声明版本
- 子模块 POM 只声明 `groupId` + `artifactId`，不写 `<version>`

---

## 4. 异常处理规范

### 4.1 异常类型

| 类 | 场景 |
|----|------|
| `BusinessException(code, message)` | 可预期的业务错误（如资源不存在、状态非法） |
| `AuthException(code, message)` | 认证/鉴权失败 |
| 原生 `RuntimeException` | 不可预期的系统错误（由全局处理器兜底） |

```java
// 抛业务异常（推荐）
throw new BusinessException(ResponseCode.BID_DOCUMENT_NOT_FOUND);

// 或带自定义消息
throw new BusinessException(7002, "投标文件 " + fileId + " 不存在");
```

### 4.2 全局异常处理器

每个后端模块必须有 `GlobalExceptionHandler`（放在 `handler/` 包），统一处理：

| 异常 | 返回 |
|------|------|
| `AuthException` | `Result.fail(code, message)` |
| `BusinessException` | `Result.fail(code, message)` |
| `MethodArgumentNotValidException` | `Result.fail(400, "field: message")` |
| `BindException` | `Result.fail(400, "field: message")` |
| `HttpMessageNotReadableException` | `Result.fail(400, "请求体格式错误")` |
| 其他 `Exception` | `Result.fail("系统异常: " + detail)` |，并打印完整堆栈

- HTTP 状态码**统一返回 200**，不通过 HTTP 状态码区分业务错误
- 兜底异常 message 为空时，使用异常类名（避免返回空 message）

---

## 5. 安全规范

### 5.1 字符集

```java
// 所有字节/字符串转换必须显式指定 UTF-8
byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
String str   = new String(bytes, StandardCharsets.UTF_8);
```

重点场景：HMAC 签名 key、摘要计算、文件路径序列化。跨平台部署时 JVM 默认字符集不一致，会导致签名/指纹静默失效。

### 5.2 配置快速失败

安全相关配置（密钥、secret、签名 key）缺失时，必须在**启动时或首次调用时**抛出 `IllegalStateException`，禁止静默降级到弱算法或跳过校验：

```java
// 正确示例
@PostConstruct
public void validate() {
    if (StringUtils.isBlank(fingerprintSecret)) {
        throw new IllegalStateException("crypto.bidder-pwd-fingerprint-secret 未配置，服务拒绝启动");
    }
}
```

### 5.3 JWT 与认证

- 四个后端服务（support/file/tender-document/crypto）共享同一套 JWT 密钥
- 所有服务启动时必须执行 `JwtUtil.configure(secret, expiration, externalExpiration)`
- JWT 配置推荐：

```yaml
jwt:
  secret: ${APP_JWT_SECRET:}           # 必须通过环境变量注入，禁止硬编码
  expiration: ${APP_JWT_EXPIRATION:7200}
  external-expiration: ${APP_JWT_EXTERNAL_EXPIRATION:604800}
```

### 5.4 密码与签名工具

```java
// 密码 BCrypt 哈希
PasswordUtil.encode(rawPassword)
PasswordUtil.matches(rawPassword, encoded)

// 外部系统 HMAC-SHA256 签名
SignatureUtil.sign(data, secret)
SignatureUtil.verify(data, signature, secret)
```

### 5.5 日志脱敏

日志中**禁止**记录：
- 完整 JWT Token
- 签名 secret / 私钥内容
- 文件二进制内容
- 原始投标口令

---

## 6. 链路追踪规范

- 所有服务统一传播 HTTP 请求头 `X-Trace-Id`
- MDC 键固定为 `traceId`，日志 pattern 中引用 `%X{traceId:-}`
- 出站 HTTP 请求（向文件服务、业务系统回调）必须透传 `X-Trace-Id`

---

## 7. 前端规范

### 7.1 组件

- 统一使用 Vue 3 `<script setup>` Composition API
- 组件文件名 PascalCase，路由文件名 kebab-case

### 7.2 状态管理

- 状态变更必须通过 Pinia store
- 禁止在组件中直接读写 `localStorage`（store 文件内除外）

### 7.3 API 调用

- 所有后端调用在 `src/api/` 添加对应函数
- 修改后端路径/参数/代理规则时，必须同步更新 `src/api/*`

### 7.4 代理规则

| 前端路径前缀 | 目标服务 | 路径重写 |
|-------------|----------|----------|
| `/support-api/*` | support :8080 | `/support-api/` → `/api/` |
| `/file-api/*` | file :8081 | 无重写 |
| `/core-api/*` | ai-tender-core :8082 | `/core-api/` → `/api/` |
| `/ai-api/*` | ai-tender-ai :8083 | `/ai-api/` → `/api/` |
| `/support-api/*`(AI系统) | ai-tender-support :8080 | `/support-api/` → `/api/` |
