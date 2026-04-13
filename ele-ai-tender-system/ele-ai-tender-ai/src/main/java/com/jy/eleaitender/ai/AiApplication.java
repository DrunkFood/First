package com.jy.eleaitender.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * AI编制系统AI服务模块启动类
 */
@SpringBootApplication
@MapperScan({"com.jy.eleaitender.ai.mapper", "com.jy.eleaitender.common.mapper"})
@ComponentScan(basePackages = {"com.jy.eleaitender.ai", "com.jy.eleaitender.common"})
public class AiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
        System.out.println("====================================");
        System.out.println("Ele AI Tender AI Service started!");
        System.out.println("====================================");
    }
}
