package com.jy.eletender.tenderdocument;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 招标文件编制系统启动类
 */
@SpringBootApplication
@MapperScan({"com.jy.eletender.tenderdocument.mapper", "com.jy.eletender.common.mapper"})
public class TenderDocumentApplication {

    public static void main(String[] args) {
        SpringApplication.run(TenderDocumentApplication.class, args);
    }
}
