package com.jy.eleaitender.support.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI配置
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EleTender 支撑中心 API")
                        .description("支撑中心服务接口文档")
                        .version("1.0.0")
                        .contact(new Contact().name("EleTender")));
    }
}
