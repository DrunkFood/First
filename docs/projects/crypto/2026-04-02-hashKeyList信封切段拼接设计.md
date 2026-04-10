# hashKeyList 信封切段与拼接设计

> 创建日期：2026-04-02
> 状态：待审核

---

## 背景

投标文件加密时，`bidderPwdStr`（32 字节 AES 密钥）需要按 `encryptOrder` 层数切段，每段分别用 CA 公钥加密，生成 `hashKeyList` 写入 `projectInfoRSA`。开标解密时需要逆向还原。

当前问题：
- Electron 客户端加解密指南中预设 TSign SDK 内置切段/拼接能力（`tsign.encryptBidderPwd` / `tsign.decryptBidderPwd`），但 TSign SDK 大概率只提供单次 CA 公钥加密/私钥解密的原子能力，不支持分段
- 如果切段/拼接逻辑分散在 Electron 和开标系统各自实现，存在对不齐的风险

## 设计决策

| 决策点 | 结论 |
|--------|------|
| 切段逻辑归属 | C++ core（给 Electron 离线使用，通过 N-API 绑定） |
| 拼接逻辑归属 | crypto 模块 HTTP API（`POST /api/crypto/envelope/join`） |
| 切段算法 | 32 字节 raw bytes 均分，不整除时末段取余 |
| 数据表示 | raw bytes 切段，传输用 base64 编码 |
| 拼接与解密的关系 | 独立接口，只负责还原 `bidderPwdStr` 并返回给业务系统 |
| 切段 HTTP API | 不暴露，当前只有 Electron 需要（YAGNI） |
| 对齐保障 | 共享测试向量，C++ 和 Java 跑同一套 |

## 一、切段逻辑（C++ core）

### 接口

```cpp
std::vector<std::vector<uint8_t>> splitBidderPwd(
    const std::vector<uint8_t>& bidderPwdBytes,  // 32 bytes
    int encryptOrder                               // 层数, 1~32
);
```

### N-API 绑定

```javascript
const segments = crypto.splitBidderPwd({
  bidderPwdBytes: Buffer.from(bidderPwdHex, 'hex'), // 32 bytes
  encryptOrder: 2
});
// segments: Buffer[]
```

### 切段规则

| encryptOrder | 段分布（字节） |
|---|---|
| 1 | [32] |
| 2 | [16, 16] |
| 3 | [10, 10, 12] |
| 4 | [8, 8, 8, 8] |

- `encryptOrder` 必须 >= 1 且 <= 32
- 每段最少 1 字节

### Electron 加密流程

```
crypto.splitBidderPwd(bidderPwdBytes, encryptOrder) → segments[]
for each layer in caKeysInfo.layers:
  for each caKey in layer.caKeys:
    hashKeyE = tsign.encrypt(segments[layer.order - 1], caKey.publicKey)
→ 组装 hashKeyList
```

## 二、拼接接口（crypto 模块 HTTP API）

### 接口

```
POST /api/crypto/envelope/join
```

### 请求体

```json
{
  "hashKeyList": [
    {
      "caId": "ca锁id-1",
      "hashKeyE": "该段CA加密后的base64",
      "hashKeyD": "该段CA解密后的base64",
      "order": 1
    },
    {
      "caId": "ca锁id-2",
      "hashKeyE": "...",
      "hashKeyD": "...",
      "order": 1
    },
    {
      "caId": "ca锁id-1",
      "hashKeyE": "...",
      "hashKeyD": "...",
      "order": 2
    }
  ]
}
```

### 处理逻辑

1. 按 `order` 去重排序，确定分段数
2. 每个 order 组内取任意一条有 `hashKeyD` 的记录
3. 按 order 升序拼接各段 raw bytes → 还原 `bidderPwdStr`
4. 校验总长度 == 32 字节，不等则返回错误

### 响应

```json
{
  "code": 200,
  "data": {
    "bidderPwdStr": "base64...(32 bytes)"
  }
}
```

### 鉴权

走现有 external JWT 认证（`type=EXTERNAL`），与其他 interaction 接口一致。

### 交互层

interaction starter 封装调用，业务系统不直接拼 HTTP 请求。

## 三、测试向量

在 `ele-tender-crypto/src/test/resources/native-vectors/` 维护向量文件：

```json
{
  "vectors": [
    {
      "description": "1 layer, no split",
      "bidderPwdBytes": "base64...(32 bytes)",
      "encryptOrder": 1,
      "expectedSegments": ["base64...(32 bytes)"]
    },
    {
      "description": "2 layers, even split",
      "bidderPwdBytes": "base64...(32 bytes)",
      "encryptOrder": 2,
      "expectedSegments": ["base64...(16 bytes)", "base64...(16 bytes)"]
    },
    {
      "description": "3 layers, remainder on last",
      "bidderPwdBytes": "base64...(32 bytes)",
      "encryptOrder": 3,
      "expectedSegments": ["base64...(10 bytes)", "base64...(10 bytes)", "base64...(12 bytes)"]
    }
  ]
}
```

- Java 侧：crypto 模块单元测试加载向量验证 split/join
- C++ 侧：复制同一套向量文件验证 `splitBidderPwd`
- 双向验证：split 结果 join 回来必须等于原始 `bidderPwdStr`

## 四、影响范围

### 需要更新的文档

| 文档 | 变更 |
|------|------|
| `docs/guides/Electron 客户端加解密指南.md` | 加密流程改为 `crypto.splitBidderPwd()` + 逐层调 TSign |
| `docs/rules/BID_DOCUMENT_FORMAT_SPEC.md` | hashKeyList 说明补充 `hashKeyD` 字段定义 |
| `docs/rules/ELE_TENDER_CRYPTO_SPEC.md` | 补充 envelope/join 接口说明 |
| `docs/rules/BID_OPENING_FLOW_SPEC.md` | 信封解密步骤更新为调 envelope/join |
| `docs/guides/业务系统接入手册.md` | 与解密服务指南合并为一份，补充 envelope/join 接入说明 |
| `docs/guides/业务系统接入解密服务指南.md` | 合并入接入手册后删除 |

### 需要更新的代码

| 位置 | 变更 |
|------|------|
| `ele-tender-crypto` | 新增 envelope/join 接口（controller、service、DTO） |
| `ele-tender-crypto` | 新增 split/join 工具类 + 测试向量 + 单元测试 |
| `ele-tender-common-interaction` | 新增 envelope/join 的请求/响应 DTO |
| `ele-tender-interaction` | starter 封装 envelope/join 调用 |
| `ele-tender-dev-tools/BidDocumentGenerator` | hashKeyList 生成改用 split 工具类 |
| C++ core | 新增 `splitBidderPwd` + N-API 绑定 + 测试 |

### 不变的部分

- 投标文件整体格式（`.HzctTbs`）不变
- `projectInfoRSA` JSON 结构不变
- AES-GCM 分段加解密逻辑不变
- 投标人自解密链路不变（不依赖 hashKeyList）
