# Interaction FileUploadClient 与 FileClient 合并设计

## 1. 背景

当前 `ele-tender-interaction` 模块封装了文件查询（`FileInfoClient`）和文件下载（`FileDownloadClient`），但缺少文件上传能力。业务系统在投标文件预存流程中需要自行对接文件服务 `POST /api/file/upload` 上传文件，再调用 `pushBidDocument` 推送预存。

交互系统尚未正式上线，无兼容负担，可借此机会将三个文件相关 Client 合并为统一的 `FileClient`。

## 2. 目标

1. 新增文件上传能力，封装为 interaction 出站 Client
2. 合并 `FileInfoClient` + `FileDownloadClient` + 新上传能力 → `FileClient`
3. 新增 `InteractionFileUploadResponse` DTO（放 `common-interaction`）
4. 交互系统版本号升级至 `1.1.3-SNAPSHOT`
5. `docs/guides/` 全部添加版本号 + 变更记录
6. 全 `docs/` 检查并更新与新功能相关的文档

## 3. 设计

### 3.1 FileClient

替代 `FileInfoClient` 和 `FileDownloadClient`，新增上传方法。

```java
package com.jy.eletender.interaction.core.client;

@Slf4j
public class FileClient {

    private final RestTemplate restTemplate;
    private final EleTenderInteractionProperties properties;
    private final InteractionRequestSigner signer;

    // === 保留原有能力 ===

    public InteractionFileInfoResponse getFileInfo(Long fileId);
    public InteractionFileDownloadResponse downloadFile(Long fileId);

    // === 新增上传 ===

    /**
     * 通过 byte[] 上传文件。
     * @param content  文件内容
     * @param fileName 文件名
     * @param bizType  业务类型（由业务系统指定）
     */
    public InteractionFileUploadResponse uploadFile(byte[] content, String fileName, String bizType);

    /**
     * 通过本地文件路径上传文件。
     * 内部读取文件后委托给 byte[] 版本，文件名从 Path 自动提取。
     * @param filePath 本地文件路径（必须存在且可读）
     * @param bizType  业务类型
     */
    public InteractionFileUploadResponse uploadFile(Path filePath, String bizType);

    private String resolveFileBaseUrl(); // 复用现有逻辑
}
```

**上传实现要点：**
- 构造 `MultiValueMap`，放入 `ByteArrayResource`（重写 `getFilename()`）+ `bizType`
- Content-Type 设为 `multipart/form-data`
- 使用 `InteractionRequestSigner` 签名（仅签名，不需要 authorization token）
- 超时沿用全局 `connectTimeout` / `readTimeout` 配置
- `uploadFile(Path)` 委托给 `uploadFile(byte[], String, String)`

**删除的类：**
- `FileInfoClient`
- `FileDownloadClient`

### 3.2 InteractionFileUploadResponse

放 `ele-tender-common-interaction`：

```java
package com.jy.eletender.common.interaction.dto;

public class InteractionFileUploadResponse {
    private Long fileId;
    private String fileName;
    private Long fileSize;
    private String fileSha256;
}
```

### 3.3 EleTenderInteractionProperties 新增配置

```java
private String fileUploadPath = "/api/file/upload";
```

### 3.4 EleTenderInteractionClient 改造

**构造器变更：** 原来注入 `FileInfoClient` + `FileDownloadClient`，改为注入单个 `FileClient`。

**方法变更：**
- 保留 `getFileInfo(Long)` 和 `downloadFile(Long)`，内部改调 `FileClient`
- 新增 `uploadFile(byte[], String, String)` 和 `uploadFile(Path, String)`

### 3.5 自动配置变更

`EleTenderInteractionAutoConfiguration`：
- 删除 `FileInfoClient` 和 `FileDownloadClient` 两个 `@Bean`
- 新增 `FileClient` 的 `@Bean`（`@ConditionalOnMissingBean`）
- `EleTenderInteractionClient` 构造调用同步修改

### 3.6 认证方式

文件服务 `POST /api/file/upload` 无认证要求（JWT 过滤器仅注册在 `/api/file/esign/*`）。FileClient 上传仅需 `InteractionRequestSigner` 签名，与 getFileInfo / downloadFile 一致。

## 4. 版本号升级

交互系统相关模块从 `1.1.2-SNAPSHOT` 升级至 `1.1.3-SNAPSHOT`：
- 父 POM `interaction.version` 属性
- `ele-tender-common-interaction/pom.xml`
- `ele-tender-interaction/pom.xml`（含所有子模块）

## 5. 文档更新

### 5.1 `docs/guides/` 添加版本（全部 7 个文件）

统一在头部添加版本信息和变更记录：

```markdown
# 标题

> **适用版本**: x.x.x
> **最后更新**: YYYY-MM-DD

## 变更记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| x.x.x | YYYY-MM-DD | 初始版本 |

---

... 正文 ...
```

已有"最后更新"元数据的文件统一迁移到新格式。OpenAPI yaml 文件保持 `info.version` 不变，额外在头部添加 changelog 注释块。

### 5.2 内容更新

| 文档 | 更新内容 |
|------|----------|
| `docs/guides/业务系统接入手册.md` | 新增 uploadFile 能力说明；FileClient 合并说明；配置项 fileUploadPath；集成顺序更新 |
| `docs/rules/INTERACTION_INTEGRATION_SPEC.md` | §6 出站客户端能力新增"上传文件"；§7 配置项新增 file-upload-path |
| `docs/rules/ELE_TENDER_CRYPTO_SPEC.md` | 投标文件预存流程补充说明可通过 interaction 上传 |
| `docs/rules/FILE_SERVICE_SPEC.md` | 检查是否需要标注 interaction 封装了上传能力 |

`docs/plans/` 和 `docs/projects/` 历史文档不更新。

## 6. 测试

- `FileClientTest`：覆盖 getFileInfo、downloadFile、uploadFile(byte[])、uploadFile(Path) 四个方法
- 删除原 `FileInfoClientTest`、`FileDownloadClientTest`
- `EleTenderInteractionClient` 相关测试同步调整
