package com.t3a.ai.service;

import cn.hutool.core.util.IdUtil;
import com.t3a.ai.domain.dto.GeneratedQuestion;
import com.t3a.ai.domain.dto.GenerateQuestionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.io.IOException;

/**
 * 题目生成任务服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionGenerationTaskService {

    private final DocumentParserService documentParser;
    private final QuestionGenerationService questionGenerator;
    private final StringRedisTemplate redisTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.core-base-url:http://localhost:8081/quiz}")
    private String coreBaseUrl;

    /**
     * 提交题目生成任务
     */
    public String submitGenerationTask(MultipartFile file, GenerateQuestionRequest request, String authorizationHeader) {
        // 生成任务ID
        String taskId = IdUtil.simpleUUID();

        // 更新任务状态为处理中
        updateTaskStatus(taskId, "PROCESSING", "正在解析文件...", 0, null);

        final String originalFilename = file.getOriginalFilename();
        final byte[] fileBytes;
        try {
            documentParser.validateFileSize(file.getSize());
            documentParser.validateFileType(originalFilename);
            fileBytes = file.getBytes();
        } catch (IOException e) {
            updateTaskStatus(taskId, "FAILED", "生成失败: 读取上传文件失败", 0, null);
            throw new RuntimeException("读取上传文件失败", e);
        } catch (Exception e) {
            updateTaskStatus(taskId, "FAILED", "生成失败: " + e.getMessage(), 0, null);
            throw e;
        }

        // 异步处理
        CompletableFuture.runAsync(() -> {
            try {
                // 1. 解析文档
                updateTaskStatus(taskId, "PROCESSING", "正在解析文档内容...", 20, null);
                String textContent = documentParser.parseDocument(originalFilename, fileBytes);

                // 2. 生成题目
                updateTaskStatus(taskId, "PROCESSING", "正在生成题目（这可能需要30秒）...", 40, null);
                List<GeneratedQuestion> questions = questionGenerator.generateQuestions(textContent, request);

                // 3. 保存题目到数据库
                updateTaskStatus(taskId, "PROCESSING", "正在保存题目...", 80, null);
                Long bankId = saveQuestionsToDatabase(questions, request, originalFilename, authorizationHeader);

                // 4. 完成
                updateTaskStatus(taskId, "COMPLETED", "题目生成完成！", 100, bankId);
                saveTaskResult(taskId, questions);

            } catch (Exception e) {
                log.error("题目生成任务失败: taskId={}", taskId, e);
                updateTaskStatus(taskId, "FAILED", "生成失败: " + e.getMessage(), 0, null);
            }
        });

        return taskId;
    }

    /**
     * 更新任务状态
     */
    private void updateTaskStatus(String taskId, String status, String message, int progress, Long bankId) {
        String key = "quiz:generation:task:" + taskId;
        Map<String, Object> statusData = new LinkedHashMap<>();
        statusData.put("status", status);
        statusData.put("message", message);
        statusData.put("progress", progress);
        statusData.put("timestamp", System.currentTimeMillis());
        if (bankId != null) {
            statusData.put("bankId", bankId);
        }

        String value;
        try {
            value = objectMapper.writeValueAsString(statusData);
        } catch (Exception e) {
            value = String.format("{\"status\":\"%s\",\"message\":\"%s\",\"progress\":%d,\"timestamp\":%d}",
                    status, message, progress, System.currentTimeMillis());
        }

        redisTemplate.opsForValue().set(key, value, 1, TimeUnit.HOURS);
        log.info("任务状态更新: taskId={}, status={}, progress={}%", taskId, status, progress);
    }

    /**
     * 保存任务结果
     */
    private void saveTaskResult(String taskId, List<GeneratedQuestion> questions) {
        String key = "quiz:generation:result:" + taskId;
        try {
            // 这里简化处理，实际应该序列化为JSON
            redisTemplate.opsForValue().set(key, String.valueOf(questions.size()), 1, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("保存任务结果失败", e);
        }
    }

    /**
     * 查询任务状态
     */
    public String getTaskStatus(String taskId) {
        String key = "quiz:generation:task:" + taskId;
        String status = redisTemplate.opsForValue().get(key);

        if (status == null) {
            return "{\"status\":\"NOT_FOUND\",\"message\":\"任务不存在\"}";
        }

        return status;
    }

    /**
     * 保存题目到数据库（通过Feign调用quiz-core）
     */
    private Long saveQuestionsToDatabase(
            List<GeneratedQuestion> questions,
            GenerateQuestionRequest request,
            String originalFilename,
            String authorizationHeader
    ) {
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("AI 未生成有效题目");
        }
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new IllegalArgumentException("缺少登录凭证，请重新登录后重试");
        }

        Long bankId = request.getBankId();
        if (bankId == null) {
            bankId = createBank(request, originalFilename, authorizationHeader);
        }

        List<Map<String, Object>> payload = new ArrayList<>();
        for (GeneratedQuestion question : questions) {
            Map<String, Object> item = new LinkedHashMap<>();
            String correctAnswer = question.getCorrectAnswer();
            if (("SHORT_ANSWER".equalsIgnoreCase(question.getQuestionType())
                    || "CODE".equalsIgnoreCase(question.getQuestionType()))
                    && (correctAnswer == null || correctAnswer.isBlank())) {
                String explanation = question.getExplanation();
                if (explanation != null && !explanation.isBlank()) {
                    String trimmed = explanation.trim();
                    correctAnswer = trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
                } else {
                    correctAnswer = "请结合题目要点作答";
                }
            }
            item.put("bankId", bankId);
            item.put("questionType", question.getQuestionType());
            item.put("content", question.getContent());
            item.put("options", toJson(question.getOptions()));
            item.put("correctAnswer", correctAnswer);
            item.put("explanation", question.getExplanation());
            item.put("difficulty", question.getDifficulty());
            item.put("tags", question.getTags() == null ? null : question.getTags().stream().collect(Collectors.joining(",")));
            item.put("score", question.getScore() == null ? 10 : question.getScore());
            item.put("aiGenerated", true);
            payload.add(item);
        }

        HttpHeaders headers = buildJsonHeaders(authorizationHeader);
        HttpEntity<List<Map<String, Object>>> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                coreBaseUrl + "/question/batch",
                HttpMethod.POST,
                entity,
                Map.class
        );
        Integer code = extractCode(response.getBody());
        if (code == null || code != 200) {
            throw new RuntimeException("保存题目失败: " + response.getBody());
        }
        log.info("保存 {} 道题目到题库 {} 成功", payload.size(), bankId);
        return bankId;
    }

    private Long createBank(GenerateQuestionRequest request, String originalFilename, String authorizationHeader) {
        String filename = originalFilename == null ? "AI题库" : originalFilename;
        int dot = filename.lastIndexOf('.');
        String bankName = request.getBankName();
        if (bankName == null || bankName.isBlank()) {
            bankName = dot > 0 ? filename.substring(0, dot) : filename;
        }

        Map<String, Object> bankPayload = new LinkedHashMap<>();
        bankPayload.put("name", bankName);
        bankPayload.put("description", "由AI根据上传材料自动生成");
        bankPayload.put("category", request.getCategory() == null ? "AI" : request.getCategory());
        bankPayload.put("creatorId", 1L);
        bankPayload.put("isPublic", true);
        bankPayload.put("aiGenerated", true);

        HttpHeaders headers = buildJsonHeaders(authorizationHeader);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(bankPayload, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                coreBaseUrl + "/bank/create",
                HttpMethod.POST,
                entity,
                Map.class
        );
        Integer code = extractCode(response.getBody());
        if (code == null || code != 200) {
            throw new RuntimeException("创建题库失败: " + response.getBody());
        }

        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        if (data == null || data.get("id") == null) {
            throw new RuntimeException("创建题库返回异常: " + response.getBody());
        }
        Long bankId = Long.valueOf(String.valueOf(data.get("id")));
        log.info("创建题库成功: id={}, name={}", bankId, bankName);
        return bankId;
    }

    private Integer extractCode(Map body) {
        if (body == null || body.get("code") == null) {
            return null;
        }
        return Integer.valueOf(String.valueOf(body.get("code")));
    }

    private HttpHeaders buildJsonHeaders(String authorizationHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        return headers;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("序列化题目选项失败", e);
        }
    }
}
