package com.jy.eletender.crypto.native_bridge;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 真实 JNI 实现。
 * native 库缺失时自动回退到 {@link StubCryptoNative}，便于开发与联调。
 */
@Component
@Primary
public class JniCryptoNative extends StubCryptoNative {

    @Override
    public UnpackResult unpackFile(byte[] encryptedFileBytes) {
        return nativeAvailable() ? nativeUnpackFile(encryptedFileBytes) : super.unpackFile(encryptedFileBytes);
    }

    @Override
    public byte[] packFile(List<byte[]> encryptedSegments, byte[] encryptedProjectInfoRsa, String formatVersion) {
        return nativeAvailable()
                ? nativePackFile(encryptedSegments, encryptedProjectInfoRsa, formatVersion)
                : super.packFile(encryptedSegments, encryptedProjectInfoRsa, formatVersion);
    }

    @Override
    public List<byte[]> encryptSegments(byte[] plainBytes, byte[] keyBytes, int segmentSize) {
        return nativeAvailable()
                ? nativeEncryptSegments(plainBytes, keyBytes, segmentSize)
                : super.encryptSegments(plainBytes, keyBytes, segmentSize);
    }

    @Override
    public byte[] decryptSegments(List<byte[]> encryptedSegments, byte[] keyBytes) {
        return nativeAvailable()
                ? nativeDecryptSegments(encryptedSegments, keyBytes)
                : super.decryptSegments(encryptedSegments, keyBytes);
    }

    @Override
    public byte[] rsaEncrypt(byte[] plainBytes, String publicKey) {
        return nativeAvailable() ? nativeRsaEncrypt(plainBytes, publicKey) : super.rsaEncrypt(plainBytes, publicKey);
    }

    @Override
    public byte[] rsaDecrypt(byte[] cipherBytes, String privateKey) {
        return nativeAvailable() ? nativeRsaDecrypt(cipherBytes, privateKey) : super.rsaDecrypt(cipherBytes, privateKey);
    }

    @Override
    public String sha256(byte[] data) {
        return nativeAvailable() ? nativeSha256(data) : super.sha256(data);
    }

    @Override
    public List<byte[]> splitBidderPwd(byte[] bidderPwdBytes, int encryptOrder) {
        // 切段逻辑当前由 Java Stub 实现；待 C++ native 侧完成后可切换到 JNI
        return super.splitBidderPwd(bidderPwdBytes, encryptOrder);
    }

    private boolean nativeAvailable() {
        return NativeLibraryLoader.isLoaded() || NativeLibraryLoader.tryLoad();
    }

    private native UnpackResult nativeUnpackFile(byte[] encryptedFileBytes);

    private native byte[] nativePackFile(List<byte[]> encryptedSegments, byte[] encryptedProjectInfoRsa, String formatVersion);

    private native List<byte[]> nativeEncryptSegments(byte[] plainBytes, byte[] keyBytes, int segmentSize);

    private native byte[] nativeDecryptSegments(List<byte[]> encryptedSegments, byte[] keyBytes);

    private native byte[] nativeRsaEncrypt(byte[] plainBytes, String publicKey);

    private native byte[] nativeRsaDecrypt(byte[] cipherBytes, String privateKey);

    private native String nativeSha256(byte[] data);
}
