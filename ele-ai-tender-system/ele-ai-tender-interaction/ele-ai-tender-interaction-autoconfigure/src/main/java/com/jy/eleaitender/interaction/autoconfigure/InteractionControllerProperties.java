package com.jy.eleaitender.interaction.autoconfigure;

import com.jy.eleaitender.common.interaction.constant.InteractionApiPaths;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 交互控制器配置
 */
@Data
@ConfigurationProperties(prefix = "ele-ai-tender.interaction.controller")
public class InteractionControllerProperties {

    private String basePath = InteractionApiPaths.BASE;
}
