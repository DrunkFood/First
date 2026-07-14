# 文件服务API

<cite>
**本文引用的文件**   
- [FILE_SERVICE_SPEC.md](file://docs/rules/FILE_SERVICE_SPEC.md)
- [FileController.java](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/controller/FileController.java)
- [IFileStorageService.java](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/service/IFileStorageService.java)
- [IWordDocumentService.java](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/service/IWordDocumentService.java)
- [InternalFileServiceClient.java](file://ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/client/InternalFileServiceClient.java)
- [AiFileClient.java](file://ele-ai-tender-system/ele-ai-tender-interaction/ele-ai-tender-interaction-core/src/main/java/com/jy/eleaitender/interaction/core/client/AiFileClient.java)
- [EleAiTenderInteractionProperties.java](file://ele-ai-tender-system/ele-ai-tender-interaction/ele-ai-tender-interaction-core/src/main/java/com/jy/eleaitender/interaction/core/properties/EleAiTenderInteractionProperties.java)
- [InteractionValidationUtils.java](file://ele-ai-tender-system/ele-ai-tender-interaction/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/util/InteractionValidationUtils.java)
- [InteractionFileInfoResponse.java](file://ele-ai-tender-system/ele-ai-tender-interaction/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/InteractionFileInfoResponse.java)
- [FileInfo.java](file://ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/entity/file/FileInfo.java)
- [ele-ai-tender-file.yml](file://docs/guides/ele-ai-tender-file.yml)
</cite>

## 更新摘要
**变更内容**   
- 新增基于SHA256的文件信息查询接口 `/api/file/info/sha256/{sha256}`
- 增强客户端方法以支持字符串参数处理，确保系统内参数处理一致性
- 添加SHA256参数验证和响应对象支持
- 完善内部服务间调用的文件信息查询能力

## 目录
1. [简介](#简介)
2. [项目结构](#项目结构)
3. [核心组件](#核心组件)
4. [架构总览](#架构总览)
5. [详细接口说明](#详细接口说明)
6. [依赖关系分析](#依赖关系分析)
7. [性能与优化建议](#性能与优化建议)
8. [故障排查指南](#故障排查指南)
9. [结论](#结论)
10. [附录](#附录)

## 简介
本文件为"文件服务模块"的API接口文档，覆盖上传、下载、信息获取、删除、文本提取、模板生成、文档修复等能力。同时给出分片上传、断点续传、进度跟踪的实现思路与参数配置建议；说明多格式文档（Word、PDF、Markdown）转换接口的使用方式与批量处理策略；并提供大文件传输优化、存储策略配置与安全访问控制的使用指南；最后补充文件格式验证、病毒扫描与存储配额管理的调用方法建议。

**更新** 新增基于SHA256的内容哈希查询能力，允许客户端通过文件内容指纹而非传统文件ID检索文件元数据，提升去重和秒传功能。

## 项目结构
文件服务位于 ele-ai-tender-file 模块，对外暴露 /api/file 前缀的REST接口，由控制器统一接收请求并委派至存储服务与文档引擎服务。

```mermaid
graph TB
Client["客户端"] --> Controller["FileController<br/>/api/file/*"]
Controller --> StorageSvc["IFileStorageService<br/>文件存储接口"]
Controller --> DocSvc["IWordDocumentService<br/>文档处理接口"]
StorageSvc --> FS["文件系统/对象存储"]
DocSvc --> Engines["文档引擎(Word/Markdown/PDF)"]
```

**图示来源**   
- [FileController.java:40-176](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/controller/FileController.java#L40-L176)
- [IFileStorageService.java:1-65](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/service/IFileStorageService.java#L1-L65)
- [IWordDocumentService.java:1-50](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/service/IWordDocumentService.java#L1-L50)

**章节来源**   
- [FILE_SERVICE_SPEC.md:1-153](file://docs/rules/FILE_SERVICE_SPEC.md#L1-L153)
- [FileController.java:40-176](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/controller/FileController.java#L40-L176)

## 核心组件
- FileController：提供统一的HTTP入口，负责参数校验、权限注解、响应封装。
- IFileStorageService：定义上传、查询、路径解析、删除、按SHA-256查询等能力。
- IWordDocumentService：定义Word文档结构解析、模板生成、文本替换修复、文本+位置索引提取等能力。
- InternalFileServiceClient：内部服务间调用的文件服务客户端，支持多种文件操作。
- AiFileClient：外部交互层的文件客户端，支持基于SHA256的文件信息查询。

**章节来源**   
- [FileController.java:40-176](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/controller/FileController.java#L40-L176)
- [IFileStorageService.java:1-65](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/service/IFileStorageService.java#L1-L65)
- [IWordDocumentService.java:1-50](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/service/IWordDocumentService.java#L1-L50)
- [InternalFileServiceClient.java:25-362](file://ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/client/InternalFileServiceClient.java#L25-362)
- [AiFileClient.java:56-108](file://ele-ai-tender-system/ele-ai-tender-interaction/ele-ai-tender-interaction-core/src/main/java/com/jy/eleaitender/interaction/core/client/AiFileClient.java#L56-L108)

## 架构总览
下图展示典型请求在控制器与服务层之间的交互流程。

```mermaid
sequenceDiagram
participant C as "客户端"
participant Ctrl as "FileController"
participant Svc as "IFileStorageService/IWordDocumentService"
participant Store as "存储/引擎"
C->>Ctrl : "POST /api/file/upload"
Ctrl->>Svc : "upload(file, bizType)"
Svc->>Store : "持久化文件/计算摘要"
Store-->>Svc : "返回上传结果"
Svc-->>Ctrl : "FileUploadResponse"
Ctrl-->>C : "Result<FileUploadResponse>"
C->>Ctrl : "GET /api/file/download/{fileId}"
Ctrl->>Svc : "getById/getFilePath"
Svc-->>Ctrl : "FileInfo/路径"
Ctrl-->>C : "二进制流下载"
```

**图示来源**   
- [FileController.java:50-86](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/controller/FileController.java#L50-L86)
- [IFileStorageService.java:21-47](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/service/IFileStorageService.java#L21-L47)

## 详细接口说明

### 通用约定
- 基础路径：/api/file
- 鉴权：所有接口均要求登录（@RequireLogin），内部服务间调用使用特定JWT类型。
- 统一响应：Result<T> 包装，包含 code/message/data。
- 错误码：参考 ResponseCode 枚举（如 PARAM_ERROR、FILE_NOT_FOUND）。

**章节来源**   
- [FileController.java:40-176](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/controller/FileController.java#L40-L176)
- [FILE_SERVICE_SPEC.md:129-137](file://docs/rules/FILE_SERVICE_SPEC.md#L129-L137)

---

### 上传文件
- 方法：POST
- 路径：/api/file/upload
- 内容类型：multipart/form-data
- 表单字段
  - file：二进制文件
  - bizType：业务类型（必填）
- 成功响应：Result<FileUploadResponse>
- 失败响应：Result.fail(ResponseCode.PARAM_ERROR) 或业务异常
- 约束
  - 默认最大文件大小：500MB
  - 白名单后缀：.doc/.docx/.pdf/.txt/.md/.jar/.war/.zip/.tar.gz
  - 支持按 SHA-256 秒传（通过 getBySha256 查询复用）

**章节来源**   
- [FileController.java:50-58](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/controller/FileController.java#L50-L58)
- [IFileStorageService.java:21-31](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/service/IFileStorageService.java#L21-L31)
- [FILE_SERVICE_SPEC.md:110-121](file://docs/rules/FILE_SERVICE_SPEC.md#L110-L121)
- [FILE_SERVICE_SPEC.md:63-63](file://docs/rules/FILE_SERVICE_SPEC.md#L63-L63)

---

### 下载文件
- 方法：GET
- 路径：/api/file/download/{fileId}
- 路径参数
  - fileId：文件ID（Long）
- 成功响应：application/octet-stream 二进制流，Content-Disposition 带文件名
- 失败响应：抛出文件不存在异常（对应 FILE_NOT_FOUND）

**章节来源**   
- [FileController.java:60-86](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/controller/FileController.java#L60-L86)

---

### 获取文件信息（基于文件ID）
- 方法：GET
- 路径：/api/file/info/{fileId}
- 路径参数
  - fileId：文件ID（Long）
- 成功响应：Result<FileInfo>
- 失败响应：Result.fail(ResponseCode.FILE_NOT_FOUND)

**章节来源**   
- [FileController.java:86-95](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/controller/FileController.java#L86-L95)

---

### 获取文件信息（基于SHA256）
- 方法：GET
- 路径：/api/file/info/sha256/{sha256}
- 路径参数
  - sha256：文件SHA-256哈希值（String，必填）
- 成功响应：Result<FileInfo>
- 失败响应：Result.fail(ResponseCode.FILE_NOT_FOUND)
- 用途：通过文件内容指纹查询文件元数据，支持秒传和去重功能

**更新** 新增接口，允许客户端使用文件内容的SHA-256哈希值直接查询文件信息，无需先上传即可检查文件是否存在。

**章节来源**   
- [FileController.java:97-106](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/controller/FileController.java#L97-L106)
- [IFileStorageService.java:57-63](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/service/IFileStorageService.java#L57-L63)

---

### 删除文件
- 方法：DELETE
- 路径：/api/file/delete/{fileId}
- 路径参数
  - fileId：文件ID（Long）
- 行为
  - 若存在记录，校验文件归属（创建者ID）
- 成功响应：Result<Boolean>
- 失败响应：未命中记录时不抛异常，直接执行删除逻辑（具体实现以服务为准）

**章节来源**   
- [FileController.java:108-119](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/controller/FileController.java#L108-L119)

---

### 获取Word文档结构
- 方法：GET
- 路径：/api/file/structure/{fileId}
- 路径参数
  - fileId：文件ID（Long）
- 成功响应：Result<WordStructureVO>
- 用途：用于模板占位符与结构解析（例如表格、循环、条件等）

**章节来源**   
- [FileController.java:121-126](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/controller/FileController.java#L121-L126)
- [IWordDocumentService.java:21-21](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/service/IWordDocumentService.java#L21-L21)

---

### 基于模板生成文档
- 方法：POST
- 路径：/api/file/generate-doc
- 请求体（JSON）
  - templateFileId：模板文件ID（Long，必填）
  - fileName：生成文件名（可选）
  - fillDataList：填充数据列表（List<Map<String,Object>>，必填）
- 成功响应：Result<Long>（生成的文件ID）
- 失败响应：Result.fail(ResponseCode.PARAM_ERROR)

**章节来源**   
- [FileController.java:128-142](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/controller/FileController.java#L128-L142)
- [IWordDocumentService.java:31-31](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/service/IWordDocumentService.java#L31-L31)

---

### 修复Word文档（替换文本）
- 方法：POST
- 路径：/api/file/fix-doc
- 请求体（JSON）
  - fileId：原始文件ID（Long，必填）
  - replacements：替换列表（List<Map<String,Object>>，必填）
    - original：原文本（String）
    - targeted：目标文本（String）
    - locationRef：可选，精准定位对象（Map）
      - type：元素类型（String）
      - elementIndex：元素序号（Integer）
      - tableIndex：表格序号（Integer，可选）
      - rowIndex：行号（Integer，可选）
      - cellIndex：单元格序号（Integer，可选）
- 成功响应：Result<WordFixResultVO>
- 失败响应：Result.fail(ResponseCode.PARAM_ERROR)

**章节来源**   
- [FileController.java:144-175](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/controller/FileController.java#L144-L175)
- [IWordDocumentService.java:40-40](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/service/IWordDocumentService.java#L40-L40)

---

### 提取Word文档文本+位置索引
- 方法：POST
- 路径：/api/file/extract-text
- 请求体（JSON）
  - fileId：文件ID（Long，必填）
- 成功响应：Result<Map<String,Object>>（包含 fullText 与 segments）
- 失败响应：Result.fail(ResponseCode.PARAM_ERROR)

**章节来源**   
- [FileController.java:177-186](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/controller/FileController.java#L177-L186)
- [IWordDocumentService.java:48-48](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/service/IWordDocumentService.java#L48-L48)

---

### OpenAPI 参考
- 路径清单与示例可在以下文件中查看：
  - /api/file/esign/upload
  - /api/file/upload
  - /api/file/download/{fileId}
  - /api/file/info/{fileId}
  - /api/file/info/sha256/{sha256}
  - /api/file/delete/{fileId}

**更新** 新增 `/api/file/info/sha256/{sha256}` 端点支持。

**章节来源**   
- [ele-ai-tender-file.yml:1-62](file://docs/guides/ele-ai-tender-file.yml#L1-L62)

## 依赖关系分析
- 控制器依赖两个服务接口：文件存储服务与文档服务。
- 文件存储服务提供上传、查询、路径解析、删除、按SHA-256查询等能力。
- 文档服务提供Word结构解析、模板生成、文本替换修复、文本+位置索引提取。
- 内部客户端支持字符串参数处理，确保跨系统调用的一致性。

```mermaid
classDiagram
class FileController {
+upload()
+download()
+info()
+sha256()
+delete()
+getFileStructure()
+generateDocument()
+fixDocument()
+extractText()
}
class IFileStorageService {
+upload()
+uploadFromBytes()
+getById()
+getFilePath()
+delete()
+getBySha256()
}
class IWordDocumentService {
+getFileStructure()
+generateDocument()
+fixDocument()
+extractText()
}
class InternalFileServiceClient {
+upload()
+download()
+info()
+getFileStructure()
+generateDocument()
+fixDocument()
+extractText()
}
class AiFileClient {
+getFileInfo()
+getFileInfoSha256()
+downloadFile()
+uploadFile()
}
FileController --> IFileStorageService : "依赖"
FileController --> IWordDocumentService : "依赖"
InternalFileServiceClient --> FileController : "内部调用"
AiFileClient --> FileController : "外部调用"
```

**图示来源**   
- [FileController.java:40-186](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/controller/FileController.java#L40-L186)
- [IFileStorageService.java:1-65](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/service/IFileStorageService.java#L1-L65)
- [IWordDocumentService.java:1-50](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/service/IWordDocumentService.java#L1-L50)
- [InternalFileServiceClient.java:25-362](file://ele-ai-tender-system/ele-ai-tender-common/src/main/java/com/jy/eleaitender/common/client/InternalFileServiceClient.java#L25-362)
- [AiFileClient.java:56-108](file://ele-ai-tender-system/ele-ai-tender-interaction/ele-ai-tender-interaction-core/src/main/java/com/jy/eleaitender/interaction/core/client/AiFileClient.java#L56-L108)

**章节来源**   
- [FILE_SERVICE_SPEC.md:129-137](file://docs/rules/FILE_SERVICE_SPEC.md#L129-L137)

## 性能与优化建议

- 分片上传与断点续传（实现建议）
  - 初始化分片：POST /api/file/initiate-upload?bizType=...&fileName=...&totalChunks=...
  - 上传分片：POST /api/file/upload-chunk?fileId=...&chunkIndex=...&chunkMd5=...
  - 合并分片：POST /api/file/complete-upload?fileId=...
  - 状态查询：GET /api/file/upload-status?fileId=...
  - 进度上报：服务端维护 Redis 键值，客户端轮询 GET /api/file/upload-progress?fileId=...
  - 注意：当前仓库未提供上述端点，可按此规范在服务端扩展。

- 大文件传输优化
  - 启用分块读取与流式写入，避免一次性加载到内存。
  - 合理设置服务器上传大小限制（默认500MB），必要时按业务调整。
  - 使用断点续传减少网络抖动影响。

- 存储策略配置
  - 本地磁盘：配置 base-path 并确保写权限。
  - 对象存储：可对接云厂商SDK，按 bizType 划分桶/前缀。
  - 命名规则：建议使用 fileId 作为对外标识，内部路径采用哈希分片目录。

- 安全访问控制
  - 外部调用需携带登录态（@RequireLogin）。
  - 内部服务间调用使用 TOKEN_TYPE_SERVICE 类型的JWT，密钥复用 APP_JWT_SECRET。
  - 删除操作需校验文件归属（创建者ID）。

- 并发与限流
  - 对上传/下载接口增加速率限制与并发上限。
  - 使用线程池隔离CPU密集型任务（如文档生成、文本提取）。

[本节为通用指导，无需代码引用]

## 故障排查指南

- 上传失败
  - 检查文件扩展名是否在白名单内。
  - 检查文件大小是否超过限制。
  - 检查 file.storage.base-path 目录是否存在且具备写权限。

- 下载404
  - 确认 file_info.file_path 对应的物理文件是否存在。
  - 检查 file.storage.base-path 配置是否正确。

- SHA256查询失败
  - 确认传入的SHA256哈希值格式正确（32位十六进制字符串）。
  - 检查文件是否已上传并计算了正确的SHA256值。
  - 验证客户端与服务端的SHA256计算算法一致（UTF-8字节序列）。

- fileSha256 校验失败
  - 确认调用方与服务端计算方式一致（UTF-8字节序列，十六进制字符串）。

- locationRef 定位失败
  - 检查 elementIndex 是否越界（段落/表格共享索引空间）。
  - 确认 WordTextExtractor 已执行。
  - 关注 DetectionResultParser.fillLocationRefs() 日志是否报匹配失败。

- 检测修复替换了错误位置
  - 检查 FixReplacement.locationRef 是否为空（为空走全文档替换会替换所有匹配项）。
  - 检查 elementIndex 对应的 IBodyElement 类型是否与 type 字段一致。

**更新** 新增SHA256查询失败的排查指南。

**章节来源**   
- [FILE_SERVICE_SPEC.md:139-146](file://docs/rules/FILE_SERVICE_SPEC.md#L139-L146)

## 结论
文件服务提供了完整的文件生命周期管理与文档处理能力，涵盖上传、下载、信息查询、删除、Word结构解析、模板生成、文本替换修复与文本+位置索引提取。**更新** 新增的基于SHA256的文件信息查询能力进一步增强了系统的去重和秒传功能，提升了用户体验和存储效率。结合分片上传、断点续传、进度跟踪与大文件优化策略，可满足高吞吐与高可用的生产场景。建议在现有接口基础上按需扩展分片与进度相关端点，并完善病毒扫描与配额管理策略。

[本节为总结性内容，无需代码引用]

## 附录

### 接口一览
- POST /api/file/upload — 上传文件
- GET /api/file/download/{fileId} — 下载文件
- GET /api/file/info/{fileId} — 获取文件信息（基于文件ID）
- GET /api/file/info/sha256/{sha256} — 获取文件信息（基于SHA256）
- DELETE /api/file/delete/{fileId} — 删除文件
- GET /api/file/structure/{fileId} — 获取Word文档结构
- POST /api/file/generate-doc — 基于模板生成文档
- POST /api/file/fix-doc — 修复Word文档（替换文本）
- POST /api/file/extract-text — 提取Word文档文本+位置索引

**更新** 新增基于SHA256的文件信息查询接口。

**章节来源**   
- [FILE_SERVICE_SPEC.md:7-17](file://docs/rules/FILE_SERVICE_SPEC.md#L7-L17)
- [ele-ai-tender-file.yml:1-62](file://docs/guides/ele-ai-tender-file.yml#L1-L62)

### 多格式文档支持与转换
- 当前内置文档生成引擎主要面向 Word 模板渲染与 Markdown→Word 转换。
- PDF 支持可通过后续扩展实现（例如引入 PDF 生成库或第三方转换服务）。
- 批量处理建议：
  - 将批量任务拆分为多个子任务，逐个调用 generate-doc/fix-doc/extract-text。
  - 使用异步队列与回调机制，避免长连接超时。

**章节来源**   
- [FILE_SERVICE_SPEC.md:18-31](file://docs/rules/FILE_SERVICE_SPEC.md#L18-L31)

### 文件格式验证、病毒扫描与配额管理（实现建议）
- 格式验证
  - 基于扩展名白名单与MIME类型双重校验。
  - 对压缩包进行解压后递归校验。
- 病毒扫描
  - 上传完成后触发异步扫描任务，扫描结果回写文件元数据。
  - 扫描失败或发现威胁时，禁止下载与预览。
- 配额管理
  - 按用户/租户维度统计已用容量，达到阈值时拒绝新上传。
  - 提供配额查询接口与告警通知。

[本节为通用指导，无需代码引用]

### SHA256文件信息查询详解
**新增** 基于SHA256的文件信息查询功能详细说明：

#### 技术实现
- **服务端接口**：`/api/file/info/sha256/{sha256}`
- **客户端支持**：AiFileClient.getFileInfoSha256() 方法
- **参数验证**：InteractionValidationUtils.validateFileSha256()
- **响应对象**：InteractionFileInfoResponse 包含 fileId、fileName、fileSize、fileSha256、bizType

#### 使用场景
1. **秒传功能**：上传前先通过SHA256查询文件是否存在，避免重复上传
2. **文件去重**：相同内容的文件只存储一次，节省存储空间
3. **跨系统文件共享**：通过内容指纹在不同系统间识别相同文件

#### 客户端调用示例
```java
// 内部服务调用
FileInfo fileInfo = internalFileServiceClient.info(fileId);

// 外部交互调用
InteractionFileInfoResponse response = aiFileClient.getFileInfoSha256(authorization, sha256);
```

**章节来源**   
- [FileController.java:97-106](file://ele-ai-tender-system/ele-ai-tender-file/src/main/java/com/jy/eleaitender/file/controller/FileController.java#L97-L106)
- [AiFileClient.java:80-101](file://ele-ai-tender-system/ele-ai-tender-interaction/ele-ai-tender-interaction-core/src/main/java/com/jy/eleaitender/interaction/core/client/AiFileClient.java#L80-L101)
- [InteractionValidationUtils.java:39-41](file://ele-ai-tender-system/ele-ai-tender-interaction/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/util/InteractionValidationUtils.java#L39-L41)
- [InteractionFileInfoResponse.java:10-20](file://ele-ai-tender-system/ele-ai-tender-interaction/ele-ai-tender-common-interaction/src/main/java/com/jy/eleaitender/common/interaction/dto/InteractionFileInfoResponse.java#L10-L20)