# AI模块动态线程池实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为AI模块实现动态线程池，支持运行时热更新线程池参数，实现用户级和全局级并发控制

**Architecture:** 参数存储复用 `sup_sys_parameter` 表（`AI_THREAD_POOL` 分组），support模块参数变更后通过HTTP通知AI模块Actuator端点刷新；AI模块自建 `DynamicThreadPoolManager` + `UserConcurrencyManager` 管理线程池和并发控制

**Tech Stack:** Spring Boot 3.2.2 Actuator / ThreadPoolExecutor / Semaphore / RestTemplate

---

## File Structure

| 操作 | 文件 | 职责 |
|------|------|------|
| Create | `ai/.../config/ThreadPoolConfig.java` | 线程池相关Bean配置类 |
| Create | `ai/.../config/ThreadPoolProperties.java` | 线程池参数POJO（从DB加载） |
| Create | `ai/.../threadpool/DynamicThreadPoolManager.java` | 动态线程池管理器 |
| Create | `ai/.../threadpool/UserConcurrencyManager.java` | 用户级+全局并发控制 |
| Create | `ai/.../threadpool/ThreadPoolRefreshEndpoint.java` | Actuator刷新端点 |
| Create | `support/.../client/InternalAiServiceClient.java` | support→ai内部HTTP客户端 |
| Create | `support/.../client/InternalAiServiceProperties.java` | AI服务客户端配置属性 |
| Create | `support/.../client/InternalAiServiceClientAutoConfiguration.java` | AI服务客户端自动配置 |
| Create | `support/.../notifier/ThreadPoolConfigNotifier.java` | 参数变更通知器 |
| Modify | `ai/.../processor/AiTaskProcessor.java` | 改为并发控制+线程池提交 |
| Modify | `ai/.../service/impl/AiChatServiceImpl.java` | executor改用DynamicThreadPoolManager |
| Modify | `support/.../service/impl/SysParameterServiceImpl.java` | AI_THREAD_POOL组更新时触发通知 |
| Modify | `common/.../enums/ParamGroup.java` | 新增AI_THREAD_POOL枚举 |
| Modify | `ai/src/main/resources/application.yml` | actuator exposure + internal.support-service配置 |
| Modify | `support/src/main/resources/application.yml` | internal.ai-service配置 |
| Modify | `support/sql/init.sql` | 插入AI_THREAD_POOL参数初始数据 |
| Modify | `support-frontend/.../sys-param/index.vue` | 分组tab新增"AI线程池" |

---

### Task 1: ParamGroup枚举 + init.sql数据

**Files:**
- Modify: `ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/enums/ParamGroup.java`
- Modify: `ele-ai-tender-system/ele-ai-tender-support/sql/init.sql`

- [ ] **Step 1: 修改ParamGroup枚举**

在 `ParamGroup.java` 中新增 `AI_THREAD_POOL` 枚举值：

```java
SYSTEM("SYSTEM", "系统参数"),
SWITCH("SWITCH", "功能开关"),
AI_RULE("AI_RULE", "AI检测规则"),
AI_THREAD_POOL("AI_THREAD_POOL", "AI线程池");
```

- [ ] **Step 2: 修改init.sql**

在 `sup_sys_parameter` INSERT 语句末尾（第513行 `('AI_RULE', 'compliance_rules', ...)` 之后），新增9条AI_THREAD_POOL参数：

```sql
('AI_THREAD_POOL', 'ai_task_timeout_minutes', '10', 'NUMBER', '单任务超时时间(分钟)', '单个AI任务执行超时时间', 5, NOW(), NOW()),
('AI_THREAD_POOL', 'user_max_pending_tasks', '5', 'NUMBER', '每用户最多发起任务数', '每个用户可发起的未完成AI任务上限，超过直接拒绝', 6, NOW(), NOW()),
('AI_THREAD_POOL', 'user_max_concurrent_tasks', '2', 'NUMBER', '每用户同时执行任务数', '每个用户可同时执行的AI任务上限，超过排队等待', 7, NOW(), NOW()),
('AI_THREAD_POOL', 'global_max_pending_tasks', '100', 'NUMBER', '全局最多发起任务数', '系统全局未完成AI任务上限，超过直接拒绝', 8, NOW(), NOW()),
('AI_THREAD_POOL', 'global_max_concurrent_tasks', '10', 'NUMBER', '全局同时执行任务数', '系统全局可同时执行的AI任务上限，超过排队等待', 9, NOW(), NOW());
```

注意：修改第513行末尾的 `;` 为 `,`，使INSERT语句延续。

- [ ] **Step 3: 手动执行SQL插入现有数据库**

在开发环境MySQL中手动执行上述9条INSERT，使现有数据库有这些参数数据。

- [ ] **Step 4: Commit**

```bash
rtk git add ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/enums/ParamGroup.java ele-ai-tender-system/ele-ai-tender-support/sql/init.sql
rtk git commit -m "feat(threadpool): 新增AI_THREAD_POOL参数分组和初始数据"
```

---

### Task 2: ThreadPoolProperties + AiTaskMapper新增统计方法

**Files:**
- Create: `ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/config/ThreadPoolProperties.java`
- Modify: `ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/mapper/AiTaskMapper.java`

- [ ] **Step 1: 创建ThreadPoolProperties**

```java
package com.jy.eleaitender.ai.config;

import lombok.Data;

/**
 * AI线程池动态参数（从sup_sys_parameter表加载）
 */
@Data
public class ThreadPoolProperties {

    private int corePoolSize = 4;
    private int maxPoolSize = 8;
    private int queueCapacity = 20;
    private int keepAliveSeconds = 60;
    private int taskTimeoutMinutes = 10;
    private int userMaxPendingTasks = 5;
    private int userMaxConcurrentTasks = 2;
    private int globalMaxPendingTasks = 100;
    private int globalMaxConcurrentTasks = 10;
}
```

- [ ] **Step 2: AiTaskMapper新增用户/全局任务统计方法**

在 `AiTaskMapper.java` 末尾（第83行之前）新增：

```java
/**
 * 统计用户未完成任务数（PENDING + PROCESSING）
 */
@DataScope(skip = true)
@Select("SELECT COUNT(*) FROM ai_task WHERE create_id = #{userId} " +
        "AND status IN ('PENDING', 'PROCESSING') AND is_delete = 0")
int countPendingByUserId(@Param("userId") Long userId);

/**
 * 统计全局未完成任务数（PENDING + PROCESSING）
 */
@DataScope(skip = true)
@Select("SELECT COUNT(*) FROM ai_task WHERE status IN ('PENDING', 'PROCESSING') AND is_delete = 0")
int countPendingGlobal();
```

- [ ] **Step 3: Commit**

```bash
rtk git add ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/config/ThreadPoolProperties.java ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/mapper/AiTaskMapper.java
rtk git commit -m "feat(threadpool): 新增ThreadPoolProperties参数类和AiTaskMapper统计方法"
```

---

### Task 3: DynamicThreadPoolManager

**Files:**
- Create: `ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/threadpool/DynamicThreadPoolManager.java`

- [ ] **Step 1: 实现DynamicThreadPoolManager**

```java
package com.jy.eleaitender.ai.threadpool;

import com.jy.eleaitender.ai.config.ThreadPoolProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.*;

/**
 * 动态线程池管理器
 * 支持从DB加载参数、运行时热更新
 */
@Slf4j
@Component
public class DynamicThreadPoolManager {

    private final JdbcTemplate jdbcTemplate;
    private volatile ThreadPoolExecutor executor;
    private volatile ThreadPoolProperties properties;

    public DynamicThreadPoolManager(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        this.properties = loadFromDb();
        this.executor = createExecutor(properties);
        log.info("动态线程池初始化完成: core={}, max={}, queue={}, keepAlive={}s",
                properties.getCorePoolSize(), properties.getMaxPoolSize(),
                properties.getQueueCapacity(), properties.getKeepAliveSeconds());
    }

    @PreDestroy
    public void destroy() {
        if (executor != null) {
            executor.shutdownNow();
            log.info("动态线程池已关闭");
        }
    }

    /**
     * 提交任务到线程池，返回Future（带超时控制）
     */
    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }

    /**
     * 提交无返回值任务
     */
    public void execute(Runnable task) {
        executor.execute(task);
    }

    /**
     * 热更新线程池参数
     */
    public synchronized void refresh() {
        ThreadPoolProperties newProps = loadFromDb();
        ThreadPoolProperties oldProps = this.properties;

        // 队列容量变更需要重建线程池
        if (newProps.getQueueCapacity() != oldProps.getQueueCapacity()) {
            log.info("队列容量变更 {} -> {}，重建线程池", oldProps.getQueueCapacity(), newProps.getQueueCapacity());
            ThreadPoolExecutor oldExecutor = this.executor;
            this.executor = createExecutor(newProps);
            // 优雅关闭旧线程池：等待已提交任务完成
            oldExecutor.shutdown();
            try {
                if (!oldExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    oldExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                oldExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        } else {
            // JDK原生支持热更新
            executor.setCorePoolSize(newProps.getCorePoolSize());
            executor.setMaximumPoolSize(newProps.getMaxPoolSize());
            executor.setKeepAliveTime(newProps.getKeepAliveSeconds(), TimeUnit.SECONDS);
        }

        this.properties = newProps;
        log.info("线程池参数已刷新: core={}, max={}, queue={}, keepAlive={}s, timeout={}min",
                newProps.getCorePoolSize(), newProps.getMaxPoolSize(),
                newProps.getQueueCapacity(), newProps.getKeepAliveSeconds(),
                newProps.getTaskTimeoutMinutes());
    }

    public ThreadPoolProperties getProperties() {
        return properties;
    }

    public int getActiveCount() {
        return executor != null ? executor.getActiveCount() : 0;
    }

    public int getPoolSize() {
        return executor != null ? executor.getPoolSize() : 0;
    }

    public int getQueueSize() {
        return executor != null ? executor.getQueue().size() : 0;
    }

    private ThreadPoolExecutor createExecutor(ThreadPoolProperties props) {
        return new ThreadPoolExecutor(
                props.getCorePoolSize(),
                props.getMaxPoolSize(),
                props.getKeepAliveSeconds(),
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(props.getQueueCapacity()),
                r -> {
                    Thread t = new Thread(r, "ai-task-pool-" + System.nanoTime());
                    t.setDaemon(false);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 从sup_sys_parameter表读取AI_THREAD_POOL组参数
     */
    private ThreadPoolProperties loadFromDb() {
        ThreadPoolProperties props = new ThreadPoolProperties();
        String sql = "SELECT param_key, param_value FROM sup_sys_parameter " +
                "WHERE param_group = 'AI_THREAD_POOL' AND is_delete = 0";
        Map<String, String> paramMap = jdbcTemplate.query(sql, rs -> {
            Map<String, String> map = new ConcurrentHashMap<>();
            while (rs.next()) {
                map.put(rs.getString("param_key"), rs.getString("param_value"));
            }
            return map;
        });

        props.setCorePoolSize(getInt(paramMap, "global_max_concurrent_tasks", 4));
        props.setMaxPoolSize(getInt(paramMap, "global_max_concurrent_tasks", 8));
        props.setQueueCapacity(getInt(paramMap, "global_max_pending_tasks", 20));
        props.setTaskTimeoutMinutes(getInt(paramMap, "ai_task_timeout_minutes", 10));
        props.setUserMaxPendingTasks(getInt(paramMap, "user_max_pending_tasks", 5));
        props.setUserMaxConcurrentTasks(getInt(paramMap, "user_max_concurrent_tasks", 2));
        props.setGlobalMaxPendingTasks(getInt(paramMap, "global_max_pending_tasks", 100));
        props.setGlobalMaxConcurrentTasks(getInt(paramMap, "global_max_concurrent_tasks", 10));

        return props;
    }

    private int getInt(Map<String, String> map, String key, int defaultValue) {
        String value = map.get(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("参数{}值无效: {}, 使用默认值: {}", key, value, defaultValue);
            return defaultValue;
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
rtk git add ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/threadpool/DynamicThreadPoolManager.java
rtk git commit -m "feat(threadpool): 实现DynamicThreadPoolManager动态线程池管理器"
```

---

### Task 4: UserConcurrencyManager

**Files:**
- Create: `ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/threadpool/UserConcurrencyManager.java`

- [ ] **Step 1: 实现UserConcurrencyManager**

```java
package com.jy.eleaitender.ai.threadpool;

import com.jy.eleaitender.ai.config.ThreadPoolProperties;
import com.jy.eleaitender.ai.mapper.AiTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * 用户级+全局并发控制管理器
 *
 * 发起数（pendingTasks）：查DB统计 PENDING+PROCESSING 数量
 * 并发数（concurrentTasks）：Semaphore控制 PROCESSING 数量
 *
 * 发起数超限 → 直接拒绝
 * 并发数已满 → 排队等待（任务保持PENDING，下次轮询重试）
 */
@Slf4j
@Component
public class UserConcurrencyManager {

    private final AiTaskMapper aiTaskMapper;
    private volatile Semaphore globalSemaphore;
    private volatile ConcurrentHashMap<Long, Semaphore> userSemaphores;
    private volatile ThreadPoolProperties properties;

    public UserConcurrencyManager(AiTaskMapper aiTaskMapper, DynamicThreadPoolManager threadPoolManager) {
        this.aiTaskMapper = aiTaskMapper;
        this.properties = threadPoolManager.getProperties();
        this.globalSemaphore = new Semaphore(properties.getGlobalMaxConcurrentTasks());
        this.userSemaphores = new ConcurrentHashMap<>();
    }

    /**
     * 检查用户/全局发起数是否超限（查DB统计PENDING+PROCESSING数量）
     *
     * @param userId 用户ID
     * @return true=允许发起, false=发起数超限应拒绝
     */
    public boolean tryReserve(Long userId) {
        // 检查全局发起数
        int globalPending = aiTaskMapper.countPendingGlobal();
        if (globalPending >= properties.getGlobalMaxPendingTasks()) {
            log.warn("全局发起数已达上限: {}/{}", globalPending, properties.getGlobalMaxPendingTasks());
            return false;
        }
        // 检查用户发起数
        int userPending = aiTaskMapper.countPendingByUserId(userId);
        if (userPending >= properties.getUserMaxPendingTasks()) {
            log.warn("用户{}发起数已达上限: {}/{}", userId, userPending, properties.getUserMaxPendingTasks());
            return false;
        }
        return true;
    }

    /**
     * 尝试获取并发执行许可（Semaphore控制）
     *
     * @param userId 用户ID
     * @return true=获得许可可执行, false=并发数已满应排队
     */
    public boolean tryAcquire(Long userId) {
        Semaphore userSemaphore = getOrCreateUserSemaphore(userId);
        // 非阻塞尝试获取：先尝试用户许可，再尝试全局许可
        if (!userSemaphore.tryAcquire()) {
            log.debug("用户{}并发数已满，排队等待", userId);
            return false;
        }
        if (!globalSemaphore.tryAcquire()) {
            // 全局已满，归还用户许可
            userSemaphore.release();
            log.debug("全局并发数已满，排队等待");
            return false;
        }
        return true;
    }

    /**
     * 释放并发执行许可
     */
    public void release(Long userId) {
        Semaphore userSemaphore = userSemaphores.get(userId);
        if (userSemaphore != null) {
            userSemaphore.release();
        }
        globalSemaphore.release();
    }

    /**
     * 热更新并发控制参数
     * 重建Semaphore，已获得的许可不强制回收
     */
    public synchronized void refresh(ThreadPoolProperties newProps) {
        this.properties = newProps;
        // 重建全局Semaphore
        this.globalSemaphore = new Semaphore(newProps.getGlobalMaxConcurrentTasks());
        // 清空用户Semaphore映射，下次使用时按新参数创建
        this.userSemaphores = new ConcurrentHashMap<>();
        log.info("并发控制参数已刷新: globalMax={}, userMax={}",
                newProps.getGlobalMaxConcurrentTasks(), newProps.getUserMaxConcurrentTasks());
    }

    private Semaphore getOrCreateUserSemaphore(Long userId) {
        return userSemaphores.computeIfAbsent(userId,
                id -> new Semaphore(properties.getUserMaxConcurrentTasks()));
    }
}
```

- [ ] **Step 2: Commit**

```bash
rtk git add ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/threadpool/UserConcurrencyManager.java
rtk git commit -m "feat(threadpool): 实现UserConcurrencyManager用户级全局并发控制"
```

---

### Task 5: ThreadPoolRefreshEndpoint + application.yml配置

**Files:**
- Create: `ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/threadpool/ThreadPoolRefreshEndpoint.java`
- Modify: `ele-ai-tender-system/ele-ai-tender-ai/src/main/resources/application.yml`

- [ ] **Step 1: 创建Actuator端点**

```java
package com.jy.eleaitender.ai.threadpool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 线程池参数刷新Actuator端点
 * 供support模块在参数变更后通知刷新
 */
@Slf4j
@Component
@Endpoint(id = "threadpool")
public class ThreadPoolRefreshEndpoint {

    private final DynamicThreadPoolManager threadPoolManager;
    private final UserConcurrencyManager concurrencyManager;

    public ThreadPoolRefreshEndpoint(DynamicThreadPoolManager threadPoolManager,
                                     UserConcurrencyManager concurrencyManager) {
        this.threadPoolManager = threadPoolManager;
        this.concurrencyManager = concurrencyManager;
    }

    @WriteOperation
    public Map<String, Object> refresh() {
        log.info("收到线程池参数刷新请求");
        try {
            threadPoolManager.refresh();
            concurrencyManager.refresh(threadPoolManager.getProperties());
            ThreadPoolProperties props = threadPoolManager.getProperties();
            return Map.of(
                    "status", "success",
                    "corePoolSize", props.getCorePoolSize(),
                    "maxPoolSize", props.getMaxPoolSize(),
                    "queueCapacity", props.getQueueCapacity(),
                    "globalMaxConcurrent", props.getGlobalMaxConcurrentTasks(),
                    "userMaxConcurrent", props.getUserMaxConcurrentTasks()
            );
        } catch (Exception e) {
            log.error("线程池参数刷新失败", e);
            return Map.of("status", "failed", "error", e.getMessage());
        }
    }
}
```

- [ ] **Step 2: 修改AI模块application.yml**

在 `application.yml` 末尾新增 actuator exposure 配置和内部支撑中心服务客户端配置：

```yaml
# Actuator端点暴露
management:
  endpoints:
    web:
      exposure:
        include: health,info,threadpool
  endpoint:
    threadpool:
      enabled: true

# 内部支撑中心服务客户端配置（用于读取sys_parameter）
internal:
  support-service:
    base-url: ${INTERNAL_SUPPORT_SERVICE_URL:http://localhost:8080}
    jwt-secret: ${APP_JWT_SECRET:ele-ai-tender-jwt-secret-key-must-be-at-least-256-bits}
```

- [ ] **Step 3: Commit**

```bash
rtk git add ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/threadpool/ThreadPoolRefreshEndpoint.java ele-ai-tender-system/ele-ai-tender-ai/src/main/resources/application.yml
rtk git commit -m "feat(threadpool): 新增Actuator刷新端点和application.yml配置"
```

---

### Task 6: InternalAiServiceClient（support→ai）

**Files:**
- Create: `ele-ai-tender-system/ele-ai-tender-support/src/main/java/com/jy/eleaitender/support/client/InternalAiServiceClient.java`
- Create: `ele-ai-tender-system/ele-ai-tender-support/src/main/java/com/jy/eleaitender/support/client/InternalAiServiceProperties.java`
- Create: `ele-ai-tender-system/ele-ai-tender-support/src/main/java/com/jy/eleaitender/support/client/InternalAiServiceClientAutoConfiguration.java`
- Modify: `ele-ai-tender-system/ele-ai-tender-support/src/main/resources/application.yml`

- [ ] **Step 1: 创建InternalAiServiceProperties**

```java
package com.jy.eleaitender.support.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 内部AI服务客户端配置属性
 */
@Data
@ConfigurationProperties(prefix = InternalAiServiceProperties.PREFIX)
public class InternalAiServiceProperties {

    static final String PREFIX = "internal.ai-service";

    /** AI服务基础URL，如 http://localhost:8083 */
    private String baseUrl;

    /** 连接超时（毫秒） */
    private int connectTimeout = 5000;

    /** 读取超时（毫秒） */
    private int readTimeout = 10000;

    /** 服务间调用使用的JWT密钥 */
    private String jwtSecret;

    /** 服务间调用Token过期时间（毫秒），默认30分钟 */
    private long tokenExpiration = 30 * 60 * 1000L;
}
```

- [ ] **Step 2: 创建InternalAiServiceClient**

```java
package com.jy.eleaitender.support.client;

import com.jy.eleaitender.common.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

/**
 * 内部AI服务客户端
 * 用于support模块通知AI模块刷新线程池参数
 */
@Slf4j
public class InternalAiServiceClient {

    private static final String SERVICE_NAME = "ele-ai-tender-support";

    private final RestTemplate restTemplate;
    private final InternalAiServiceProperties properties;

    private volatile String cachedToken;
    private volatile long tokenExpireAt;

    public InternalAiServiceClient(RestTemplate restTemplate, InternalAiServiceProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * 通知AI模块刷新线程池参数
     */
    public boolean notifyRefreshThreadPool() {
        String url = properties.getBaseUrl() + "/actuator/threadpool/refresh";
        log.info("通知AI模块刷新线程池参数: url={}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        addAuthHeader(headers);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, request, String.class);
            log.info("AI模块线程池刷新响应: status={}", response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("通知AI模块刷新线程池参数失败: {}", e.getMessage());
            return false;
        }
    }

    private void addAuthHeader(HttpHeaders headers) {
        String token = getOrCreateToken();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private synchronized String getOrCreateToken() {
        long now = System.currentTimeMillis();
        if (cachedToken != null && tokenExpireAt > now + 60_000) {
            return cachedToken;
        }
        String secret = properties.getJwtSecret();
        cachedToken = JwtUtil.generateServiceToken(SERVICE_NAME, secret, properties.getTokenExpiration());
        tokenExpireAt = now + properties.getTokenExpiration();
        return cachedToken;
    }
}
```

- [ ] **Step 3: 创建InternalAiServiceClientAutoConfiguration**

```java
package com.jy.eleaitender.support.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 内部AI服务客户端自动配置
 * 仅在配置了 internal.ai-service.base-url 时生效
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(InternalAiServiceProperties.class)
@ConditionalOnProperty(prefix = InternalAiServiceProperties.PREFIX, name = "base-url")
public class InternalAiServiceClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public InternalAiServiceClient internalAiServiceClient(
            InternalAiServiceProperties properties,
            RestTemplateBuilder restTemplateBuilder) {
        log.info("初始化内部AI服务客户端: baseUrl={}", properties.getBaseUrl());

        RestTemplate restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(properties.getConnectTimeout()))
                .setReadTimeout(Duration.ofMillis(properties.getReadTimeout()))
                .build();

        return new InternalAiServiceClient(restTemplate, properties);
    }
}
```

- [ ] **Step 4: 修改support模块application.yml**

在 support 的 `application.yml` 末尾新增：

```yaml
# 内部AI服务客户端配置
internal:
  ai-service:
    base-url: ${INTERNAL_AI_SERVICE_URL:http://localhost:8083}
    jwt-secret: ${APP_JWT_SECRET:ele-ai-tender-jwt-secret-key-must-be-at-least-256-bits}
```

- [ ] **Step 5: 注册AutoConfiguration**

检查support模块是否有 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件，如果有则添加一行：

```
com.jy.eleaitender.support.client.InternalAiServiceClientAutoConfiguration
```

如果没有该文件，则创建。同时检查是否已有 `InternalFileServiceClientAutoConfiguration` 的注册（在common模块中），此处是support模块独立的AutoConfiguration注册。

- [ ] **Step 6: Commit**

```bash
rtk git add ele-ai-tender-system/ele-ai-tender-support/src/main/java/com/jy/eleaitender/support/client/ ele-ai-tender-system/ele-ai-tender-support/src/main/resources/application.yml
rtk git commit -m "feat(threadpool): 实现support→ai内部服务客户端和自动配置"
```

---

### Task 7: ThreadPoolConfigNotifier + SysParameterServiceImpl集成

**Files:**
- Create: `ele-ai-tender-system/ele-ai-tender-support/src/main/java/com/jy/eleaitender/support/notifier/ThreadPoolConfigNotifier.java`
- Modify: `ele-ai-tender-system/ele-ai-tender-support/src/main/java/com/jy/eleaitender/support/service/impl/SysParameterServiceImpl.java`

- [ ] **Step 1: 创建ThreadPoolConfigNotifier**

```java
package com.jy.eleaitender.support.notifier;

import com.jy.eleaitender.support.client.InternalAiServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 线程池参数变更通知器
 * 当AI_THREAD_POOL组参数被修改时，通知AI模块刷新
 */
@Slf4j
@Component
public class ThreadPoolConfigNotifier {

    @Autowired(required = false)
    private InternalAiServiceClient internalAiServiceClient;

    /**
     * 通知AI模块刷新线程池参数
     * @param paramGroup 变更的参数分组
     */
    public void onParameterChanged(String paramGroup) {
        if (!"AI_THREAD_POOL".equals(paramGroup)) {
            return;
        }
        if (internalAiServiceClient == null) {
            log.warn("AI服务客户端未配置，跳过线程池刷新通知");
            return;
        }
        try {
            boolean success = internalAiServiceClient.notifyRefreshThreadPool();
            if (success) {
                log.info("AI线程池参数刷新通知发送成功");
            } else {
                log.warn("AI线程池参数刷新通知发送失败");
            }
        } catch (Exception e) {
            log.error("AI线程池参数刷新通知异常: {}", e.getMessage());
        }
    }
}
```

- [ ] **Step 2: 修改SysParameterServiceImpl**

在 `SysParameterServiceImpl` 中注入 `ThreadPoolConfigNotifier`，在 `updateByKey` 和 `batchUpdate` 方法中添加通知逻辑。

添加依赖注入：

```java
@Autowired
private ThreadPoolConfigNotifier threadPoolConfigNotifier;
```

修改 `updateByKey` 方法（在第80行的方法中），在 `redisTemplate.delete(cacheKey)` 之后新增通知逻辑。需要在方法中查出param的分组：

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void updateByKey(String paramKey, String paramValue) {
    // 先查出参数分组
    LambdaQueryWrapper<SysParameter> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(SysParameter::getParamKey, paramKey)
                .select(SysParameter::getParamGroup, SysParameter::getParamValue);
    SysParameter existing = sysParameterMapper.selectOne(queryWrapper);
    String paramGroup = existing != null ? existing.getParamGroup() : null;

    // 更新数据库
    LambdaUpdateWrapper<SysParameter> wrapper = new LambdaUpdateWrapper<>();
    wrapper.eq(SysParameter::getParamKey, paramKey)
           .set(SysParameter::getParamValue, paramValue);
    int rows = sysParameterMapper.update(null, wrapper);
    if (rows == 0) {
        throw new BusinessException(ResponseCode.SYS_PARAM_NOT_FOUND);
    }
    // 清除缓存
    String cacheKey = RedisKeyConstant.SYS_PARAM_PREFIX + paramKey;
    redisTemplate.delete(cacheKey);

    // 通知AI模块刷新线程池参数
    threadPoolConfigNotifier.onParameterChanged(paramGroup);
}
```

修改 `batchUpdate` 方法，在循环结束后判断是否涉及AI_THREAD_POOL组：

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void batchUpdate(Map<String, String> params) {
    boolean aiThreadPoolChanged = false;
    for (Map.Entry<String, String> entry : params.entrySet()) {
        updateByKey(entry.getKey(), entry.getValue());
        // updateByKey内部已逐个通知，但批量场景只需通知一次
    }
    // 批量更新后统一通知一次（updateByKey中已逐个通知，此处为兜底）
    if (params.keySet().stream().anyMatch(key -> key.startsWith("ai_") || key.startsWith("user_") || key.startsWith("global_"))) {
        threadPoolConfigNotifier.onParameterChanged("AI_THREAD_POOL");
    }
}
```

注意：由于 `updateByKey` 内部已触发通知，`batchUpdate` 中的通知是冗余的但无害（幂等操作）。为简洁起见，`batchUpdate` 可以只依赖 `updateByKey` 中的通知，不需要额外通知。最终简化方案：**只在 `updateByKey` 中加通知**，`batchUpdate` 调用 `updateByKey` 时自动触发。

- [ ] **Step 3: Commit**

```bash
rtk git add ele-ai-tender-system/ele-ai-tender-support/src/main/java/com/jy/eleaitender/support/notifier/ThreadPoolConfigNotifier.java ele-ai-tender-system/ele-ai-tender-support/src/main/java/com/jy/eleaitender/support/service/impl/SysParameterServiceImpl.java
rtk git commit -m "feat(threadpool): 实现参数变更通知器，SysParameterServiceImpl集成通知逻辑"
```

---

### Task 8: 改造AiTaskProcessor

**Files:**
- Modify: `ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/processor/AiTaskProcessor.java`

- [ ] **Step 1: 重写AiTaskProcessor**

将顺序执行改为并发控制+线程池提交。完整替换 `processPendingTasks` 方法，新增 `executeTask` 方法：

```java
package com.jy.eleaitender.ai.processor;

import com.jy.eleaitender.ai.mapper.AiTaskMapper;
import com.jy.eleaitender.ai.processor.checker.DetectionEngine;
import com.jy.eleaitender.ai.processor.generator.DocumentIntegration;
import com.jy.eleaitender.ai.processor.generator.RequirementGenerator;
import com.jy.eleaitender.ai.processor.generator.ReviewItemGenerator;
import com.jy.eleaitender.ai.processor.generator.TextOptimizer;
import com.jy.eleaitender.ai.threadpool.DynamicThreadPoolManager;
import com.jy.eleaitender.ai.threadpool.UserConcurrencyManager;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.enums.AiTaskType;
import com.jy.eleaitender.common.exception.AiErrorContentException;
import com.jy.eleaitender.common.exception.AiUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * AI任务轮询处理器（定时任务）
 * 负责从ai_task表拉取PENDING任务，经并发控制后提交到动态线程池执行
 */
@Slf4j
@Component
public class AiTaskProcessor {

    @Autowired
    private AiTaskMapper aiTaskMapper;

    @Autowired
    private RequirementGenerator requirementGenerator;

    @Autowired
    private ReviewItemGenerator reviewItemGenerator;

    @Autowired
    private DetectionEngine detectionEngine;

    @Autowired
    private DocumentIntegration documentIntegration;

    @Autowired
    private TextOptimizer textOptimizer;

    @Autowired
    private DynamicThreadPoolManager threadPoolManager;

    @Autowired
    private UserConcurrencyManager concurrencyManager;

    /**
     * 每5秒轮询待处理任务
     */
    @Scheduled(fixedDelay = 5000)
    public void processPendingTasks() {
        List<AiTask> tasks = aiTaskMapper.selectPendingTasks(10);
        if (tasks.isEmpty()) {
            return;
        }
        log.debug("待处理AI任务: {}", tasks.size());

        for (AiTask task : tasks) {
            Long userId = task.getCreateId();
            if (userId == null || userId == 0) {
                // 无用户信息的任务直接执行
                submitDirectly(task);
                continue;
            }

            // 检查发起数上限
            if (!concurrencyManager.tryReserve(userId)) {
                log.warn("任务{}发起数超限，拒绝: userId={}", task.getId(), userId);
                aiTaskMapper.markFailed(task.getId(), "超过最大发起数限制，请等待已有任务完成");
                continue;
            }

            // 检查并发数上限（排队机制：CAS抢占成功后尝试获取许可）
            int updated = aiTaskMapper.casUpdateStatus(task.getId(), "PENDING", "PROCESSING");
            if (updated == 0) {
                continue; // 已被其他实例抢占
            }

            if (!concurrencyManager.tryAcquire(userId)) {
                // 并发数已满，回退状态为PENDING，下次轮询重试
                aiTaskMapper.casUpdateStatus(task.getId(), "PROCESSING", "PENDING");
                log.debug("任务{}并发数已满，排队等待: userId={}", task.getId(), userId);
                continue;
            }

            // 获得许可，提交到线程池执行
            submitWithConcurrencyControl(task, userId);
        }
    }

    /**
     * 无并发控制的直接提交（兼容无用户信息的任务）
     */
    private void submitDirectly(AiTask task) {
        int updated = aiTaskMapper.casUpdateStatus(task.getId(), "PENDING", "PROCESSING");
        if (updated == 0) {
            return;
        }
        log.info("开始处理AI任务(直接): id={}, type={}", task.getId(), task.getTaskType());

        threadPoolManager.execute(() -> {
            try {
                String result = dispatch(task);
                aiTaskMapper.markCompleted(task.getId(), result);
                log.info("AI任务处理完成: id={}, type={}", task.getId(), task.getTaskType());
            } catch (AiUnavailableException e) {
                log.error("AI服务不可用: id={}, type={}", task.getId(), task.getTaskType(), e);
                aiTaskMapper.markAiUnavailable(task.getId(), e.getMessage());
            } catch (AiErrorContentException e) {
                log.error("AI任务内容异常: id={}, type={}", task.getId(), task.getTaskType(), e);
                aiTaskMapper.markFailed(task.getId(), e.getContent(), truncateErrorMsg(e.getMessage()));
            } catch (Exception e) {
                log.error("AI任务处理失败: id={}, type={}", task.getId(), task.getTaskType(), e);
                aiTaskMapper.markFailed(task.getId(), truncateErrorMsg(e.getMessage()));
            }
        });
    }

    /**
     * 带并发控制和超时的任务提交
     */
    private void submitWithConcurrencyControl(AiTask task, Long userId) {
        int timeoutMinutes = threadPoolManager.getProperties().getTaskTimeoutMinutes();
        if (task.getTimeoutMinutes() != null && task.getTimeoutMinutes() > 0) {
            timeoutMinutes = task.getTimeoutMinutes();
        }

        log.info("开始处理AI任务: id={}, type={}, userId={}, timeout={}min",
                task.getId(), task.getTaskType(), userId, timeoutMinutes);

        Future<String> future = threadPoolManager.submit(() -> dispatch(task));

        threadPoolManager.execute(() -> {
            try {
                String result = future.get(timeoutMinutes, TimeUnit.MINUTES);
                aiTaskMapper.markCompleted(task.getId(), result);
                log.info("AI任务处理完成: id={}, type={}", task.getId(), task.getTaskType());
            } catch (TimeoutException e) {
                future.cancel(true);
                log.error("AI任务执行超时: id={}, type={}, timeout={}min", task.getId(), task.getTaskType(), timeoutMinutes);
                aiTaskMapper.markFailed(task.getId(), "任务执行超时(" + timeoutMinutes + "分钟)");
            } catch (Exception e) {
                Throwable cause = e.getCause();
                if (cause instanceof AiUnavailableException aue) {
                    log.error("AI服务不可用: id={}, type={}", task.getId(), task.getTaskType(), aue);
                    aiTaskMapper.markAiUnavailable(task.getId(), aue.getMessage());
                } else if (cause instanceof AiErrorContentException aece) {
                    log.error("AI任务内容异常: id={}, type={}", task.getId(), task.getTaskType(), aece);
                    aiTaskMapper.markFailed(task.getId(), aece.getContent(), truncateErrorMsg(aece.getMessage()));
                } else {
                    log.error("AI任务处理失败: id={}, type={}", task.getId(), task.getTaskType(), e);
                    aiTaskMapper.markFailed(task.getId(), truncateErrorMsg(e.getMessage()));
                }
            } finally {
                concurrencyManager.release(userId);
            }
        });
    }

    /**
     * 按任务类型分发到实际处理器
     */
    private String dispatch(AiTask task) {
        AiTaskType taskType = AiTaskType.fromCode(task.getTaskType());
        return switch (taskType) {
            case REQUIREMENT_GENERATE,
                 PROJECT_REQUIREMENT_GENERATE -> requirementGenerator.generate(task);
            case REVIEW_ITEM_GENERATE -> reviewItemGenerator.generate(task);
            case DETECTION_SENSITIVE_WORD,
                 DETECTION_TYPO,
                 DETECTION_POLICY_REVIEW,
                 DETECTION_FORMAT_CHECK -> detectionEngine.detect(task);
            case DOCUMENT_INTEGRATION -> documentIntegration.integration(task);
            case TEXT_OPTIMIZE -> textOptimizer.optimize(task);
        };
    }

    private String truncateErrorMsg(String msg) {
        if (msg == null) return "未知错误";
        return msg.length() > 500 ? msg.substring(0, 500) : msg;
    }
}
```

- [ ] **Step 2: Commit**

```bash
rtk git add ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/processor/AiTaskProcessor.java
rtk git commit -m "feat(threadpool): 改造AiTaskProcessor为并发控制+线程池提交模式"
```

---

### Task 9: 修改AiChatServiceImpl

**Files:**
- Modify: `ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/service/impl/AiChatServiceImpl.java`

- [ ] **Step 1: 替换executor**

将 `AiChatServiceImpl` 中第44行的 `Executors.newVirtualThreadPerTaskExecutor()` 替换为注入 `DynamicThreadPoolManager`。

删除：
```java
private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
```

新增注入：
```java
@Autowired
private DynamicThreadPoolManager threadPoolManager;
```

修改第48行和第110行的 `executor.execute(...)` 为 `threadPoolManager.execute(...)`。

删除不再需要的import：
```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
```

- [ ] **Step 2: Commit**

```bash
rtk git add ele-ai-tender-system/ele-ai-tender-ai/src/main/java/com/jy/eleaitender/ai/service/impl/AiChatServiceImpl.java
rtk git commit -m "feat(threadpool): AiChatServiceImpl改用DynamicThreadPoolManager"
```

---

### Task 10: 前端——支撑中心新增AI线程池tab

**Files:**
- Modify: `ele-ai-tender-support-frontend/src/views/system/sys-param/index.vue`

- [ ] **Step 1: 修改index.vue**

在第14行 `<el-tab-pane label="AI检测规则" name="AI_RULE" />` 后新增：

```html
<el-tab-pane label="AI线程池" name="AI_THREAD_POOL" />
```

仅此一行修改，页面其余逻辑（按 `activeGroup` 查询参数、NUMBER类型渲染 `el-input-number`、批量保存）均已支持，无需其他改动。

- [ ] **Step 2: Commit**

```bash
rtk git add ele-ai-tender-support-frontend/src/views/system/sys-param/index.vue
rtk git commit -m "feat(threadpool): 支撑中心新增AI线程池参数管理tab"
```

---

### Task 11: common模块install + 编译验证

**Files:** 无新增/修改，验证编译

- [ ] **Step 1: 安装common模块到本地仓库**

```bash
cd ele-ai-tender-system && mvn install -pl ele-ai-tender-common -am -Dmaven.test.skip=true
```

- [ ] **Step 2: 编译AI模块**

```bash
cd ele-ai-tender-system && mvn clean compile -pl ele-ai-tender-ai -am -Dmaven.test.skip=true
```

- [ ] **Step 3: 编译support模块**

```bash
cd ele-ai-tender-system && mvn clean compile -pl ele-ai-tender-support -am -Dmaven.test.skip=true
```

- [ ] **Step 4: 编译前端**

```bash
cd ele-ai-tender-support-frontend && npm run build
```

- [ ] **Step 5: Commit（如有编译问题则修复后提交）**

---

### Task 12: 端到端验证

**Files:** 无新增/修改，手动验证

- [ ] **Step 1: 启动support和ai服务**

```bash
cd ele-ai-tender-system/ele-ai-tender-support && mvn spring-boot:run
cd ele-ai-tender-system/ele-ai-tender-ai && mvn spring-boot:run
```

- [ ] **Step 2: 验证Actuator端点**

```bash
curl -X POST http://localhost:8083/actuator/threadpool/refresh
```

预期返回 JSON 含 `status: "success"` 和当前参数值。

- [ ] **Step 3: 验证支撑中心前端**

打开支撑中心前端 → 系统参数 → "AI线程池" tab → 确认9个参数正确显示 → 修改某个参数 → 保存 → 检查AI模块日志是否收到刷新通知。

- [ ] **Step 4: 验证并发控制**

提交多个AI任务，观察：
- 超过 `user_max_concurrent_tasks` 的任务保持PENDING（排队等待）
- 超过 `user_max_pending_tasks` 的任务被标记FAILED（拒绝）
- 超时任务被自动取消并标记FAILED
