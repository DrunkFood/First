package com.jy.eleaitender.common.constant;

import java.util.regex.Pattern;

public final class FileConstants {

    public static final String DEFAULT_TENDER_DOCUMENT_SUFFIX = ".HzctZbs";
    public static final String DEFAULT_BID_DOCUMENT_SUFFIX = ".HzctTbs";

    private static final Pattern SUFFIX_PATTERN = Pattern.compile("^\\.[A-Za-z0-9]{1,31}$");

    private FileConstants() {
    }

    /**
     * 校验文件后缀格式。
     * <ul>
     *   <li>null / 空白 → 返回 null</li>
     *   <li>合法（以 . 开头，仅含字母数字，总长度 2–32） → 原样返回</li>
     *   <li>不合法 → 抛出 {@link IllegalArgumentException}</li>
     * </ul>
     *
     * @param suffix 待校验的文件后缀
     * @return 原始后缀，或 null（当输入为空白时）
     */
    public static String validateDocumentSuffix(String suffix) {
        if (suffix == null || suffix.isBlank()) {
            return null;
        }
        if (!SUFFIX_PATTERN.matcher(suffix).matches()) {
            throw new IllegalArgumentException("文件后缀格式不合法: " + suffix
                    + "，要求以 . 开头，仅含字母数字，最长32字符");
        }
        return suffix;
    }
}
