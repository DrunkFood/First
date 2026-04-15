package com.jy.eleaitender.support.schedule;

import com.jy.eleaitender.common.util.RsaKeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * RSA密钥定时轮换任务
 * 每2小时轮换一次密钥对
 */
@Slf4j
@Component
public class RsaKeyRotateTask {

    @Scheduled(fixedRate = 2 * 60 * 60 * 1000)
    public void rotateRsaKey() {
        log.info("开始轮换RSA密钥对...");
        RsaKeyUtil.rotate();
    }
}
