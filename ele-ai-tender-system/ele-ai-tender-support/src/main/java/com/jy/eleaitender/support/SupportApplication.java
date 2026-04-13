package com.jy.eleaitender.support;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan({"com.jy.eleaitender.support.mapper", "com.jy.eleaitender.common.mapper"})
@ComponentScan(basePackages = {"com.jy.eleaitender.support", "com.jy.eleaitender.common"})
public class SupportApplication {
    public static void main(String[] args) {
        SpringApplication.run(SupportApplication.class, args);
        System.out.println("====================================");
        System.out.println("Ele AI Tender Support Center started!");
        System.out.println("====================================");
    }
}
