package com.jy.eletender.interaction.core.support;

/**
 * 交互层日志脱敏工具。
 */
public final class InteractionLogMasker {

    private InteractionLogMasker() {
    }

    public static String maskToken(String value) {
        return maskMiddle(value, 6, 4);
    }

    public static String maskSecret(String value) {
        return maskMiddle(value, 4, 4);
    }

    private static String maskMiddle(String value, int prefix, int suffix) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }
        if (value.length() <= prefix + suffix) {
            return "****";
        }
        return value.substring(0, prefix) + "****" + value.substring(value.length() - suffix);
    }
}
