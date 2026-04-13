package com.jy.eleaitender.support.service;

/**
 * 短信验证码服务接口
 */
public interface ISmsService {

    /**
     * 发送验证码（模拟实现，实际应调用短信服务）
     * 
     * @param phone 手机号
     * @return 验证码（仅用于测试，实际不返回）
     */
    String sendSmsCode(String phone);

    /**
     * 验证验证码
     * 
     * @param phone 手机号
     * @param code 验证码
     * @return 是否验证成功
     */
    boolean verifyCode(String phone, String code);
}
