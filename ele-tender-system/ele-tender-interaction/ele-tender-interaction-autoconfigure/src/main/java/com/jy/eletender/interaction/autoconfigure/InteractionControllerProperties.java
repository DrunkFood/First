package com.jy.eletender.interaction.autoconfigure;

import com.jy.eletender.common.interaction.constant.InteractionApiPaths;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 交互控制器配置
 */
@Data
@ConfigurationProperties(prefix = "ele-tender.interaction.controller")
public class InteractionControllerProperties {

    private String basePath = InteractionApiPaths.BASE;
}
