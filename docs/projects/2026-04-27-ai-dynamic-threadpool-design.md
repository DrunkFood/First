# AI模块动态线程池设计

## 背景

AI模块 `AiTaskProcessor` 使用单线程 `@Scheduled(fixedDelay=5000)` 轮询，取最多10个PENDING任务后**顺序执行**。一个任务卡住，后续全部等待，导致超时。需要改为动态线程池并发执行，支持运行时热更新参数，并实现用户级和全局级并发控制。

## 参数模型

复用 `sup_sys_parameter` 表，新增 `param_group = AI_THREAD_POOL`：

| param_key | 含义 | 默认值 | param_type |
|-----------|------|--------|------------|
| `ai_core_pool_size` | 线程池核心线程数 | 4 | NUMBER |
| `ai_max_pool_size` | 线程池最大线程数 | 8 | NUMBER |
| `ai_queue_capacity` | 任务队列容量 | 20 | NUMBER |
| `ai_keep_alive_seconds` | 空闲线程存活时间(秒) | 60 | NUMBER |
| `ai_task_timeout_minutes` | 单任务超时时间(分钟) | 10 | NUMBER |
| `user_max_pending_tasks` | 每用户最多发起任务数 | 5 | NUMBER |
| `user_max_concurrent_tasks` | 每用户同时执行任务数 | 2 | NUMBER |
| `global_max_pending_tasks` | 全局最多发起任务数 | 100 | NUMBER |
| `global_max_concurrent_tasks` | 全局同时执行任务数 | 10 | NUMBER |

## 数据流

```
支撑中心后台修改参数
  → PUT /api/v1/sys-params (support模块)
  → 更新DB + 删除Redis缓存
  → ThreadPoolConfigNotifier 检测到 AI_THREAD_POOL 组变更
  → HTTP通知 AI模块 Actuator端点 POST /actuator/threadpool/refresh
  → AI模块重新读取DB → 更新线程池参数 + Semaphore许可数
```

参数不走Redis缓存（参考 `ModelConfigCacheService` 直接查DB的设计），保证配置实时生效。

## 核心组件

### 1. DynamicThreadPoolManager（ai模块）

持有 `ThreadPoolExecutor` 实例，支持核心/最大线程数、队列容量、keepAlive热更新。

- 启动时从DB加载参数创建线程池
- 热更新：`setCorePoolSize()`、`setMaximumPoolSize()`、`setKeepAliveTime()` 是JDK原生支持的方法；队列容量变更需重建线程池
- 线程命名：`ai-task-pool-{n}`，便于监控和日志排查

### 2. UserConcurrencyManager（ai模块）

用户级+全局并发控制。

- 全局并发：单一 `Semaphore(globalMaxConcurrentTasks)`
- 用户并发：`ConcurrentHashMap<userId, Semaphore>`，每个用户一个 `Semaphore(userMaxConcurrentTasks)`
- 发起数统计：实时查 `ai_task` 表（`status IN ('PENDING','PROCESSING')`），不依赖内存计数器，多实例部署一致
- 方法：
  - `tryReserve(userId, taskId)` — 检查用户+全局发起数上限（查DB统计），超限返回false
  - `tryAcquire(userId)` — 检查用户+全局并发执行数上限（Semaphore），无空位返回false
  - `release(userId)` — 释放用户+全局Semaphore许可
- Semaphore热更新：新建Semaphore替换，已获得的许可不强制回收，等任务自然完成后新任务按新限额排队

### 3. ThreadPoolRefreshEndpoint（ai模块）

Spring Boot Actuator `@Endpoint`，`@WriteOperation` 暴露刷新端点。

- `POST /actuator/threadpool/refresh` → 调用 Manager 和 Concurrency 的刷新方法
- 需在 `application.yml` 中 exposure 该端点

### 4. ThreadPoolConfigNotifier（support模块）

在 `SysParameterServiceImpl` 更新参数时，如果变更的参数属于 `AI_THREAD_POOL` 组，通过 `InternalAiServiceClient` 通知AI模块刷新。

### 5. InternalAiServiceClient（support模块）

复用现有 `InternalFileServiceClient` 模式（RestTemplate + JWT服务认证），调用AI模块的Actuator端点。

## 任务提交流程

```
AiTaskProcessor.poll()
  → 取PENDING任务列表(最多10个)
  → 遍历每个任务:
      1. tryReserve(userId, taskId)
         失败(发起数超限) → 标记任务FAILED(reason="超过最大发起数限制")
      2. tryAcquire(userId)
         失败(并发数已满) → 任务保持PENDING，下次轮询重试（排队等待）
         成功 → DynamicThreadPoolManager.execute(taskWrapper)
           → Future.get(timeoutMinutes, MINUTES)
           → 正常完成: release(userId) + 标记COMPLETED
           → 超时: future.cancel(true) + release(userId) + 标记FAILED
           → 异常: release(userId) + 标记FAILED
```

## 超时与异常处理

- 用 `Future.get(timeout, TimeUnit)` 包裹每个任务执行
- 超时后 `future.cancel(true)` 中断线程，AI生成类任务需检查中断状态及时退出
- release 用 try-finally 确保一定执行，防止许可泄漏
- 现有 `AiServiceLifecycle` 重启恢复逻辑不变

## Semaphore热更新策略

刷新时：
1. 读取新参数
2. 如果新并发数 >= 当前活跃数 → 直接替换Semaphore
3. 如果新并发数 < 当前活跃数 → 新Semaphore立即生效，但已获得的许可不强制回收，等已有任务自然完成后新任务按新限额排队

## 发起数统计

```sql
-- 用户已发起未完成任务数
SELECT COUNT(*) FROM ai_task
WHERE user_id = ? AND status IN ('PENDING', 'PROCESSING') AND is_delete = 0

-- 全局已发起未完成任务数
SELECT COUNT(*) FROM ai_task
WHERE status IN ('PENDING', 'PROCESSING') AND is_delete = 0
```

## 改动范围

### 新增文件

| 文件 | 模块 | 说明 |
|------|------|------|
| `DynamicThreadPoolManager.java` | ai | 动态线程池管理器 |
| `UserConcurrencyManager.java` | ai | 用户级+全局并发控制 |
| `ThreadPoolRefreshEndpoint.java` | ai | Actuator端点 |
| `ThreadPoolConfigNotifier.java` | support | 参数变更通知器 |
| `InternalAiServiceClient.java` | support | support→ai内部HTTP客户端 |

### 修改文件

| 文件 | 模块 | 改动 |
|------|------|------|
| `AiTaskProcessor.java` | ai | 轮询取任务后改为并发控制+线程池提交 |
| `AiChatServiceImpl.java` | ai | executor改为使用DynamicThreadPoolManager |
| `SysParameterServiceImpl.java` | support | 更新AI_THREAD_POOL组时触发通知 |
| `ParamGroup.java` | common | 枚举新增AI_THREAD_POOL |
| `init.sql` | support | 插入AI_THREAD_POOL组参数初始数据 |
| `application.yml` | ai | actuator exposure配置 |
| `sys-param/index.vue` | support-frontend | 分组tab新增"AI线程池" |

### 不改动

- `AiServiceLifecycle` — 重启恢复逻辑不变
- `ModelConfigCacheService` — 无关
- `DetectionEngine` / 各Generator — 执行逻辑不变，由上层控制超时
