# Crypto Native Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `ele-tender-crypto` 模块内落地可编译、可测试的 C++ core 与 JNI 集成，实现文件打包/解包、AES-GCM、RSA、SHA256，并与 Java native bridge 对齐。

**Architecture:** 在 `ele-tender-system/ele-tender-crypto` 下新增 `src/main/cpp` 与 `src/test/cpp`，使用 CMake 构建共享库与 native test executable。Java 侧 `JniCryptoNative` 负责加载共享库并调用 JNI 方法，`StubCryptoNative` 继续作为 native 库缺失时的开发回退。

**Tech Stack:** C++17, CMake, OpenSSL, JNI, JUnit 5, Maven

---

### Task 1: 工程骨架与计划输入

**Files:**
- Create: `docs/plans/2026-03-31-crypto-native-implementation-plan.md`
- Create: `ele-tender-system/ele-tender-crypto/src/main/cpp/CMakeLists.txt`
- Create: `ele-tender-system/ele-tender-crypto/src/main/cpp/include/*`
- Create: `ele-tender-system/ele-tender-crypto/src/test/cpp/*`

- [ ] 建立 native 目录结构与 CMake 构建入口
- [ ] 明确 OpenSSL 与 JNI 依赖查找方式
- [ ] 为后续 native unit test 预留 test target
- [ ] 执行 `cmake -S src/main/cpp -B target/native` 验证工程骨架可配置

### Task 2: C++ core 实现

**Files:**
- Create: `ele-tender-system/ele-tender-crypto/src/main/cpp/include/crypto_core.hpp`
- Create: `ele-tender-system/ele-tender-crypto/src/main/cpp/include/crypto_error.hpp`
- Create: `ele-tender-system/ele-tender-crypto/src/main/cpp/src/crypto_core.cpp`
- Create: `ele-tender-system/ele-tender-crypto/src/main/cpp/src/file_format.cpp`
- Create: `ele-tender-system/ele-tender-crypto/src/main/cpp/src/rsa_crypto.cpp`
- Create: `ele-tender-system/ele-tender-crypto/src/main/cpp/src/aes_gcm_crypto.cpp`
- Create: `ele-tender-system/ele-tender-crypto/src/test/cpp/crypto_core_test.cpp`

- [ ] 先写 native test，覆盖 `pack/unpack` round-trip
- [ ] 跑 test，确认失败
- [ ] 实现 `pack_file` / `unpack_file`
- [ ] 跑 native test，确认通过
- [ ] 增加 AES-GCM 分段加解密 test
- [ ] 跑 test，确认失败
- [ ] 实现 `encrypt_segments` / `decrypt_segments`
- [ ] 跑 native test，确认通过
- [ ] 增加 RSA 兼容 test
- [ ] 跑 test，确认失败
- [ ] 实现 `rsa_encrypt` / `rsa_decrypt` / `sha256_hex`
- [ ] 跑 native test，确认通过

### Task 3: JNI 集成

**Files:**
- Modify: `ele-tender-system/ele-tender-crypto/src/main/java/com/jy/eletender/crypto/native_bridge/JniCryptoNative.java`
- Create: `ele-tender-system/ele-tender-crypto/src/main/java/com/jy/eletender/crypto/native_bridge/NativeLibraryLoader.java`
- Create: `ele-tender-system/ele-tender-crypto/src/main/cpp/src/jni_crypto_native.cpp`
- Create: `ele-tender-system/ele-tender-crypto/src/test/java/com/jy/eletender/crypto/native_bridge/JniCryptoNativeIntegrationTest.java`

- [ ] 先写 Java 集成测试，约束 JNI `pack/unpack` / `decryptSegments` 行为
- [ ] 跑 test，确认失败
- [ ] 实现 `NativeLibraryLoader` 与 `JniCryptoNative`
- [ ] 实现 JNI C++ 桥接
- [ ] 构建 native 共享库
- [ ] 跑 Java 集成测试，确认通过

### Task 4: 文档与全面验证

**Files:**
- Modify: `docs/rules/ELE_TENDER_CRYPTO_SPEC.md`
- Modify: `docs/guides/本地加解密工具指南（Java CLI）.md`
- Modify: `docs/guides/Electron 客户端加解密指南.md`
- Modify: `docs/projects/2026-03-31-crypto-native-interface-design.md`

- [ ] 同步 native 目录、构建方式、JNI/N-API 状态到文档
- [ ] 运行 native tests
- [ ] 运行 Java native bridge tests
- [ ] 运行 `mvn -pl ele-tender-crypto -am test`
- [ ] 复查 git diff
- [ ] 提交 commit（中文）
