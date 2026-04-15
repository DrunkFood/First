package com.jy.eleaitender.support.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jy.eleaitender.common.entity.support.SupSmsCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.support.mapper.SmsCodeMapper;
import com.jy.eleaitender.support.service.ISmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 短信验证码服务实现
 * 同时写入Redis（用于快速校验）和数据库（用于审计和持久化）
 */
@Slf4j
@Service
public class SmsServiceImpl implements ISmsService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SmsCodeMapper smsCodeMapper;

    private static final String SMS_CODE_PREFIX = "ai:sms:code:";
    private static final int CODE_EXPIRE_MINUTES = 5;
    private static final int SEND_INTERVAL_SECONDS = 60;

    /** 验证码状态 */
    private static final String STATUS_UNUSED = "UNUSED";
    private static final String STATUS_USED = "USED";
    private static final String STATUS_EXPIRED = "EXPIRED";

    @Override
    public String sendSmsCode(String phone, String scene, String ipAddress) {
        // 检查发送频率
        String rateLimitKey = SMS_CODE_PREFIX + "rate:" + phone;
        Boolean exists = redisTemplate.hasKey(rateLimitKey);
        if (Boolean.TRUE.equals(exists)) {
            throw new BusinessException("短信发送过于频繁，请" + SEND_INTERVAL_SECONDS + "秒后再试");
        }

        // 将之前未使用的验证码标记为过期
        expirePreviousCodes(phone, scene);

        // 生成6位验证码
        String code = String.format("%06d", new Random().nextInt(1000000));

        // 存储验证码到Redis，5分钟过期
        String codeKey = SMS_CODE_PREFIX + phone;
        redisTemplate.opsForValue().set(codeKey, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        // 设置发送频率限制
        redisTemplate.opsForValue().set(rateLimitKey, "1", SEND_INTERVAL_SECONDS, TimeUnit.SECONDS);

        // 持久化到数据库
        SupSmsCode smsCode = new SupSmsCode();
        smsCode.setPhone(phone);
        smsCode.setCode(code);
        smsCode.setScene(scene);
        smsCode.setStatus(STATUS_UNUSED);
        smsCode.setExpireTime(new Date(System.currentTimeMillis() + CODE_EXPIRE_MINUTES * 60 * 1000L));
        smsCode.setIpAddress(ipAddress);
        smsCode.setCreateTime(new Date());
        smsCodeMapper.insert(smsCode);

        // TODO: 实际项目中应调用短信服务API发送验证码
        log.info("【模拟短信发送】手机号: {}, 验证码: {}, 场景: {}", phone, code, scene);

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

        // 验证成功后删除Redis中的验证码（一次性使用）
        redisTemplate.delete(codeKey);

        // 标记数据库中对应的验证码为已使用
        markCodeAsUsed(phone, code);

        log.info("验证码验证成功，手机号: {}", phone);
        return true;
    }

    /**
     * 将之前未使用的验证码标记为过期
     */
    private void expirePreviousCodes(String phone, String scene) {
        LambdaQueryWrapper<SupSmsCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupSmsCode::getPhone, phone)
               .eq(SupSmsCode::getStatus, STATUS_UNUSED);
        if (scene != null) {
            wrapper.eq(SupSmsCode::getScene, scene);
        }

        List<SupSmsCode> unusedCodes = smsCodeMapper.selectList(wrapper);
        Date now = new Date();
        for (SupSmsCode smsCode : unusedCodes) {
            smsCode.setStatus(STATUS_EXPIRED);
            smsCodeMapper.updateById(smsCode);
        }
    }

    /**
     * 标记验证码为已使用
     */
    private void markCodeAsUsed(String phone, String code) {
        LambdaQueryWrapper<SupSmsCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupSmsCode::getPhone, phone)
               .eq(SupSmsCode::getCode, code)
               .eq(SupSmsCode::getStatus, STATUS_UNUSED)
               .orderByDesc(SupSmsCode::getCreateTime)
               .last("LIMIT 1");

        SupSmsCode smsCode = smsCodeMapper.selectOne(wrapper);
        if (smsCode != null) {
            smsCode.setStatus(STATUS_USED);
            smsCode.setUsedTime(new Date());
            smsCodeMapper.updateById(smsCode);
        }
    }
}
