package com.jy.eleaitender.common.utils;

import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.symmetric.AES;
import com.jy.eleaitender.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * AES 加密解密工具
 * 用于敏感字段（如API密钥）的加密存储和解密读取
 * 使用 CBC 模式，IV 与密钥相同（16字节），密文格式: Base64(IV+cipherBytes)
 */
@Slf4j
public class AesUtil {

    private static final String DEFAULT_KEY = "eleAiTenderAesK1"; // 16字节 = AES-128
    private static final String DEFAULT_IV = "eleAiTenderIv001"; // 16字节 IV

    private static volatile AES aesCipher;

    private AesUtil() {
    }

    /**
     * 获取AES实例（支持通过环境变量 APP_AES_KEY 覆盖密钥）
     */
    private static AES getAes() {
        if (aesCipher == null) {
            synchronized (AesUtil.class) {
                if (aesCipher == null) {
                    String key = System.getenv("APP_AES_KEY");
                    if (key == null || key.length() != 16) {
                        key = DEFAULT_KEY;
                    }
                    String iv = System.getenv("APP_AES_IV");
                    if (iv == null || iv.length() != 16) {
                        iv = DEFAULT_IV;
                    }
                    aesCipher = new AES(Mode.CBC, Padding.PKCS5Padding,
                            key.getBytes(StandardCharsets.UTF_8),
                            iv.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
        return aesCipher;
    }

    /**
     * AES加密
     *
     * @param plaintext 明文
     * @return Base64编码的密文
     */
    public static String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        try {
            return getAes().encryptBase64(plaintext);
        } catch (Exception e) {
            log.error("AES加密失败", e);
            throw new BusinessException("敏感数据加密失败");
        }
    }

    /**
     * AES解密
     *
     * @param ciphertext Base64编码的密文
     * @return 明文
     */
    public static String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }
        try {
            return getAes().decryptStr(ciphertext);
        } catch (Exception e) {
            log.error("AES解密失败", e);
            throw new BusinessException("敏感数据解密失败");
        }
    }

    /**
     * 脱敏处理：保留前4位和后4位，中间用****替代
     * 不足8位的密钥全部用****替代
     *
     * @param plaintext 明文
     * @return 脱敏后的字符串
     */
    public static String mask(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        if (plaintext.length() <= 8) {
            return "****";
        }
        String prefix = plaintext.substring(0, 4);
        String suffix = plaintext.substring(plaintext.length() - 4);
        return prefix + "****" + suffix;
    }
}
