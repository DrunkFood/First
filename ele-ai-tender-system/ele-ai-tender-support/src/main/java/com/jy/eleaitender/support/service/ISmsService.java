package com.jy.eleaitender.support.service;

/**
 * 短信验证码服务接口
 */
public interface ISmsService {

    /**
     * 发送验证码
     *
     * @param phone 手机号
     * @param scene 使用场景: LOGIN/REGISTER/RESET_PWD/BIND_PHONE
     * @param ipAddress 发送方IP地址
     */
    void sendSmsCode(String phone, String scene, String ipAddress);

    /**
     * 验证验证码
     *
     * @param phone 手机号
     * @param code 验证码
     * @return 是否验证成功
     */
    boolean verifyCode(String phone, String code);

    /**
     * 验证指定场景验证码
     *
     * @param phone 手机号
     * @param code 验证码
     * @param scene 使用场景: LOGIN/REGISTER/RESET_PWD/BIND_PHONE
     * @return 是否验证成功
     */
    boolean verifyCode(String phone, String code, String scene);
}
