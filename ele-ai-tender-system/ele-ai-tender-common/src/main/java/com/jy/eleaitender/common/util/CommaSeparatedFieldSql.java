package com.jy.eleaitender.common.util;

/**
 * Utilities for fields that store dictionary codes as comma-separated values.
 */
public final class CommaSeparatedFieldSql {

    private CommaSeparatedFieldSql() {
    }

    public static String contains(String columnName) {
        return "CONCAT(',', REPLACE(" + columnName + ", ' ', ''), ',') LIKE CONCAT('%,', {0}, ',%')";
    }

    public static boolean containsValue(String fieldValue, String expectedValue) {
        if (!hasText(fieldValue) || !hasText(expectedValue)) {
            return false;
        }
        String normalizedExpected = expectedValue.trim();
        String[] values = fieldValue.split(",");
        for (String value : values) {
            if (normalizedExpected.equals(value.trim())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
