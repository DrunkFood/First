package com.jy.eleaitender.common.util;

import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import com.jy.eleaitender.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * RSA 密钥对管理工具
 * 启动时生成密钥对，支持定时轮换，提供公钥获取和私钥解密能力
 */
@Slf4j
public class RsaKeyUtil {

    /**
     * 当前密钥对持有者
     */
    private static final AtomicReference<RsaKeyHolder> KEY_HOLDER = new AtomicReference<>();

    static {
        rotate();
    }

    /**
     * 轮换密钥对（由定时任务或启动时调用）
     */
    public static void rotate() {
        RSA rsa = new RSA();
        RsaKeyHolder holder = new RsaKeyHolder(
                UUID.randomUUID().toString().replace("-", ""),
                rsa
        );
        KEY_HOLDER.set(holder);
        log.info("RSA密钥对已轮换，keyId={}", holder.keyId);
    }

    /**
     * 获取当前公钥信息（供前端获取）
     */
    public static PublicKeyInfo getPublicKeyInfo() {
        RsaKeyHolder holder = KEY_HOLDER.get();
        String publicKeyBase64 = Base64.getEncoder().encodeToString(
                holder.rsa.getPublicKey().getEncoded()
        );
        return new PublicKeyInfo(holder.keyId, publicKeyBase64);
    }

    /**
     * 用指定 keyId 对应的私钥解密
     *
     * @param keyId           密钥ID
     * @param encryptedBase64 Base64 编码的密文
     * @return 明文
     */
    public static String decrypt(String keyId, String encryptedBase64) {
        RsaKeyHolder holder = KEY_HOLDER.get();
        if (!holder.keyId.equals(keyId)) {
            throw new SecurityException("RSA密钥ID不匹配，可能密钥已轮换，请重新获取公钥");
        }
        byte[] encrypted = Base64.getDecoder().decode(encryptedBase64);
        byte[] decrypted = holder.rsa.decrypt(encrypted, KeyType.PrivateKey);
        return new String(decrypted);
    }

    /**
     * RSA解密密码
     */
    public static String decryptPassword(String keyId, String encryptedPassword) {
        try {
            return RsaKeyUtil.decrypt(keyId, encryptedPassword);
        } catch (SecurityException e) {
            log.warn("RSA解密失败，keyId={}: {}", keyId, e.getMessage(), e);
            throw new BusinessException("密码解密失败，请重新获取公钥");
        } catch (Exception e) {
            log.error("RSA解密异常，keyId={}", keyId, e);
            throw new BusinessException("密码解密失败，请重新获取公钥");
        }
    }

    /**
     * 密钥对持有者
     */
    private record RsaKeyHolder(String keyId, RSA rsa) {
    }

    /**
     * 公钥信息（返回给前端）
     */
    public record PublicKeyInfo(String keyId, String publicKey) {
    }
}
