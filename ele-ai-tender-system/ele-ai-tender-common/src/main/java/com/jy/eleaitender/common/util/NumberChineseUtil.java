package com.jy.eleaitender.common.util;

import java.util.Map;

public final class NumberChineseUtil {

    private static final String[] CHINESE_DIGITS = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九"};
    private static final Map<Character, Integer> DIGIT_MAP = Map.of(
            '零', 0,
            '一', 1,
            '二', 2,
            '三', 3,
            '四', 4,
            '五', 5,
            '六', 6,
            '七', 7,
            '八', 8,
            '九', 9
    );

    private NumberChineseUtil() {
    }

    public static String toChineseNumber(int number) {
        if (number < 0) {
            throw new IllegalArgumentException("number must be >= 0");
        }
        if (number < 10) {
            return CHINESE_DIGITS[number];
        }
        if (number < 20) {
            return "十" + (number % 10 == 0 ? "" : CHINESE_DIGITS[number % 10]);
        }
        if (number < 100) {
            int tens = number / 10;
            int ones = number % 10;
            return CHINESE_DIGITS[tens] + "十" + (ones == 0 ? "" : CHINESE_DIGITS[ones]);
        }
        throw new IllegalArgumentException("number must be < 100");
    }

    public static int toArabicNumber(String chineseNumber) {
        if (chineseNumber == null || chineseNumber.isBlank()) {
            throw new IllegalArgumentException("chineseNumber must not be blank");
        }
        String value = chineseNumber.trim();
        if ("十".equals(value)) {
            return 10;
        }
        if (!value.contains("十")) {
            return digitOf(value);
        }
        String[] parts = value.split("十", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Unsupported chinese number: " + chineseNumber);
        }
        int tens = parts[0].isEmpty() ? 1 : digitOf(parts[0]);
        int ones = parts[1].isEmpty() ? 0 : digitOf(parts[1]);
        return tens * 10 + ones;
    }

    private static int digitOf(String chineseDigit) {
        if (chineseDigit.length() != 1) {
            throw new IllegalArgumentException("Unsupported chinese number: " + chineseDigit);
        }
        Integer value = DIGIT_MAP.get(chineseDigit.charAt(0));
        if (value == null) {
            throw new IllegalArgumentException("Unsupported chinese number: " + chineseDigit);
        }
        return value;
    }
}
