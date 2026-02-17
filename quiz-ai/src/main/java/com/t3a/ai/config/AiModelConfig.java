package com.t3a.ai.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiModelConfig {

    // --- DeepSeek 配置 ---
    @Value("${app.models.deepseek.base-url}")
    private String deepSeekBaseUrl;

    @Value("${app.models.deepseek.api-key}")
    private String deepSeekApiKey;

    @Value("${app.models.deepseek.model}")
    private String deepSeekModelName;

    @Value("${app.models.deepseek.temperature}")
    private Double deepSeekTemperature;

    @Bean(name = "deepSeekChatModel")
    public ChatModel deepSeekChatModel() {
        // 1. 创建 API 绑定，指定 Base URL
        OpenAiApi deepSeekApi = OpenAiApi.builder()
                .baseUrl(deepSeekBaseUrl)
                .apiKey(deepSeekApiKey)
                .build();

        // 2. 配置默认选项
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(deepSeekModelName)
                .temperature(deepSeekTemperature)
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(deepSeekApi)
                .defaultOptions(options)
                .build();
    }

    // --- Kimi (Moonshot) 配置 ---
//    @Value("${app.models.kimi.base-url}")
//    private String kimiBaseUrl;
//
//    @Value("${app.models.kimi.api-key}")
//    private String kimiApiKey;
//
//    @Value("${app.models.kimi.model}")
//    private String kimiModelName;
//
//    @Value("${app.models.kimi.temperature}")
//    private Double kimiTemperature;
//
//    @Bean(name = "kimiChatModel")
//    public ChatModel kimiChatModel() {
//        // 1. 创建 API 绑定，指定 Base URL
//        OpenAiApi deepSeekApi = OpenAiApi.builder()
//                .baseUrl(kimiBaseUrl)
//                .apiKey(kimiApiKey)
//                .build();
//
//        // 2. 配置默认选项
//        OpenAiChatOptions options = OpenAiChatOptions.builder()
//                .model(kimiModelName)
//                .temperature(kimiTemperature)
//                .build();
//
//        // 3. 返回 ChatModel
//        return OpenAiChatModel.builder().openAiApi(deepSeekApi)
//                .defaultOptions(options)
//                .build();
//    }
}