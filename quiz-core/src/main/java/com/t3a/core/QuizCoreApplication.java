package com.t3a.core;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Quiz Core 启动类
 * 负责题库管理、随机化逻辑和会话状态跟踪
 */
@SpringBootApplication
@EnableDiscoveryClient  // Disabled for simple mode
@MapperScan("com.t3a.core.mapper")  // Disabled for simple mode
public class QuizCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuizCoreApplication.class, args);
    }
}
