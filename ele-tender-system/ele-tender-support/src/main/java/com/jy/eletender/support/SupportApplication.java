package com.jy.eletender.support;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 支撑中心启动类
 */
@SpringBootApplication
@MapperScan({"com.jy.eletender.support.mapper", "com.jy.eletender.common.mapper"})
public class SupportApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupportApplication.class, args);
    }
}
