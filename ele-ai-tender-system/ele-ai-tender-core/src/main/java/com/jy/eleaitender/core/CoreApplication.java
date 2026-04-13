package com.jy.eleaitender.core;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.jy.eleaitender.core.mapper")
@ComponentScan(basePackages = {"com.jy.eleaitender.core", "com.jy.eleaitender.common"})
public class CoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoreApplication.class, args);
        System.out.println("====================================");
        System.out.println("Ele AI Tender Core Service started!");
        System.out.println("====================================");
    }
}
