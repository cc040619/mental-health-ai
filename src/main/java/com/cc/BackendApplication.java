package com.cc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Spring Boot 项目主启动类
 *
 * @author cc
 * @date 2026-09-01
 */
@EnableAsync
@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        // 启动 Spring 容器
        SpringApplication.run(BackendApplication.class, args);
    }

}
