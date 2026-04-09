package com.jy.eletender.crypto.support;

import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 投标口令指纹工具。
 * 这里只产出口令指纹，不返回原始口令，也不做可逆处理；
 * 指纹的唯一用途是做 `bdc_decrypt_artifact` 去重键的一部分。
 */
public final class CryptoFingerprintUtil {

    private static volatile String bidderPwdFingerprintSecret;

    private CryptoFingerprintUtil() {
    }

    public static synchronized void configure(String secret) {
        bidderPwdFingerprintSecret = secret;
    }

    public static String fingerprint(String bidderPwdStr) {
        // 统一使用 HMAC-SHA256 + UTF-8，保证跨环境指纹结果稳定。
        if (StringUtils.isBlank(bidderPwdFingerprintSecret)) {
            throw new IllegalStateException("bidderPwdFingerprintSecret 未配置，指纹算法不可用");
        }
        String normalized = bidderPwdStr == null ? "" : bidderPwdStr;
        HMac hmac = new HMac(HmacAlgorithm.HmacSHA256, bidderPwdFingerprintSecret.getBytes(StandardCharsets.UTF_8));
        return hmac.digestHex(normalized).toLowerCase(Locale.ROOT);
    }
}
