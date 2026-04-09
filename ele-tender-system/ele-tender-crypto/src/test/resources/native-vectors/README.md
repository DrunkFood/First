# native-vectors

该目录用于 Electron / Node 与 Java / JNI 对拍。

说明：
- `sample.HzctTbs` 为样例加密文件
- `sample-bid-document.json` 为原始明文
- `sample-project-info-rsa-plain.json` 为 RSA 加密前明文
- `public-key.pem` / `private-key.pem` 为测试专用密钥
- `manifest.json` 记录固定密码、SHA256、文件名与校验说明
- RSA/PKCS1 加密是随机的，因此未来重新导出时 `sample.HzctTbs` 字节不要求完全相同，但必须可被本仓库 Java 实现解开且业务字段一致
