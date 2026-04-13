package com.jy.eleaitender.support.service.impl;

import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.support.service.ISmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 短信验证码服务实现（模拟实现）
 */
@Slf4j
@Service
public class SmsServiceImpl implements ISmsService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String SMS_CODE_PREFIX = "ai:sms:code:";
    private static final int CODE_EXPIRE_MINUTES = 5;
    private static final int SEND_INTERVAL_SECONDS = 60;

    @Override
    public String sendSmsCode(String phone) {
        // 检查发送频率
        String rateLimitKey = SMS_CODE_PREFIX + "rate:" + phone;
        Boolean exists = redisTemplate.hasKey(rateLimitKey);
        if (Boolean.TRUE.equals(exists)) {
            throw new BusinessException("短信发送过于频繁，请" + SEND_INTERVAL_SECONDS + "秒后再试");
        }

        // 生成4位验证码
        String code = String.format("%04d", new Random().nextInt(10000));

        // 存储验证码到Redis，5分钟过期
        String codeKey = SMS_CODE_PREFIX + phone;
        redisTemplate.opsForValue().set(codeKey, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        // 设置发送频率限制
        redisTemplate.opsForValue().set(rateLimitKey, "1", SEND_INTERVAL_SECONDS, TimeUnit.SECONDS);

        // TODO: 实际项目中应调用短信服务API发送验证码
        // 这里仅打印验证码用于测试
        log.info("【模拟短信发送】手机号: {}, 验证码: {}", phone, code);

        return code;
    }

    @Override
    public boolean verifyCode(String phone, String code) {
        String codeKey = SMS_CODE_PREFIX + phone;
        String storedCode = redisTemplate.opsForValue().get(codeKey);

        if (storedCode == null) {
            log.warn("验证码已过期或不存在，手机号: {}", phone);
            return false;
        }

        if (!storedCode.equals(code)) {
            log.warn("验证码错误，手机号: {}, 输入: {}, 正确: {}", phone, code, storedCode);
            return false;
        }

        // 验证成功后删除验证码（一次性使用）
        redisTemplate.delete(codeKey);

        log.info("验证码验证成功，手机号: {}", phone);
        return true;
    }
}
