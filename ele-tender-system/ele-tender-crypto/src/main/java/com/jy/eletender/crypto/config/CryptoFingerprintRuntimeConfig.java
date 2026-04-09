package com.jy.eletender.crypto.config;

import com.jy.eletender.crypto.support.CryptoFingerprintUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Configuration;

@Configuration
/**
 * 投标口令指纹运行时初始化。
 * 指纹 secret 缺失时直接阻止服务启动，避免口令指纹算法退化或出现不同环境结果不一致。
 */
public class CryptoFingerprintRuntimeConfig {

    public CryptoFingerprintRuntimeConfig(CryptoProperties cryptoProperties) {
        String secret = cryptoProperties.getBidderPwdFingerprintSecret();
        if (StringUtils.isBlank(secret)) {
            throw new IllegalStateException(
                    "crypto.bidder-pwd-fingerprint-secret 未配置，服务无法启动。" +
                    "请设置环境变量 CRYPTO_BIDDER_PWD_FINGERPRINT_SECRET。");
        }
        CryptoFingerprintUtil.configure(secret);
    }
}
