# 交互层一期实施计划

> **For Claude：** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标：** 构建电子标系统与业务系统之间的交互 starter 与共享协议层，同时补强现有服务的入站请求日志与出站调用日志能力。

**补充说明：** 回传接口中的 `fileId` 仅作为文件引用。为形成业务系统可落地接收文件的闭环，后续补充实现需要同时覆盖“根据 `fileId` 查询文件信息”和“根据 `fileId` 下载文件”的标准能力，建议优先复用 `ele-tender-file` 现有接口。

**架构方案：** 在 `ele-tender-system` 下新增交互相关 Maven 模块，对外保留 `ele-tender-interaction` 模块名，对内拆分为 `core`、`autoconfigure`、`spring-boot-starter` 三层。复用 `ele-tender-support` 现有外部认证能力，在 `ele-tender-common` 中补充统一的 trace/logging 基础设施，在 `ele-tender-common-interaction` 中定义交互 DTO 与 SPI 契约，并通过 starter 在业务系统中自动暴露 `/api/eleTender/interaction/*` 标准接口。

**技术栈：** Java 21、Spring Boot 3.2.2、Spring MVC、Spring Boot AutoConfiguration、MyBatis-Plus 3.5.5、Redis、JUnit 5、Spring Boot Test、MockMvc、ApplicationContextRunner

---

### 任务 1：新增模块骨架并接入 Maven reactor

**文件：**
- 修改：`ele-tender-system/pom.xml`
- 新建：`ele-tender-system/ele-tender-common-interaction/pom.xml`
- 新建：`ele-tender-system/ele-tender-interaction/pom.xml`
- 新建：`ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/pom.xml`
- 新建：`ele-tender-system/ele-tender-interaction/ele-tender-interaction-autoconfigure/pom.xml`
- 新建：`ele-tender-system/ele-tender-interaction/ele-tender-interaction-spring-boot-starter/pom.xml`
- 新建：`ele-tender-system/ele-tender-tender-document/pom.xml`
- 新建：`ele-tender-system/ele-tender-tender-document/src/main/java/com/jy/eletender/tenderdocument/TenderDocumentApplication.java`
- 新建：`ele-tender-system/ele-tender-tender-document/src/main/resources/application.yml`

**步骤 1：先写一个会失败的 reactor 检查**

执行：

```bash
cd ele-tender-system && mvn -pl ele-tender-common-interaction -am test
```

预期：失败，并提示类似 `Could not find the selected project in the reactor`。

**步骤 2：先把模块声明补进父 POM**

将 `ele-tender-system/pom.xml` 的 `<modules>` 修改为：

```xml
<modules>
    <module>ele-tender-common</module>
    <module>ele-tender-common-interaction</module>
    <module>ele-tender-file</module>
    <module>ele-tender-support</module>
    <module>ele-tender-interaction</module>
    <module>ele-tender-tender-document</module>
</modules>
```

同时在 `<dependencyManagement>` 中加入：

```xml
<dependency>
    <groupId>com.jy.eletender</groupId>
    <artifactId>ele-tender-common-interaction</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>com.jy.eletender</groupId>
    <artifactId>ele-tender-interaction-core</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>com.jy.eletender</groupId>
    <artifactId>ele-tender-interaction-autoconfigure</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>com.jy.eletender</groupId>
    <artifactId>ele-tender-interaction-spring-boot-starter</artifactId>
    <version>${project.version}</version>
</dependency>
```

**步骤 3：补齐最小模块 POM**

`ele-tender-system/ele-tender-common-interaction/pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.jy.eletender</groupId>
        <artifactId>ele-tender-system</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>ele-tender-common-interaction</artifactId>
    <packaging>jar</packaging>
    <dependencies>
        <dependency>
            <groupId>com.jy.eletender</groupId>
            <artifactId>ele-tender-common</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>jakarta.validation</groupId>
            <artifactId>jakarta.validation-api</artifactId>
        </dependency>
    </dependencies>
</project>
```

`ele-tender-system/ele-tender-interaction/pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.jy.eletender</groupId>
        <artifactId>ele-tender-system</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>ele-tender-interaction</artifactId>
    <packaging>pom</packaging>
    <modules>
        <module>ele-tender-interaction-core</module>
        <module>ele-tender-interaction-autoconfigure</module>
        <module>ele-tender-interaction-spring-boot-starter</module>
    </modules>
</project>
```

`ele-tender-system/ele-tender-tender-document/pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.jy.eletender</groupId>
        <artifactId>ele-tender-system</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>ele-tender-tender-document</artifactId>
    <packaging>jar</packaging>
    <dependencies>
        <dependency>
            <groupId>com.jy.eletender</groupId>
            <artifactId>ele-tender-common</artifactId>
        </dependency>
        <dependency>
            <groupId>com.jy.eletender</groupId>
            <artifactId>ele-tender-common-interaction</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

补一个最小启动类：

```java
package com.jy.eletender.tenderdocument;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.jy.eletender.tenderdocument.mapper")
public class TenderDocumentApplication {

    public static void main(String[] args) {
        SpringApplication.run(TenderDocumentApplication.class, args);
    }
}
```

**步骤 4：验证 reactor 能识别新模块**

执行：

```bash
cd ele-tender-system && mvn -pl ele-tender-common-interaction,ele-tender-interaction/ele-tender-interaction-spring-boot-starter,ele-tender-tender-document -am test
```

预期：通过，且新模块已被 reactor 正常识别。

**步骤 5：提交**

```bash
git add ele-tender-system/pom.xml ele-tender-system/ele-tender-common-interaction ele-tender-system/ele-tender-interaction ele-tender-system/ele-tender-tender-document
git commit -m "feat: scaffold interaction and tender document modules"
```

### 任务 2：在 `ele-tender-common` 中补统一 traceId 与请求日志基础设施

**文件：**
- 修改：`ele-tender-system/ele-tender-common/pom.xml`
- 新建：`ele-tender-system/ele-tender-common/src/main/java/com/jy/eletender/common/logging/TraceConstants.java`
- 新建：`ele-tender-system/ele-tender-common/src/main/java/com/jy/eletender/common/logging/TraceContext.java`
- 新建：`ele-tender-system/ele-tender-common/src/main/java/com/jy/eletender/common/logging/SensitiveLogMasker.java`
- 新建：`ele-tender-system/ele-tender-common/src/main/java/com/jy/eletender/common/logging/HttpRequestLogFilter.java`
- 新建：`ele-tender-system/ele-tender-common/src/main/java/com/jy/eletender/common/logging/LoggingAutoConfiguration.java`
- 新建：`ele-tender-system/ele-tender-common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- 新建：`ele-tender-system/ele-tender-common/src/test/java/com/jy/eletender/common/logging/HttpRequestLogFilterTest.java`
- 修改：`ele-tender-system/ele-tender-support/src/main/resources/application.yml`
- 修改：`ele-tender-system/ele-tender-file/src/main/resources/application.yml`
- 修改：`ele-tender-system/ele-tender-tender-document/src/main/resources/application.yml`

**步骤 1：先写失败测试**

创建 `HttpRequestLogFilterTest`，验证 trace 透传和敏感信息脱敏：

```java
@Test
void shouldCreateTraceIdAndMaskSensitiveHeaders() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    request.addHeader("Authorization", "Bearer abcdefghijklmn");
    request.addHeader("X-App-Key", "demo-key");
    request.addHeader("X-Signature", "SECRET-SIGNATURE");

    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    HttpRequestLogFilter filter = new HttpRequestLogFilter();
    filter.doFilter(request, response, chain);

    assertThat(response.getHeader("X-Trace-Id")).isNotBlank();
    assertThat(SensitiveLogMasker.maskToken("Bearer abcdefghijklmn")).contains("***");
    assertThat(SensitiveLogMasker.maskSecret("SECRET-SIGNATURE")).contains("***");
}
```

**步骤 2：运行测试确认失败**

执行：

```bash
cd ele-tender-system && mvn -pl ele-tender-common -am -Dtest=HttpRequestLogFilterTest test
```

预期：失败，因为相关日志类尚未创建。

**步骤 3：实现统一日志基础设施**

使用 `OncePerRequestFilter`：
- 读取请求头中的 `X-Trace-Id`，没有则生成；
- 将其放入 MDC 的 `traceId`；
- 同时写回响应头；
- 记录方法、URI、query、客户端 IP、耗时、脱敏后的鉴权头和异常摘要。

实现骨架如下：

```java
@Slf4j
public class HttpRequestLogFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String traceId = TraceContext.initTraceId(request.getHeader(TraceConstants.TRACE_ID_HEADER));
        response.setHeader(TraceConstants.TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            log.info("HTTP IN traceId={} method={} uri={} status={} elapsedMs={} auth={} appKey={} signature={}",
                    traceId,
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    elapsed,
                    SensitiveLogMasker.maskToken(request.getHeader("Authorization")),
                    request.getHeader("X-App-Key"),
                    SensitiveLogMasker.maskSecret(request.getHeader("X-Signature")));
            TraceContext.clear();
        }
    }
}
```

通过自动装配注册 filter，使 `support`、`file`、`tender-document` 默认接入：

```java
@AutoConfiguration
public class LoggingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public FilterRegistrationBean<HttpRequestLogFilter> httpRequestLogFilter() {
        FilterRegistrationBean<HttpRequestLogFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new HttpRequestLogFilter());
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
```

并在 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 中加入：

```text
com.jy.eletender.common.logging.LoggingAutoConfiguration
```

同时修改三个 `application.yml`，让日志级别模式带上 MDC traceId：

```yaml
logging:
  pattern:
    level: "%5p [traceId:%X{traceId:-}]"
```

**步骤 4：重新执行测试**

执行：

```bash
cd ele-tender-system && mvn -pl ele-tender-common -am -Dtest=HttpRequestLogFilterTest test
```

预期：通过。

**步骤 5：提交**

```bash
git add ele-tender-system/ele-tender-common ele-tender-system/ele-tender-support/src/main/resources/application.yml ele-tender-system/ele-tender-file/src/main/resources/application.yml ele-tender-system/ele-tender-tender-document/src/main/resources/application.yml
git commit -m "feat: add shared trace and request logging"
```

### 任务 3：定义交互协议 DTO、常量与 SPI 契约

**文件：**
- 新建：`ele-tender-system/ele-tender-common-interaction/src/main/java/com/jy/eletender/common/interaction/constant/InteractionApiPaths.java`
- 新建：`ele-tender-system/ele-tender-common-interaction/src/main/java/com/jy/eletender/common/interaction/constant/InteractionHeaderConstants.java`
- 新建：`ele-tender-system/ele-tender-common-interaction/src/main/java/com/jy/eletender/common/interaction/enums/InteractionBizType.java`
- 新建：`ele-tender-system/ele-tender-common-interaction/src/main/java/com/jy/eletender/common/interaction/dto/IdentityContext.java`
- 新建：`ele-tender-system/ele-tender-common-interaction/src/main/java/com/jy/eletender/common/interaction/dto/IdentityQueryResponse.java`
- 新建：`ele-tender-system/ele-tender-common-interaction/src/main/java/com/jy/eletender/common/interaction/dto/ProjectBasicInfoQueryRequest.java`
- 新建：`ele-tender-system/ele-tender-common-interaction/src/main/java/com/jy/eletender/common/interaction/dto/ProjectBasicInfoResponse.java`
- 新建：`ele-tender-system/ele-tender-common-interaction/src/main/java/com/jy/eletender/common/interaction/dto/BidRecordSchemeQueryRequest.java`
- 新建：`ele-tender-system/ele-tender-common-interaction/src/main/java/com/jy/eletender/common/interaction/dto/BidRecordSchemeResponse.java`
- 新建：`ele-tender-system/ele-tender-common-interaction/src/main/java/com/jy/eletender/common/interaction/dto/TenderPdfCallbackRequest.java`
- 新建：`ele-tender-system/ele-tender-common-interaction/src/main/java/com/jy/eletender/common/interaction/dto/TenderPackageCallbackRequest.java`
- 新建：`ele-tender-system/ele-tender-common-interaction/src/main/java/com/jy/eletender/common/interaction/dto/TenderEntryContext.java`
- 新建：`ele-tender-system/ele-tender-common-interaction/src/main/java/com/jy/eletender/common/interaction/spi/InteractionIdentityService.java`
- 新建：`ele-tender-system/ele-tender-common-interaction/src/main/java/com/jy/eletender/common/interaction/spi/InteractionProjectInfoService.java`
- 新建：`ele-tender-system/ele-tender-common-interaction/src/main/java/com/jy/eletender/common/interaction/spi/InteractionBidRecordSchemeService.java`
- 新建：`ele-tender-system/ele-tender-common-interaction/src/main/java/com/jy/eletender/common/interaction/spi/InteractionTenderPdfReceiveService.java`
- 新建：`ele-tender-system/ele-tender-common-interaction/src/main/java/com/jy/eletender/common/interaction/spi/InteractionTenderPackageReceiveService.java`
- 新建：`ele-tender-system/ele-tender-common-interaction/src/main/java/com/jy/eletender/common/interaction/spi/InteractionEventLogger.java`
- 新建：`ele-tender-system/ele-tender-common-interaction/src/test/java/com/jy/eletender/common/interaction/InteractionApiPathsTest.java`

**步骤 1：先写失败测试**

创建 `InteractionApiPathsTest`：

```java
@Test
void shouldExposeFixedControllerPrefix() {
    assertThat(InteractionApiPaths.BASE).isEqualTo("/api/eleTender/interaction");
    assertThat(InteractionApiPaths.IDENTITY_CURRENT).isEqualTo("/api/eleTender/interaction/identity/current");
}
```

**步骤 2：运行测试确认失败**

执行：

```bash
cd ele-tender-system && mvn -pl ele-tender-common-interaction -am -Dtest=InteractionApiPathsTest test
```

预期：失败，因为常量类还不存在。

**步骤 3：实现协议层**

固定 API 路径常量，保证接口控制权在平台侧：

```java
public final class InteractionApiPaths {
    public static final String BASE = "/api/eleTender/interaction";
    public static final String IDENTITY_CURRENT = BASE + "/identity/current";
    public static final String PROJECT_BASIC_INFO = BASE + "/projects/basic-info";
    public static final String BID_RECORD_SCHEME = BASE + "/bid-record-schemes/query";
    public static final String CALLBACK_TENDER_PDF = BASE + "/callbacks/tender-pdf";
    public static final String CALLBACK_TENDER_PACKAGE = BASE + "/callbacks/tender-package";
    private InteractionApiPaths() {}
}
```

统一业务类型枚举：

```java
public enum InteractionBizType {
    PROJECT(1, "立项"),
    CLARIFICATION(2, "答疑");
}
```

请求 DTO 要对业务键做基础校验：

```java
@Data
public class ProjectBasicInfoQueryRequest {
    @NotNull
    private Integer bizType;
    @NotBlank
    private String bizId;
    @NotBlank
    private String projectId;
    @NotBlank
    private String tenderId;
}
```

SPI 设计要尽量窄，降低业务系统实现成本：

```java
public interface InteractionIdentityService {
    IdentityQueryResponse queryCurrentIdentity(IdentityContext context);
}

public interface InteractionProjectInfoService {
    ProjectBasicInfoResponse queryProjectBasicInfo(ProjectBasicInfoQueryRequest request);
}
```

增加可选扩展点 `InteractionEventLogger`，用于让宿主系统接管交互事件日志：

```java
public interface InteractionEventLogger {
    void logInbound(String apiName, Object request, Object response, Throwable error);
    void logOutbound(String apiName, Object request, Object response, Throwable error);
}
```

**步骤 4：重新执行测试**

执行：

```bash
cd ele-tender-system && mvn -pl ele-tender-common-interaction -am -Dtest=InteractionApiPathsTest test
```

预期：通过。

**步骤 5：提交**

```bash
git add ele-tender-system/ele-tender-common-interaction
git commit -m "feat: define interaction protocol and spi contracts"
```

### 任务 4：在 `interaction-core` 中实现出站交互 client

**文件：**
- 新建：`ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/src/main/java/com/jy/eletender/interaction/core/properties/EleTenderInteractionProperties.java`
- 新建：`ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/src/main/java/com/jy/eletender/interaction/core/support/InteractionRequestSigner.java`
- 新建：`ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/src/main/java/com/jy/eletender/interaction/core/support/InteractionRestTemplateFactory.java`
- 新建：`ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/src/main/java/com/jy/eletender/interaction/core/support/OutboundLogInterceptor.java`
- 新建：`ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/src/main/java/com/jy/eletender/interaction/core/client/ExternalAuthClient.java`
- 新建：`ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/src/main/java/com/jy/eletender/interaction/core/client/ExternalUserInfoClient.java`
- 新建：`ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/src/main/java/com/jy/eletender/interaction/core/client/TenderDocumentEntryUrlBuilder.java`
- 新建：`ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/src/main/java/com/jy/eletender/interaction/core/client/EleTenderInteractionClient.java`
- 新建：`ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/src/test/java/com/jy/eletender/interaction/core/client/ExternalAuthClientTest.java`
- 新建：`ele-tender-system/ele-tender-interaction/ele-tender-interaction-core/src/test/java/com/jy/eletender/interaction/core/client/TenderDocumentEntryUrlBuilderTest.java`

**步骤 1：先写失败测试**

`ExternalAuthClientTest`

```java
@Test
void shouldCallExternalTokenEndpointWithSignedHeaders() {
    MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
    server.expect(requestTo("http://localhost:8080/api/external/token"))
            .andExpect(header("X-App-Key", "demo-key"))
            .andExpect(header("X-Timestamp", notNullValue()))
            .andExpect(header("X-Signature", notNullValue()))
            .andRespond(withSuccess("{\"code\":200,\"data\":{\"token\":\"abc\",\"expireIn\":1800}}", MediaType.APPLICATION_JSON));

    ExternalTokenResponse response = client.getExternalToken(request);
    assertThat(response.getToken()).isEqualTo("abc");
}
```

`TenderDocumentEntryUrlBuilderTest`

```java
@Test
void shouldBuildEntryUrlWithEncodedBusinessParameters() {
    TenderEntryContext context = new TenderEntryContext();
    context.setBizType(1);
    context.setBizId("BIZ-1");
    context.setProjectId("P-1");
    context.setTenderId("T-1");
    context.setToken("token-123");

    String url = builder.build(context);

    assertThat(url).contains("bizType=1");
    assertThat(url).contains("bizId=BIZ-1");
    assertThat(url).contains("projectId=P-1");
    assertThat(url).contains("tenderId=T-1");
    assertThat(url).contains("token=token-123");
}
```

**步骤 2：运行测试确认失败**

执行：

```bash
cd ele-tender-system && mvn -pl ele-tender-interaction/ele-tender-interaction-core -am -Dtest=ExternalAuthClientTest,TenderDocumentEntryUrlBuilderTest test
```

预期：失败，因为 core client 尚未实现。

**步骤 3：实现核心 client**

使用统一配置类：

```java
@Data
@ConfigurationProperties(prefix = "ele-tender.interaction")
public class EleTenderInteractionProperties {
    private boolean enabled = true;
    private String baseUrl;
    private String appKey;
    private String appSecret;
    private String tokenPath = "/api/external/token";
    private String userInfoPath = "/api/external/userinfo";
    private String tenderDocumentPagePath = "/tender-document/compose";
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(20);
}
```

出站请求签名直接复用现有 `SignatureUtil`：

```java
public HttpHeaders sign(HttpHeaders headers) {
    long timestamp = System.currentTimeMillis();
    headers.set("X-App-Key", properties.getAppKey());
    headers.set("X-Timestamp", String.valueOf(timestamp));
    headers.set("X-Signature", SignatureUtil.generateSignature(properties.getAppKey(), timestamp, properties.getAppSecret()));
    return headers;
}
```

本项目优先使用 `RestTemplate`，不引入 `WebClient`，原因：
- 现有项目没有响应式基础设施；
- `MockRestServiceServer` 下测试更短；
- 当前交互全部是同步调用。

`ExternalUserInfoClient` 需要把当前 `Authorization` 头转发给 `ele-tender-support`，用于通过当前电子标 token 获取外部用户信息：

```java
public ExternalUserInfoResponse getCurrentExternalUser(String authorization) {
    HttpHeaders headers = signer.sign(new HttpHeaders());
    headers.set(HttpHeaders.AUTHORIZATION, authorization);
    HttpEntity<Void> entity = new HttpEntity<>(headers);
    return restTemplate.exchange(url("/api/external/userinfo"), HttpMethod.GET, entity, new ParameterizedTypeReference<Result<ExternalUserInfoResponse>>() {})
            .getBody()
            .getData();
}
```

统一对外暴露一个 facade：

```java
public class EleTenderInteractionClient {
    public ExternalTokenResponse getExternalToken(ExternalTokenRequest request) { ... }
    public ExternalUserInfoResponse getCurrentExternalUser(String authorization) { ... }
    public String buildTenderDocumentEntryUrl(TenderEntryContext context) { ... }
}
```

**步骤 4：重新执行测试**

执行：

```bash
cd ele-tender-system && mvn -pl ele-tender-interaction/ele-tender-interaction-core -am -Dtest=ExternalAuthClientTest,TenderDocumentEntryUrlBuilderTest test
```

预期：通过。

**步骤 5：提交**

```bash
git add ele-tender-system/ele-tender-interaction/ele-tender-interaction-core
git commit -m "feat: add outbound interaction client core"
```

### 任务 5：在 starter 中实现自动装配与标准入站 controller

**文件：**
- 新建：`ele-tender-system/ele-tender-interaction/ele-tender-interaction-autoconfigure/src/main/java/com/jy/eletender/interaction/autoconfigure/EleTenderInteractionAutoConfiguration.java`
- 新建：`ele-tender-system/ele-tender-interaction/ele-tender-interaction-autoconfigure/src/main/java/com/jy/eletender/interaction/autoconfigure/InteractionControllerProperties.java`
- 新建：`ele-tender-system/ele-tender-interaction/ele-tender-interaction-autoconfigure/src/main/java/com/jy/eletender/interaction/autoconfigure/web/InteractionSignatureInterceptor.java`
- 新建：`ele-tender-system/ele-tender-interaction/ele-tender-interaction-autoconfigure/src/main/java/com/jy/eletender/interaction/autoconfigure/web/InteractionWebMvcConfigurer.java`
- 新建：`ele-tender-system/ele-tender-interaction/ele-tender-interaction-autoconfigure/src/main/java/com/jy/eletender/interaction/autoconfigure/controller/InteractionIdentityController.java`
- 新建：`ele-tender-system/ele-tender-interaction/ele-tender-interaction-autoconfigure/src/main/java/com/jy/eletender/interaction/autoconfigure/controller/InteractionProjectInfoController.java`
- 新建：`ele-tender-system/ele-tender-interaction/ele-tender-interaction-autoconfigure/src/main/java/com/jy/eletender/interaction/autoconfigure/controller/InteractionBidRecordSchemeController.java`
- 新建：`ele-tender-system/ele-tender-interaction/ele-tender-interaction-autoconfigure/src/main/java/com/jy/eletender/interaction/autoconfigure/controller/InteractionTenderPdfCallbackController.java`
- 新建：`ele-tender-system/ele-tender-interaction/ele-tender-interaction-autoconfigure/src/main/java/com/jy/eletender/interaction/autoconfigure/controller/InteractionTenderPackageCallbackController.java`
- 新建：`ele-tender-system/ele-tender-interaction/ele-tender-interaction-autoconfigure/src/main/java/com/jy/eletender/interaction/autoconfigure/handler/InteractionGlobalExceptionHandler.java`
- 新建：`ele-tender-system/ele-tender-interaction/ele-tender-interaction-autoconfigure/src/test/java/com/jy/eletender/interaction/autoconfigure/controller/InteractionIdentityControllerTest.java`
- 新建：`ele-tender-system/ele-tender-interaction/ele-tender-interaction-autoconfigure/src/test/java/com/jy/eletender/interaction/autoconfigure/EleTenderInteractionAutoConfigurationTest.java`
- 新建：`ele-tender-system/ele-tender-interaction/ele-tender-interaction-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

**步骤 1：先写失败测试**

`InteractionIdentityControllerTest`

```java
@Test
void shouldResolveCurrentIdentityFromCurrentElectronicTenderToken() throws Exception {
    when(client.getCurrentExternalUser("Bearer demo-token")).thenReturn(externalUserInfo());
    when(identityService.queryCurrentIdentity(any())).thenReturn(identityResponse());

    mockMvc.perform(get("/api/eleTender/interaction/identity/current")
            .header("Authorization", "Bearer demo-token")
            .header("X-App-Key", "demo-key")
            .header("X-Timestamp", String.valueOf(System.currentTimeMillis()))
            .header("X-Signature", validSignature()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.userId").value("U-100"));
}
```

**步骤 2：运行测试确认失败**

执行：

```bash
cd ele-tender-system && mvn -pl ele-tender-interaction/ele-tender-interaction-autoconfigure -am -Dtest=InteractionIdentityControllerTest test
```

预期：失败，因为 controller 和自动装配尚未存在。

**步骤 3：实现自动装配与 controller**

自动装配仅在启用时生效：

```java
@AutoConfiguration
@EnableConfigurationProperties({EleTenderInteractionProperties.class, InteractionControllerProperties.class})
@ConditionalOnProperty(prefix = "ele-tender.interaction", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EleTenderInteractionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public InteractionSignatureInterceptor interactionSignatureInterceptor(EleTenderInteractionProperties properties) {
        return new InteractionSignatureInterceptor(properties);
    }
}
```

对 starter 暴露的全部接口统一加签名校验拦截器：

```java
public class InteractionSignatureInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String appKey = request.getHeader("X-App-Key");
        long timestamp = Long.parseLong(request.getHeader("X-Timestamp"));
        String signature = request.getHeader("X-Signature");
        if (!properties.getAppKey().equals(appKey)
                || !SignatureUtil.verifySignature(appKey, timestamp, properties.getAppSecret(), signature)) {
            throw new BusinessException(ResponseCode.SIGNATURE_ERROR);
        }
        return true;
    }
}
```

身份查询 controller 处理流程：
- 必须携带 `Authorization`；
- 先调用 `EleTenderInteractionClient.getCurrentExternalUser(authorization)` 到 `ele-tender-support` 校验当前电子标 token；
- 将外部用户信息转换为 `IdentityContext`；
- 再委托给业务系统实现的 SPI。

controller 骨架：

```java
@RestController
public class InteractionIdentityController {

    @GetMapping(InteractionApiPaths.IDENTITY_CURRENT)
    public Result<IdentityQueryResponse> queryCurrentIdentity(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        ExternalUserInfoResponse externalUser = interactionClient.getCurrentExternalUser(authorization);
        IdentityContext context = IdentityContext.from(externalUser);
        return Result.success(identityService.queryCurrentIdentity(context));
    }
}
```

回传 controller 要保持同步、轻薄：

```java
@PostMapping(InteractionApiPaths.CALLBACK_TENDER_PDF)
public Result<Void> receiveTenderPdf(@Valid @RequestBody TenderPdfCallbackRequest request) {
    tenderPdfReceiveService.receive(request);
    return Result.success();
}
```

同时提供 starter 内部的全局异常处理，保证宿主系统无需单独再为这些接口写一套 `Result<T>` 封装。

**步骤 4：重新执行测试**

执行：

```bash
cd ele-tender-system && mvn -pl ele-tender-interaction/ele-tender-interaction-autoconfigure -am -Dtest=InteractionIdentityControllerTest,EleTenderInteractionAutoConfigurationTest test
```

预期：通过。

**步骤 5：提交**

```bash
git add ele-tender-system/ele-tender-interaction/ele-tender-interaction-autoconfigure
git commit -m "feat: auto-configure interaction controllers and signature checks"
```

### 任务 6：打包 starter 并补业务系统接入文档

**文件：**
- 修改：`ele-tender-system/ele-tender-interaction/ele-tender-interaction-spring-boot-starter/pom.xml`
- 新建：`ele-tender-system/ele-tender-interaction/ele-tender-interaction-spring-boot-starter/src/test/java/com/jy/eletender/interaction/starter/StarterSmokeTest.java`
- 新建：`ele-tender-system/ele-tender-interaction/README.md`

**步骤 1：先写失败测试**

```java
class StarterSmokeTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EleTenderInteractionAutoConfiguration.class))
            .withUserConfiguration(TestSpiConfiguration.class)
            .withPropertyValues(
                    "ele-tender.interaction.base-url=http://localhost:8080",
                    "ele-tender.interaction.app-key=demo-key",
                    "ele-tender.interaction.app-secret=demo-secret");

    @Test
    void shouldLoadStarterBeansWhenSpiImplementationsExist() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(EleTenderInteractionClient.class);
            assertThat(context).hasBean("interactionIdentityController");
        });
    }
}
```

**步骤 2：运行测试确认失败**

执行：

```bash
cd ele-tender-system && mvn -pl ele-tender-interaction/ele-tender-interaction-spring-boot-starter -am -Dtest=StarterSmokeTest test
```

预期：失败，因为 starter POM 还未正确组装。

**步骤 3：组装 starter**

starter POM 只依赖另外两个交互模块：

```xml
<dependencies>
    <dependency>
        <groupId>com.jy.eletender</groupId>
        <artifactId>ele-tender-interaction-core</artifactId>
    </dependency>
    <dependency>
        <groupId>com.jy.eletender</groupId>
        <artifactId>ele-tender-interaction-autoconfigure</artifactId>
    </dependency>
</dependencies>
```

在 `ele-tender-system/ele-tender-interaction/README.md` 中写清楚业务系统接入方法：
- starter 的 Maven 依赖坐标；
- 必填配置项；
- 固定入站接口前缀 `/api/eleTender/interaction/*`；
- 业务系统必须实现的 5 个 SPI；
- 获取 token 后拼装编制页面跳转地址的示例。

README 示例代码：

```java
ExternalTokenResponse token = eleTenderInteractionClient.getExternalToken(request);
TenderEntryContext context = new TenderEntryContext();
context.setBizType(1);
context.setBizId("BIZ-100");
context.setProjectId("P-100");
context.setTenderId("T-100");
context.setToken(token.getToken());
String url = eleTenderInteractionClient.buildTenderDocumentEntryUrl(context);
```

**步骤 4：重新执行测试**

执行：

```bash
cd ele-tender-system && mvn -pl ele-tender-interaction/ele-tender-interaction-spring-boot-starter -am -Dtest=StarterSmokeTest test
```

预期：通过。

**步骤 5：提交**

```bash
git add ele-tender-system/ele-tender-interaction
git commit -m "feat: package interaction spring boot starter"
```

### 任务 7：为 `ele-tender-support` 中复用的外部认证接口补契约测试

**文件：**
- 新建：`ele-tender-system/ele-tender-support/src/test/java/com/jy/eletender/support/controller/ExternalAuthControllerTest.java`
- 修改：`ele-tender-system/ele-tender-support/src/main/resources/application.yml`

**步骤 1：先写失败测试**

创建 `ExternalAuthControllerTest`，使用 mock 的 `IAuthService`：

```java
@WebMvcTest(ExternalAuthController.class)
class ExternalAuthControllerTest {

    @MockBean
    private IAuthService authService;

    @MockBean
    private Validator validator;

    @Test
    void shouldReturnExternalTokenWhenSignatureIsValid() throws Exception {
        when(authService.verifyExternalSignature(anyString(), anyLong(), anyString())).thenReturn(true);
        when(authService.getExternalToken(anyString(), any())).thenReturn(new ExternalTokenResponse("abc", 1800L));

        mockMvc.perform(post("/api/external/token")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-App-Key", "demo-key")
                .header("X-Timestamp", System.currentTimeMillis())
                .header("X-Signature", "demo-signature")
                .content("{\"userId\":\"U1\",\"userName\":\"张三\",\"enterpriseId\":\"E1\",\"enterpriseName\":\"企业A\",\"enterpriseCode\":\"QY001\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.token").value("abc"));
    }
}
```

同时再补一个 `/api/external/userinfo` 的契约测试，锁住 starter 依赖的行为。

**步骤 2：运行测试确认失败**

执行：

```bash
cd ele-tender-system && mvn -pl ele-tender-support -am -Dtest=ExternalAuthControllerTest test
```

预期：失败，因为测试还未创建。

**步骤 3：实现测试并保持日志配置一致**

正常情况下不改 controller 行为，除非测试暴露出真实缺口。如果 `/api/external/userinfo` 在切片测试中不方便构造安全上下文，则补最小 mock 和上下文注入，不要随意改接口形态。

如果 `application.yml` 在并行开发中被改动，要保持它与任务 2 的 trace 日志格式一致。

**步骤 4：重新执行测试**

执行：

```bash
cd ele-tender-system && mvn -pl ele-tender-support -am -Dtest=ExternalAuthControllerTest test
```

预期：通过。

**步骤 5：提交**

```bash
git add ele-tender-system/ele-tender-support
git commit -m "test: lock external auth contract for interaction starter"
```

### 任务 8：执行一期验收测试并补最终接入说明

**文件：**
- 修改：`ele-tender-system/ele-tender-interaction/README.md`

**步骤 1：执行聚焦模块测试**

执行：

```bash
cd ele-tender-system
mvn -pl ele-tender-common,ele-tender-common-interaction,ele-tender-interaction/ele-tender-interaction-core,ele-tender-interaction/ele-tender-interaction-autoconfigure,ele-tender-interaction/ele-tender-interaction-spring-boot-starter,ele-tender-support,ele-tender-tender-document -am test
```

预期：通过。

**步骤 2：执行全量 reactor 测试**

执行：

```bash
cd ele-tender-system && mvn clean test
```

预期：通过。

**步骤 3：在 README 中补经过验证的接入清单**

补充一段简短清单，至少覆盖：
- 业务系统引入 starter 依赖；
- 配置 `ele-tender.interaction.base-url`、`app-key`、`app-secret`；
- 实现 5 个 SPI；
- 使用 starter client 获取 token 并构造跳转 URL；
- 入站接口统一暴露在 `/api/eleTender/interaction/*`；
- `support`、`file`、`tender-document` 与宿主系统日志中均可看到 traceId。

**步骤 4：提交**

```bash
git add ele-tender-system/ele-tender-interaction/README.md
git commit -m "docs: finalize interaction phase 1 verification notes"
```

## 范围说明

- 本期 **不** 实现 `ele-tender-tender-document` 的具体业务流转。
- 本期 **不** 落地回调失败后的自动重试调度表，仅保留同步回调与手工重试扩展点。
- 本期 **不** 让 `ele-tender-common` 反向依赖 `ele-tender-common-interaction`。

## 执行提醒

- 每完成一个任务就运行对应测试，不要把所有模块一次性写完再回头修。
- 所有新接口都必须保持 `Result<T>` 返回结构。
- 所有新增签名校验都必须复用现有 `SignatureUtil`，避免同仓库出现两套算法。
- 所有日志增强都必须考虑脱敏，不能直接把 token、签名值、文件内容打到日志里。
