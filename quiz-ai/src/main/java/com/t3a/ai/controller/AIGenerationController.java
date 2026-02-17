package com.t3a.ai.controller;

import com.t3a.ai.domain.dto.GenerateQuestionRequest;
import com.t3a.ai.service.QuestionGenerationTaskService;
import com.t3a.common.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * AI 题目生成接口
 */
@Slf4j
@RestController
@RequestMapping("/generation")
@Tag(name = "AI Generation", description = "AI题目生成接口")
public class AIGenerationController {

    private final QuestionGenerationTaskService generationTaskService;

    private final ChatModel deepSeekModel;

//    private final ChatModel kimiModel;

    public AIGenerationController(QuestionGenerationTaskService generationTaskService, @Qualifier("deepSeekChatModel") ChatModel deepSeekModel) {
        this.generationTaskService = generationTaskService;
        this.deepSeekModel = deepSeekModel;
//        this.kimiModel = kimiModel;
    }

    @Operation(summary = "上传学习材料并生成题目")
    @PostMapping(value = "/generate", consumes = "multipart/form-data")
    public Result<Map<String, String>> generateQuestions(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "request", required = false) String requestJson,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        log.info("接收题目生成请求: file={}, requestJson={}",
                file.getOriginalFilename(), requestJson);

        // Manually parse JSON
        GenerateQuestionRequest request;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            request = mapper.readValue(requestJson, GenerateQuestionRequest.class);
        } catch (Exception e) {
            log.error("解析请求参数失败", e);
            return Result.error("请求参数格式错误: " + e.getMessage());
        }

        log.info("解析后的请求: count={}, difficulty={}", request.getCount(), request.getDifficulty());

        try {
            String taskId = generationTaskService.submitGenerationTask(file, request, authorization);
            return Result.success("题目生成任务已提交", Map.of(
                    "taskId", taskId,
                    "message", "任务处理中，预计30秒完成",
                    "statusUrl", "/generation/status/" + taskId
            ));
        } catch (Exception e) {
            log.error("提交生成任务失败", e);
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "查询生成任务状态")
    @GetMapping("/status/{taskId}")
    public Result<String> getGenerationStatus(@PathVariable String taskId) {
        log.info("查询生成任务状态: taskId={}", taskId);
        try {
            String status = generationTaskService.getTaskStatus(taskId);
            return Result.success(status);
        } catch (Exception e) {
            log.error("查询任务状态失败", e);
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "测试聊天功能")
    @GetMapping("/chat")
    public String chatWithDeepSeek(@RequestParam String message) {
        return deepSeekModel.call(message);
    }
}
