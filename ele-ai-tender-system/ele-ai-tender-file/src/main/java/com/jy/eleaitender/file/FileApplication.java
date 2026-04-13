package com.jy.eleaitender.file;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 文件服务启动类
 */
@SpringBootApplication
@MapperScan({"com.jy.eleaitender.file.mapper", "com.jy.eleaitender.common.mapper"})
public class FileApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileApplication.class, args);
    }
}
