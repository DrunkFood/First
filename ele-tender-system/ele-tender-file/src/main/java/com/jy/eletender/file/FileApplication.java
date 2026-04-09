package com.jy.eletender.file;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 文件服务启动类
 */
@SpringBootApplication
@MapperScan({"com.jy.eletender.file.mapper", "com.jy.eletender.common.mapper"})
public class FileApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileApplication.class, args);
    }
}
