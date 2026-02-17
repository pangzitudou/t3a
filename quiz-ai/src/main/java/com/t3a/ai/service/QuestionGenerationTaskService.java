package com.t3a.ai.service;

import cn.hutool.core.util.IdUtil;
import com.t3a.ai.domain.dto.GeneratedQuestion;
import com.t3a.ai.domain.dto.GenerateQuestionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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

    /**
     * 提交题目生成任务
     */
    public String submitGenerationTask(MultipartFile file, GenerateQuestionRequest request) {
        // 生成任务ID
        String taskId = IdUtil.simpleUUID();

        // 更新任务状态为处理中
        updateTaskStatus(taskId, "PROCESSING", "正在解析文件...", 0);

        // 异步处理
        CompletableFuture.runAsync(() -> {
            try {
                // 1. 验证文件
                documentParser.validateFileSize(file);
                documentParser.validateFileType(file);

                // 2. 解析文档
                updateTaskStatus(taskId, "PROCESSING", "正在解析文档内容...", 20);
                String textContent = documentParser.parseDocument(file);

                // 3. 生成题目
                updateTaskStatus(taskId, "PROCESSING", "正在生成题目（这可能需要30秒）...", 40);
                List<GeneratedQuestion> questions = questionGenerator.generateQuestions(textContent, request);

                // 4. 保存题目到数据库
                updateTaskStatus(taskId, "PROCESSING", "正在保存题目...", 80);
                // TODO: 调用 quiz-core 服务保存题目
                saveQuestionsToDatabase(questions, request);

                // 5. 完成
                updateTaskStatus(taskId, "COMPLETED", "题目生成完成！", 100);
                saveTaskResult(taskId, questions);

            } catch (Exception e) {
                log.error("题目生成任务失败: taskId={}", taskId, e);
                updateTaskStatus(taskId, "FAILED", "生成失败: " + e.getMessage(), 0);
            }
        });

        return taskId;
    }

    /**
     * 更新任务状态
     */
    private void updateTaskStatus(String taskId, String status, String message, int progress) {
        String key = "quiz:generation:task:" + taskId;
        String value = String.format("{\"status\":\"%s\",\"message\":\"%s\",\"progress\":%d,\"timestamp\":%d}",
                status, message, progress, System.currentTimeMillis());

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
    private void saveQuestionsToDatabase(List<GeneratedQuestion> questions, GenerateQuestionRequest request) {
        // TODO: 实现Feign客户端调用quiz-core服务保存题目
        log.info("保存 {} 道题目到题库 {}", questions.size(), request.getBankId());

        // 这里暂时只记录日志，实际应该调用quiz-core的API
        questions.forEach(q -> {
            log.debug("题目: type={}, content={}", q.getQuestionType(), q.getContent());
        });
    }
}
