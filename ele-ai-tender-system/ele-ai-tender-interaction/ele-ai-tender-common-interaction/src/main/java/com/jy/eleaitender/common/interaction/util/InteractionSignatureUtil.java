package com.jy.eleaitender.common.interaction.util;

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 交互层签名工具。
 * 算法保持与电子标系统当前 SignatureUtil 一致。
 */
public final class InteractionSignatureUtil {

    private static final long SIGNATURE_EXPIRE_MILLIS = 5 * 60 * 1000L;

    private InteractionSignatureUtil() {
    }

    public static String generateSignature(String appKey, long timestamp, String appSecret) {
        String content = safe(appKey) + timestamp;
        return hmacSha256(content, safe(appSecret)).toUpperCase();
    }

    public static boolean verifySignature(String appKey, long timestamp, String appSecret, String signature) {
        if (isBlank(appKey) || isBlank(appSecret) || isBlank(signature)) {
            return false;
        }
        if (!isTimestampValid(timestamp)) {
            return false;
        }
        return generateSignature(appKey, timestamp, appSecret).equals(signature);
    }

    public static boolean isTimestampValid(long timestamp) {
        long diff = Math.abs(System.currentTimeMillis() - timestamp);
        return diff <= SIGNATURE_EXPIRE_MILLIS;
    }

    public static String hmacSha256(String content, String appSecret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 algorithm not available", e);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
