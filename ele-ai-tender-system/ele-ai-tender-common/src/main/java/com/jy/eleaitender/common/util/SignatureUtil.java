package com.jy.eleaitender.common.utils;

import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * 签名工具类
 */
public class SignatureUtil {

    private SignatureUtil() {}

    /**
     * 签名有效期（毫秒）- 5分钟
     */
    private static final long SIGNATURE_EXPIRE_MILLIS = 5 * 60 * 1000L;

    /**
     * 生成签名
     * 签名内容 = appKey + timestamp
     * signature = HMAC-SHA256(appSecret, 签名内容).toUpperCase()
     *
     * @param appKey    应用Key
     * @param timestamp 时间戳（毫秒）
     * @param appSecret 应用密钥
     * @return 签名字符串
     */
    public static String generateSignature(String appKey, long timestamp, String appSecret) {
        String content = appKey + timestamp;
        return hmacSha256Hex(content, appSecret).toUpperCase();
    }

    /**
     * 验证签名
     *
     * @param appKey    应用Key
     * @param timestamp 时间戳（毫秒）
     * @param appSecret 应用密钥
     * @param signature 待验证的签名
     * @return 是否有效
     */
    public static boolean verifySignature(String appKey, long timestamp, String appSecret, String signature) {
        return verifySignature(appKey, timestamp, appSecret, signature, SIGNATURE_EXPIRE_MILLIS / 1000);
    }

    /**
     * 验证签名（自定义签名有效期）
     *
     * @param appKey               应用Key
     * @param timestamp            时间戳（毫秒）
     * @param appSecret            应用密钥
     * @param signature            待验证的签名
     * @param signatureExpireSeconds 签名有效期（秒）
     * @return 是否有效
     */
    public static boolean verifySignature(String appKey, long timestamp, String appSecret, String signature, long signatureExpireSeconds) {
        if (StringUtils.isAnyBlank(appKey, appSecret, signature)) {
            return false;
        }

        // 验证时间戳是否在有效期内
        if (!isTimestampValid(timestamp, signatureExpireSeconds)) {
            return false;
        }

        // 重新计算签名并对比
        String expectedSignature = generateSignature(appKey, timestamp, appSecret);
        return expectedSignature.equals(signature);
    }

    /**
     * 验证时间戳是否在有效期内（5分钟）
     *
     * @param timestamp 时间戳（毫秒）
     * @return 是否有效
     */
    public static boolean isTimestampValid(long timestamp) {
        return isTimestampValid(timestamp, SIGNATURE_EXPIRE_MILLIS / 1000);
    }

    /**
     * 验证时间戳是否在有效期内（自定义秒数）
     */
    public static boolean isTimestampValid(long timestamp, long signatureExpireSeconds) {
        long currentTime = System.currentTimeMillis();
        long diff = Math.abs(currentTime - timestamp);
        return diff <= signatureExpireSeconds * 1000;
    }

    /**
     * 生成AppKey
     *
     * @return 32位UUID格式的AppKey
     */
    public static String generateAppKey() {
        return cn.hutool.core.util.IdUtil.simpleUUID();
    }

    /**
     * 生成AppSecret
     *
     * @return 64位随机字符串
     */
    public static String generateAppSecret() {
        return cn.hutool.core.util.IdUtil.simpleUUID() + cn.hutool.core.util.IdUtil.simpleUUID();
    }

    private static String hmacSha256Hex(String content, String appSecret) {
        HMac hMac = new HMac(HmacAlgorithm.HmacSHA256, appSecret.getBytes(StandardCharsets.UTF_8));
        return hMac.digestHex(content, StandardCharsets.UTF_8);
    }
}
