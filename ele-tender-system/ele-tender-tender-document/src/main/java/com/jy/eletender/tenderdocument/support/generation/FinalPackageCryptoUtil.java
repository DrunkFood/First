package com.jy.eletender.tenderdocument.support.generation;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 最终数据包 AES-GCM 加解密工具。
 * 二进制格式：magic(4) + version(1) + nonce(12) + ciphertextAndTag。
 */
public final class FinalPackageCryptoUtil {

    // 数据包头标识：用于在解密前快速校验“是否为本系统加密产物”。
    private static final byte[] MAGIC = new byte[]{'H', 'Z', 'P', 'K'};
    private static final byte FORMAT_VERSION = 1;
    // GCM 推荐使用 12 字节 nonce，兼顾安全性和性能。
    private static final int NONCE_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private FinalPackageCryptoUtil() {
    }

    public static byte[] encrypt(byte[] plainContent, String algorithm, byte[] aesKey) {
        if (plainContent == null || plainContent.length == 0) {
            throw new IllegalArgumentException("plain content is empty");
        }
        assertAesKeyLength(aesKey);
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            SECURE_RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(GCM_TAG_BITS, nonce));
            byte[] cipherBytes = cipher.doFinal(plainContent);
            return packBinary(nonce, cipherBytes);
        } catch (Exception ex) {
            throw new IllegalStateException("encrypt failed: " + ex.getMessage(), ex);
        }
    }

    public static byte[] decrypt(byte[] encryptedContent, String algorithm, byte[] aesKey) {
        if (encryptedContent == null || encryptedContent.length == 0) {
            throw new IllegalArgumentException("encrypted content is empty");
        }
        assertAesKeyLength(aesKey);
        ByteBuffer buffer = ByteBuffer.wrap(encryptedContent);
        if (buffer.remaining() < MAGIC.length + 1 + NONCE_BYTES + 16) {
            throw new IllegalArgumentException("encrypted content length is invalid");
        }
        byte[] magic = new byte[MAGIC.length];
        buffer.get(magic);
        if (!Arrays.equals(magic, MAGIC)) {
            throw new IllegalArgumentException("encrypted content magic is invalid");
        }
        byte version = buffer.get();
        if (version != FORMAT_VERSION) {
            throw new IllegalArgumentException("encrypted content version is unsupported: " + version);
        }
        byte[] nonce = new byte[NONCE_BYTES];
        buffer.get(nonce);
        byte[] cipherBytes = new byte[buffer.remaining()];
        buffer.get(cipherBytes);
        try {
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(GCM_TAG_BITS, nonce));
            return cipher.doFinal(cipherBytes);
        } catch (Exception ex) {
            throw new IllegalStateException("decrypt failed: " + ex.getMessage(), ex);
        }
    }

    public static byte[] decodeBase64Key(String keyBase64) {
        if (keyBase64 == null || keyBase64.isBlank()) {
            throw new IllegalArgumentException("keyBase64 is blank");
        }
        try {
            byte[] key = Base64.getDecoder().decode(keyBase64);
            assertAesKeyLength(key);
            return key;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("keyBase64 is invalid: " + ex.getMessage(), ex);
        }
    }

    private static byte[] packBinary(byte[] nonce, byte[] cipherBytes) {
        // 固定封装格式，便于后续版本演进时兼容处理（先识别 magic/version 再分支解码）。
        ByteBuffer buffer = ByteBuffer.allocate(MAGIC.length + 1 + nonce.length + cipherBytes.length);
        buffer.put(MAGIC);
        buffer.put(FORMAT_VERSION);
        buffer.put(nonce);
        buffer.put(cipherBytes);
        return buffer.array();
    }

    private static void assertAesKeyLength(byte[] aesKey) {
        if (aesKey == null) {
            throw new IllegalArgumentException("aes key is null");
        }
        int length = aesKey.length;
        if (length != 16 && length != 24 && length != 32) {
            throw new IllegalArgumentException("aes key length is invalid: " + length);
        }
    }
}
