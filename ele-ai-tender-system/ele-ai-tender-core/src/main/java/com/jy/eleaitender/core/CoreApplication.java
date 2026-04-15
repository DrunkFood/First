package com.jy.eleaitender.core;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI编制系统核心业务模块启动类
 */
@SpringBootApplication
@MapperScan({"com.jy.eleaitender.core.mapper", "com.jy.eleaitender.common.mapper"})
@ComponentScan(basePackages = {"com.jy.eleaitender.core", "com.jy.eleaitender.common"})
@EnableScheduling
public class CoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoreApplication.class, args);
        System.out.println("====================================");
        System.out.println("Ele AI Tender Core Service started!");
        System.out.println("====================================");
    }
}
