package com.jy.eleaitender.support.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jy.eleaitender.common.entity.support.SupSmsCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.support.mapper.SmsCodeMapper;
import com.jy.eleaitender.support.service.ISmsService;
import com.jy.eleaitender.support.service.SmsGatewayClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
public class SmsServiceImpl extends ServiceImpl<SmsCodeMapper, SupSmsCode> implements ISmsService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private SmsCodeMapper smsCodeMapper;

    @Autowired
    private SmsGatewayClient smsGatewayClient;

    private static final String SMS_CODE_PREFIX = "ai:sms:code:";
    private static final int CODE_EXPIRE_MINUTES = 5;
    private static final int SEND_INTERVAL_SECONDS = 60;
    private static final String DEFAULT_SCENE = "LOGIN";

    /** 验证码状态 */
    private static final String STATUS_UNUSED = "UNUSED";
    private static final String STATUS_USED = "USED";
    private static final String STATUS_EXPIRED = "EXPIRED";

    @Override
    public String sendSmsCode(String phone, String scene, String ipAddress) {
        String normalizedScene = normalizeScene(scene);

        // 检查发送频率
        String rateLimitKey = SMS_CODE_PREFIX + "rate:" + phone;
        Boolean exists = redisTemplate.hasKey(rateLimitKey);
        if (Boolean.TRUE.equals(exists)) {
            throw new BusinessException("短信发送过于频繁，请" + SEND_INTERVAL_SECONDS + "秒后再试");
        }

        // 将之前未使用的验证码标记为过期
        expirePreviousCodes(phone, normalizedScene);

        // 生成6位验证码
        String code = String.format("%06d", new Random().nextInt(1000000));

        // 真实发送成功后，再写入可校验验证码
        boolean formalSent = smsGatewayClient.sendCode(phone, code, normalizedScene);

        // 存储验证码到Redis，5分钟过期
        String codeKey = buildCodeKey(phone, normalizedScene);
        redisTemplate.opsForValue().set(codeKey, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);

        // 设置发送频率限制
        redisTemplate.opsForValue().set(rateLimitKey, "1", SEND_INTERVAL_SECONDS, TimeUnit.SECONDS);

        // 持久化到数据库
        SupSmsCode smsCode = new SupSmsCode();
        smsCode.setPhone(phone);
        smsCode.setCode(code);
        smsCode.setScene(normalizedScene);
        smsCode.setStatus(STATUS_UNUSED);
        smsCode.setExpireTime(new Date(System.currentTimeMillis() + CODE_EXPIRE_MINUTES * 60 * 1000L));
        smsCode.setIpAddress(ipAddress);
        smsCode.setCreateTime(new Date());
        smsCodeMapper.insert(smsCode);

        return formalSent ? null : code;
    }

    @Override
    public boolean verifyCode(String phone, String code) {
        return verifyCode(phone, code, DEFAULT_SCENE);
    }

    @Override
    public boolean verifyCode(String phone, String code, String scene) {
        String normalizedScene = normalizeScene(scene);
        String codeKey = buildCodeKey(phone, normalizedScene);
        String storedCode = redisTemplate.opsForValue().get(codeKey);

        if (storedCode == null) {
            log.warn("验证码已过期或不存在，手机号: {}, 场景: {}", phone, normalizedScene);
            return false;
        }

        if (!storedCode.equals(code)) {
            log.warn("验证码错误，手机号: {}, 场景: {}", phone, normalizedScene);
            return false;
        }

        // 验证成功后删除Redis中的验证码（一次性使用）
        redisTemplate.delete(codeKey);

        // 标记数据库中对应的验证码为已使用
        markCodeAsUsed(phone, code, normalizedScene);

        log.info("验证码验证成功，手机号: {}, 场景: {}", phone, normalizedScene);
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
        for (SupSmsCode smsCode : unusedCodes) {
            smsCode.setStatus(STATUS_EXPIRED);
            smsCodeMapper.updateById(smsCode);
        }
    }

    /**
     * 标记验证码为已使用
     */
    private void markCodeAsUsed(String phone, String code, String scene) {
        LambdaQueryWrapper<SupSmsCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupSmsCode::getPhone, phone)
               .eq(SupSmsCode::getCode, code)
               .eq(SupSmsCode::getScene, scene)
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

    private String buildCodeKey(String phone, String scene) {
        return SMS_CODE_PREFIX + scene + ":" + phone;
    }

    private String normalizeScene(String scene) {
        return StringUtils.defaultIfBlank(scene, DEFAULT_SCENE);
    }
}
