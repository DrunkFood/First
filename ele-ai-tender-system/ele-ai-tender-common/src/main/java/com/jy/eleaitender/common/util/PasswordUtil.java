package com.jy.eleaitender.common.utils;

import cn.hutool.crypto.digest.BCrypt;
import org.apache.commons.lang3.StringUtils;

/**
 * 密码工具类
 */
public class PasswordUtil {

    private PasswordUtil() {}

    /**
     * 加密密码
     *
     * @param password 明文密码
     * @return BCrypt 加密后的密码
     */
    public static String encode(String password) {
        if (StringUtils.isBlank(password)) {
            return null;
        }
        return BCrypt.hashpw(password);
    }

    /**
     * 验证密码
     *
     * @param rawPassword     明文密码
     * @param encodedPassword BCrypt 加密后的密码
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (StringUtils.isBlank(rawPassword) || StringUtils.isBlank(encodedPassword)) {
            return false;
        }
        if (!encodedPassword.startsWith("$2")) {
            return false;
        }
        return BCrypt.checkpw(rawPassword, encodedPassword);
    }

    /**
     * 生成随机密码
     *
     * @param length 密码长度
     * @return 随机密码
     */
    public static String generateRandomPassword(int length) {
        return cn.hutool.core.util.RandomUtil.randomString(length);
    }

    /**
     * 生成默认长度的随机密码（8位）
     *
     * @return 随机密码
     */
    public static String generateRandomPassword() {
        return generateRandomPassword(8);
    }
}
