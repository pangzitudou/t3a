package com.t3a.communication.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * WebSocket 消息处理控制器
 */
@Slf4j
@Controller
public class WebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 接收客户端进度更新
     */
    @MessageMapping("/quiz/progress")
    @SendTo("/topic/quiz/progress")
    public String handleProgress(String message) {
        log.info("接收到进度更新: {}", message);
        return message;
    }

    /**
     * 推送实时评分结果
     */
    public void pushScore(String userId, Object scoreData) {
        String destination = "/topic/quiz/score/" + userId;
        messagingTemplate.convertAndSend(destination, scoreData);
        log.info("推送评分结果到: {}", destination);
    }

    /**
     * 推送AI分析结果
     */
    public void pushAnalysis(String userId, Object analysisData) {
        String destination = "/topic/quiz/analysis/" + userId;
        messagingTemplate.convertAndSend(destination, analysisData);
        log.info("推送分析结果到: {}", destination);
    }
}
