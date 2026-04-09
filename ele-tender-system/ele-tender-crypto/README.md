# ele-tender-crypto

## 模块定位

`ele-tender-crypto` 是投标文件加解密服务，面向外部业务系统提供以下能力：

- 投标文件预存：`POST /api/crypto/bid-document/push`
- 解密请求提交：`POST /api/crypto/bid-decrypt/submit`
- 解密状态查询：`GET /api/crypto/bid-decrypt/status/{recordId}`

模块只暴露 external JWT 业务接口；后台管理查询、人工重试和访问日志检索统一收口到 `ele-tender-support`。

## 核心链路

### 预存

1. 业务系统传入 `fileId` 与 `fileSha256`
2. 服务从文件服务下载加密文件并校验 SHA-256
3. 文件按散列目录落盘
4. 写入 `bdc_bid_document`
5. 回调业务系统预存结果

### 解密

1. 业务系统提交 `projectId/tenderId/bidRecordId/bidderPwdStr/fileSha256`
2. 使用 `HMAC-SHA256(crypto.bidder-pwd-fingerprint-secret, bidderPwdStr)` 计算指纹
3. 按 `(app_key, file_sha256, bidder_pwd_fingerprint)` 查找或创建 `bdc_decrypt_artifact`
4. 创建 `bdc_decrypt_request` 并投递 Redis Stream
5. Consumer 异步执行解密
6. 工件终态后把结果投影到请求记录并逐条回调业务系统

## 关键数据模型

### `bdc_bid_document`

- 记录预存后的加密投标文件
- 关键字段：`file_id`、`file_sha256`、`file_storage_path`、`upload_status`、`callback_status`

### `bdc_decrypt_artifact`

- 解密结果缓存，按 `(app_key, file_sha256, bidder_pwd_fingerprint)` 去重
- 只保存口令指纹，不保存原始口令
- 关键字段：`status`、`retry_count`、`decrypt_result_json`、`bid_record_data_json`

### `bdc_decrypt_request`

- 记录每次解密提交、终态和回调结果
- `recordId` 即 `bdc_decrypt_request.id`
- 关键字段：`status`、`callback_status`、`organized_encrypted_file_path`、`organized_decrypted_file_path`

## 文件存储结构

| 类型 | 路径格式 | 用途 |
|------|----------|------|
| 加密文件（原始） | `{bid-document-path}/{appKey}/{sha256[0:2]}/{sha256[2:4]}/{sha256[4:6]}/{sha256}.HzctTbs` | 内容寻址，去重基准 |
| 解密文件（原始） | `{decrypt-file-path}/{artifactId}.json` | 工件级原始存储 |
| 加密文件（组织视图） | `{organized-file-path}/{appKey}/{projectId}/{tenderId}/encrypted/{bidRecordId}.HzctTbs` | 按项目/标段浏览 |
| 解密文件（组织视图） | `{organized-file-path}/{appKey}/{projectId}/{tenderId}/decrypted/{bidRecordId}.json` | 按项目/标段浏览 |

组织视图在解密成功后由 `OrganizedFileCopyService` 复制，文件路径记录在 `bdc_decrypt_request` 的 `organized_encrypted_file_path` 和 `organized_decrypted_file_path` 字段。复制失败不阻塞解密流程。

> 路径中的 `.HzctTbs` 后缀可按接入系统配置（`sup_access_system.bid_document_suffix`），未配置时使用默认值。

## 关键配置

```yaml
crypto:
  bid-document-path: /data/ele-tender/crypto/bid-documents
  decrypt-file-path: /data/ele-tender/crypto/decrypted
  temp-file-path: /data/ele-tender/crypto/temp
  organized-file-path: /data/ele-tender/crypto/organized  # 项目/标段组织视图根目录
  bidder-pwd-fingerprint-secret: ${CRYPTO_BIDDER_PWD_FINGERPRINT_SECRET:}
  file-service-base-url: http://localhost:8081
  file-info-path: /api/file/info/{fileId}
  file-download-path: /api/file/download/{fileId}
  password-cache-seconds: 3600
  stream:
    key: bdc:decrypt:tasks
    group: bdc-decrypt-group
```

说明：

- `bidder-pwd-fingerprint-secret` 缺失时当前实现会直接启动失败
- `password-cache-seconds` 应覆盖异步执行窗口，避免任务读取不到 Redis 中的原始口令
- `organized-file-path` 对应环境变量 `CRYPTO_ORGANIZED_FILE_PATH`，不配置时使用默认值

## 常用命令

```bash
cd ele-tender-system
mvn -pl ele-tender-crypto -am spring-boot:run
mvn -pl ele-tender-crypto,ele-tender-common-interaction,ele-tender-interaction/ele-tender-interaction-core -am test
```

### native / JNI 构建与测试

当前模块已在仓库内提供 C++ core 与 JNI bridge，源码路径：

- `ele-tender-system/ele-tender-crypto/src/main/cpp`
- `ele-tender-system/ele-tender-crypto/src/test/cpp`

常用命令：

```bash
cd ele-tender-system/ele-tender-crypto
cmake -S src/main/cpp -B target/native
cmake --build target/native --target crypto_core_test
ctest --test-dir target/native --output-on-failure
cmake --build target/native --target eletender_crypto_native
```

说明：

- `crypto_core_test` 用于 native 层自测
- `eletender_crypto_native` 为 JNI 共享库
- Java 集成测试可通过 `JniCryptoNativeIntegrationTest` 自动构建并加载该共享库

## 排障要点

- 预存失败先看：
  - 文件服务下载接口是否可达
  - 传入 `fileSha256` 是否与实际文件一致
  - 本地目录是否有写权限
- 解密未推进先看：
  - Redis Stream `bdc:decrypt:tasks`
  - artifact/request 状态是否停在 `PENDING/PROCESSING`
  - Redis 中 `crypto:pwd:{artifactId}` 是否还存在
- 回调失败先看：
  - `sup_access_system` 中的系统配置是否完整
  - 业务系统回调地址是否可达
  - request 的 `callback_status/callback_response_code/callback_response_message`

## 注意事项

- 模块只开放业务接口，不开放后台管理接口
- 后台查询、人工重试统一放在 `ele-tender-support`
- 工件去重维度固定为 `appKey + fileSha256 + bidderPwdFingerprint`
- 原始投标口令只在 Redis 短期缓存，不允许落库
- Stream 消息采用至少一次投递，终态幂等由 artifact 状态保证

## 相关文档

- `docs/rules/ELE_TENDER_CRYPTO_SPEC.md`
- `docs/guides/业务系统接入解密服务指南.md`
- `docs/guides/本地加解密工具指南（Java CLI）.md`
- `docs/guides/Electron-native-交付清单.md`
- `docs/guides/NativeVectorExportCli-使用说明.md`

## Electron 交付物

给 Electron / Node.js 同事的固定交付物包括：

- native 协议与实现边界文档
- `src/test/resources/native-vectors` 对拍样例
- C++ core 与 JNI 参考实现

如需重新导出样例：

```bash
cd ele-tender-system
mvn -pl ele-tender-crypto -am \
  -Dtest=NativeVectorExportCliTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dnative.vector.export.dir=src/test/resources/native-vectors \
  test
```
