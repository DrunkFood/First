# crypto native 接口设计（C++ core / JNI / N-API）

## 1. 背景

当前 `ele-tender-crypto` 已完成 Java 侧 native bridge 契约重构，已明确：

- 投标文件解密存在两条链路：
  - 投标人自解密：用户再次输入原始自定义密码，本地 `SHA256(UTF-8)` 得到 `bidderPwdStr`
  - 平台/开标解密：通过 `hashKeyList + CA 锁体系` 恢复 `bidderPwdStr`
- `projectInfoRSA` 首版行为必须兼容既有 Java `RsaUtils`
- Java native bridge 已切换为字节模型，不再以字符串作为密文边界

本设计文档用于冻结后续 C++ core、JNI、N-API 的实现边界，避免 Node/Electron 与 Java/JNI 分叉。

## 1.1 当前落地状态

截至 2026-03-31，仓库内已经完成：

- `ele-tender-crypto/src/main/cpp` 下的 `crypto-core`
- `ele-tender-crypto/src/main/cpp/src/jni_crypto_native.cpp` JNI bridge
- `JniCryptoNative` 的 Java 侧 native 加载与回退逻辑
- native test 与 Java 集成测试

当前未在本仓库内直接提交 Electron 侧 N-API package，但 Node / Electron 接口必须遵守本文档冻结的 C++ core 契约，不允许另起协议。

当前固定对拍样例位于：

- `ele-tender-system/ele-tender-crypto/src/test/resources/native-vectors`

## 2. 目标

本阶段目标：

- 定义唯一的 C++ 核心能力边界
- 让 JNI 与 N-API 只做薄适配，不承载业务逻辑
- 冻结文件打包/解包、AES-GCM 分段加解密、RSA、SHA256 的输入输出模型
- 为后续真实 C++ 实现与跨语言测试向量提供统一基线

本阶段不做：

- CA 锁实现
- 流式 API
- 多版本文件格式兼容
- 协议升级到 OAEP-SHA256

## 3. 总体分层

### 3.1 分层结构

1. `crypto-core`
   - 纯 C++
   - 负责文件打包/解包、AES-GCM、RSA、SHA256、错误码
2. `crypto-jni-binding`
   - Java/JNI 适配层
   - 负责 Java `byte[]/List<byte[]>/String` 与 C++ 结构体之间的转换
3. `crypto-node-binding`
   - Electron/N-API 适配层
   - 负责 Node.js `Buffer/Object` 与 C++ 结构体之间的转换

### 3.2 边界原则

- C++ core 不负责业务分支判断
- C++ core 不负责口令恢复逻辑
- C++ core 不直接依赖 Java/Node 类型
- JNI / N-API 不重复实现加解密算法

## 4. 文件格式

首版文件格式继续沿用当前文本封装：

```text
encResult_0|||encResult_1|||...|||encResult_n|||{{{projectInfoRSA}}}[[version]]
```

字段定义：

- `encResult_i`
  - base64 字符串
  - 解码后为单段密文字节
  - 单段格式：`nonce(12) + ciphertext + tag(16)`
- `projectInfoRSA`
  - base64 字符串
  - 解码后为 RSA 密文字节
- `version`
  - 明文字符串

### 4.1 解析约束

- `unpackFile` 只做结构解析，不做 RSA 解密
- `unpackFile` 不应要求先成功解开 `projectInfoRSA`
- 即使 `projectInfoRSA` 不可解，也不影响正文分段读取
- `decryptSegments` 只依赖 segments + 32 字节 key

## 5. 两条解密路径

### 5.1 投标人自解密

流程：

1. 用户再次输入自定义密码
2. 客户端做 `SHA256(UTF-8)`，得到 32 字节 `bidderPwdStr`
3. 调用 `decryptSegments(segments, keyBytes)`
4. 校验 `originSha256`

约束：

- 不依赖系统 RSA 私钥
- 不依赖 `hashKeyList` 恢复口令
- 可读取元信息，但元信息不是正文解密前置条件

### 5.2 平台/开标解密

流程：

1. `unpackFile`
2. `rsaDecrypt(projectInfoRSA, privateKey)`
3. 解析 `hashKeyList`
4. 通过 CA 锁体系恢复 `bidderPwdStr`
5. 调用 `decryptSegments(segments, keyBytes)`
6. 校验 `originSha256`

约束：

- 口令恢复逻辑在上层，不在 C++ core 内
- C++ core 只负责 `rsaDecrypt` 与正文 `decryptSegments`

## 6. C++ core 数据模型

建议统一使用如下结构：

```cpp
namespace eletender::crypto {

struct ParsedEncryptedFile {
    std::vector<std::vector<std::uint8_t>> encrypted_segments;
    std::vector<std::uint8_t> encrypted_project_info_rsa;
    std::string format_version;
    std::vector<std::uint8_t> raw_file_bytes;
};

}
```

说明：

- `encrypted_segments`：每段原始二进制，不使用 base64 字符串表示
- `encrypted_project_info_rsa`：RSA 密文原始二进制
- `format_version`：明文版本号
- `raw_file_bytes`：可选保留，用于排障和调试

## 7. C++ core API

建议首版核心 API 如下：

```cpp
namespace eletender::crypto {

ParsedEncryptedFile unpack_file(const std::vector<std::uint8_t>& file_bytes);

std::vector<std::uint8_t> pack_file(
    const std::vector<std::vector<std::uint8_t>>& encrypted_segments,
    const std::vector<std::uint8_t>& encrypted_project_info_rsa,
    const std::string& format_version
);

std::vector<std::vector<std::uint8_t>> encrypt_segments(
    const std::vector<std::uint8_t>& plain_bytes,
    const std::array<std::uint8_t, 32>& key_bytes,
    std::size_t segment_size
);

std::vector<std::uint8_t> decrypt_segments(
    const std::vector<std::vector<std::uint8_t>>& encrypted_segments,
    const std::array<std::uint8_t, 32>& key_bytes
);

std::vector<std::uint8_t> rsa_encrypt(
    const std::vector<std::uint8_t>& plain_bytes,
    const std::string& public_key
);

std::vector<std::uint8_t> rsa_decrypt(
    const std::vector<std::uint8_t>& cipher_bytes,
    const std::string& private_key
);

std::string sha256_hex(const std::vector<std::uint8_t>& data);

}
```

### 7.1 设计说明

- `pack_file` 放入 core，禁止 Java/Node 各自拼接文本格式
- `decrypt_segments` 只接受 32 字节 key，不接受口令字符串
- 字符串转字节统一由上层完成，固定 UTF-8
- `rsa_encrypt` / `rsa_decrypt` 接受兼容 `RsaUtils` 的 key 文本

## 8. AES 约束

- 算法：`AES-256-GCM`
- 分段大小：默认 `2097152` 字节
- nonce 长度：12 字节
- tag 长度：16 字节
- 每段输出格式：`nonce(12) + ciphertext + tag(16)`

### 8.1 nonce 规则

- 使用段序号派生 nonce
- 12 字节
- 高位补 0
- 低位写入 segment index
- 小端序

## 9. RSA 约束

首版必须兼容 Java `RsaUtils` 默认行为：

- transformation：`RSA/ECB/PKCS1Padding`
- 公钥输入兼容：
  - X.509 DER Base64
  - PEM `BEGIN PUBLIC KEY`
- 私钥输入兼容：
  - PKCS#8 DER Base64
  - PEM `BEGIN PRIVATE KEY`
- 忽略头尾和空白字符
- 字符串转字节统一 UTF-8

### 9.1 分段规则

与 `RsaUtils` 对齐：

- 2048 位 RSA：
  - 加密块 `245`
  - 解密块 `256`
- 通用公式：
  - 加密块 = `keySizeBytes - 11`
  - 解密块 = `keySizeBytes`

### 9.2 升级约束

- 当前格式版本下禁止混用 PKCS1 / OAEP-SHA1 / OAEP-SHA256
- 若切换到 `OAEPWithSHA-256AndMGF1Padding`，必须升级文件格式版本

## 10. JNI 设计

Java 侧接口已冻结为：

```java
public interface ICryptoNative {

    UnpackResult unpackFile(byte[] encryptedFileBytes);

    byte[] packFile(List<byte[]> encryptedSegments, byte[] encryptedProjectInfoRsa, String formatVersion);

    List<byte[]> encryptSegments(byte[] plainBytes, byte[] keyBytes, int segmentSize);

    byte[] decryptSegments(List<byte[]> encryptedSegments, byte[] keyBytes);

    byte[] rsaEncrypt(byte[] plainBytes, String publicKey);

    byte[] rsaDecrypt(byte[] cipherBytes, String privateKey);

    String sha256(byte[] data);
}
```

JNI 实现要求：

- `List<byte[]>` 映射到 `std::vector<std::vector<uint8_t>>`
- `byte[]` 映射到 `std::vector<uint8_t>`
- `String` 仅用于：
  - version
  - public/private key
  - sha256 hex 返回值

### 10.1 JNI 头文件建议能力

- `Java_..._unpackFile`
- `Java_..._packFile`
- `Java_..._encryptSegments`
- `Java_..._decryptSegments`
- `Java_..._rsaEncrypt`
- `Java_..._rsaDecrypt`
- `Java_..._sha256`

## 11. N-API 设计

对 Electron 暴露接口建议如下：

```ts
encryptSegments(options: {
  data: Buffer,
  key: Buffer,
  segmentSize?: number
}): Buffer[]

decryptSegments(options: {
  segments: Buffer[],
  key: Buffer
}): Buffer

rsaEncrypt(options: {
  data: Buffer,
  publicKey: string
}): Buffer

rsaDecrypt(options: {
  data: Buffer,
  privateKey: string
}): Buffer

packFile(options: {
  segments: Buffer[],
  projectInfoRSA: Buffer,
  formatVersion: string
}): Buffer

unpackFile(fileBuffer: Buffer): {
  segments: Buffer[],
  projectInfoRSA: Buffer,
  formatVersion: string
}

sha256(data: Buffer | string): string
```

要求：

- Node 侧统一使用 `Buffer`
- 不在 N-API 层处理业务逻辑
- 不在 N-API 层恢复口令

## 12. 错误码建议

建议 C++ core 内部统一错误分类：

- `INVALID_FILE_FORMAT`
- `MISSING_PROJECT_INFO`
- `MISSING_VERSION`
- `INVALID_BASE64_SEGMENT`
- `INVALID_AES_KEY_LENGTH`
- `AES_ENCRYPT_FAILED`
- `AES_DECRYPT_FAILED`
- `RSA_ENCRYPT_FAILED`
- `RSA_DECRYPT_FAILED`
- `UNSUPPORTED_VERSION`
- `SHA256_VERIFY_FAILED`

JNI / N-API 再将其映射为各自语言异常。

## 13. 测试向量建议

实现前先准备跨语言固定测试向量：

1. 固定明文 JSON
2. 固定用户密码
3. 固定 `SHA256(userPwdInput)` 结果
4. 固定分段结果
5. 固定每段密文 base64
6. 固定 `projectInfoRSA` 明文
7. 固定 `projectInfoRSA` 密文
8. 固定最终 `.HzctTbs` 文件样本
9. 错误样本：
   - 缺 `[[version]]`
   - 缺 `{{{projectInfoRSA}}}`
   - 非法 base64
   - key 长度不为 32
   - GCM tag 校验失败

## 14. 实施顺序建议

1. 先实现 `crypto-core`
   - `sha256_hex`
   - `encrypt_segments`
   - `decrypt_segments`
   - `pack_file`
   - `unpack_file`
   - `rsa_encrypt`
   - `rsa_decrypt`
2. 再实现 `crypto-jni-binding`
3. 再实现 `crypto-node-binding`
4. 最后补跨语言对拍测试

## 15. 当前结论

当前推荐的落地方向是：

- 协议保持不变
- 两条解密路径保持分离
- Java / Node 都依赖同一个 C++ core
- 首版优先做“正确一致”，不提前做流式优化
