package com.jy.eletender.common.logging;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 访问日志配置
 */
@Data
@ConfigurationProperties(prefix = "ele-tender.logging.access")
public class AccessLogProperties {

    private boolean enabled = true;

    private boolean persistEnabled = true;

    private boolean asyncPersist = true;

    private int bodyMaxLength = 4000;

    private int errorMessageMaxLength = 1000;
}
