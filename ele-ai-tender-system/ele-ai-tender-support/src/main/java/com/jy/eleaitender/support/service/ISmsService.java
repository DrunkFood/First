package com.jy.eleaitender.support.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jy.eleaitender.common.entity.support.SupSmsCode;

/**
 * 短信验证码服务接口
 */
public interface ISmsService extends IService<SupSmsCode> {

    /**
     * 发送验证码
     *
     * @param phone     手机号
     * @param scene     使用场景: LOGIN/REGISTER/RESET_PWD/BIND_PHONE
     * @param ipAddress 发送方IP地址
     * @return 非正式发送模式下返回验证码，正式发送模式下返回null
     */
    String sendSmsCode(String phone, String scene, String ipAddress);

    /**
     * 验证验证码
     *
     * @param phone 手机号
     * @param code  验证码
     * @return 是否验证成功
     */
    boolean verifyCode(String phone, String code);

    /**
     * 验证指定场景验证码
     *
     * @param phone 手机号
     * @param code  验证码
     * @param scene 使用场景: LOGIN/REGISTER/RESET_PWD/BIND_PHONE
     * @return 是否验证成功
     */
    boolean verifyCode(String phone, String code, String scene);
}
