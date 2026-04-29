package com.jy.eleaitender.common.util;

/**
 * 文本规范化工具类
 * 用于检测原文模糊匹配时的空白字符处理
 */
public final class TextNormalizeUtil {

    private TextNormalizeUtil() {}

    /**
     * 规范化文本：移除所有空白字符
     * \s 不覆盖不间断空格(U+00A0)和全角空格(U+3000)，需显式补充
     */
    public static String normalize(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\s\\u00A0\\u3000]+", "");
    }

    /**
     * 判断字符是否为空白（包括不间断空格和全角空格）
     * 注意：U+00A0 和 U+3000 必须用转义写法，字面量在源码中不可见易误改
     */
    public static boolean isWhitespaceChar(char c) {
        return Character.isWhitespace(c) || c == ' ' || c == '　';
    }
}
