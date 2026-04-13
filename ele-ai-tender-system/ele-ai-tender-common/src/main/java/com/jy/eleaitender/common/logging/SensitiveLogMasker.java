package com.jy.eleaitender.common.logging;

import org.apache.commons.lang3.StringUtils;

/**
 * 日志敏感信息脱敏工具
 */
public final class SensitiveLogMasker {

    private static final String MASK = "***";

    private SensitiveLogMasker() {
    }

    public static String maskToken(String token) {
        return maskValue(token, 10, 6);
    }

    public static String maskSecret(String secret) {
        return maskValue(secret, 4, 4);
    }

    private static String maskValue(String value, int prefixLength, int suffixLength) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        if (value.length() <= prefixLength + suffixLength) {
            return MASK;
        }
        String prefix = value.substring(0, prefixLength);
        String suffix = value.substring(value.length() - suffixLength);
        return prefix + MASK + suffix;
    }
}
