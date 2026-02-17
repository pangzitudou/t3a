package com.t3a.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Quiz AI 启动类
 * 负责AI题目生成、主观题评分和知识图谱分析
 */
@SpringBootApplication
@EnableDiscoveryClient
public class QuizAIApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuizAIApplication.class, args);
    }
}
