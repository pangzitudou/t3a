package com.t3a.communication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Quiz Communication 启动类
 * 负责实时WebSocket通信
 */
@SpringBootApplication
@EnableDiscoveryClient
public class QuizCommunicationApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuizCommunicationApplication.class, args);
    }
}
