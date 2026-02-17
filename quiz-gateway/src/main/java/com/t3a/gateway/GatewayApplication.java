package com.t3a.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API Gateway 启动类
 * 负责路由、认证和限流
 */
@SpringBootApplication(exclude = {
    // 不排除任何自动配置，通过配置文件禁用CORS
})
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
