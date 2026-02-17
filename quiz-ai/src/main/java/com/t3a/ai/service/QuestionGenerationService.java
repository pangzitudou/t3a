package com.t3a.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.t3a.ai.domain.dto.GeneratedQuestion;
import com.t3a.ai.domain.dto.GenerateQuestionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 题目生成服务
 */
@Slf4j
@Service
public class QuestionGenerationService {

    private final ChatModel deepSeekModel;

    private final ObjectMapper objectMapper;

    public QuestionGenerationService(@Qualifier("deepSeekChatModel") ChatModel deepSeekModel, ObjectMapper objectMapper) {
        this.deepSeekModel = deepSeekModel;
        this.objectMapper = objectMapper;
    }

    /**
     * 根据文本内容生成题目
     */
    public List<GeneratedQuestion> generateQuestions(String textContent, GenerateQuestionRequest request) {
        log.info("开始生成题目: count={}, difficulty={}", request.getCount(), request.getDifficulty());

        String prompt = buildPrompt(textContent, request);
        log.debug("生成的Prompt长度: {}", prompt.length());

        try {
            String response = deepSeekModel.call(prompt);
            log.info("AI响应长度: {}", response.length());

            // 解析 JSON 响应
            List<GeneratedQuestion> questions = parseAIResponse(response);
            log.info("成功生成 {} 道题目", questions.size());

            return questions;
        } catch (Exception e) {
            log.error("生成题目失败", e);
            throw new RuntimeException("AI题目生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建 Prompt
     */
    private String buildPrompt(String textContent, GenerateQuestionRequest request) {
        // 限制文本长度，避免超出Token限制
        String limitedContent = limitTextLength(textContent, 8000);

        String template = """
                你是一位专业的教育专家，擅长根据学习材料生成高质量的测验题目。

                请根据以下学习材料生成 {count} 道测验题目：

                【学习材料】
                {content}

                【生成要求】
                1. 难度级别: {difficulty}
                2. 题型分布: {typeDistribution}
                3. 知识领域: {category}

                【题型说明】
                - SINGLE_CHOICE: 单选题（4个选项，只有1个正确答案）
                - MULTIPLE_CHOICE: 多选题（4-5个选项，有2-3个正确答案）
                - SHORT_ANSWER: 简答题（需要文字描述回答）

                【输出格式】
                请严格按照以下JSON数组格式输出，不要添加任何其他内容：

                [
                  {
                    "questionType": "SINGLE_CHOICE",
                    "content": "题目内容",
                    "options": ["选项A", "选项B", "选项C", "选项D"],
                    "correctAnswer": "A",
                    "explanation": "答案解析",
                    "difficulty": "MEDIUM",
                    "tags": ["知识点1", "知识点2"],
                    "score": 10
                  }
                ]

                【重要提示】
                1. 题目要基于学习材料的核心知识点
                2. 选项要有迷惑性，但正确答案必须明确
                3. 每道题都要提供详细的解析
                4. 标注相关知识点标签，方便后续分析
                5. 只输出JSON数组，不要输出其他任何文字
                """;

        Map<String, Object> variables = new HashMap<>();
        variables.put("content", limitedContent);
        variables.put("count", request.getCount());
        variables.put("difficulty", getDifficultyDescription(request.getDifficulty()));
        variables.put("typeDistribution", getTypeDistributionDescription(request.getTypeDistribution()));
        variables.put("category", request.getCategory() != null ? request.getCategory() : "通用");

        PromptTemplate promptTemplate = new PromptTemplate(template);
        return promptTemplate.create(variables).getContents();
    }

    /**
     * 限制文本长度
     */
    private String limitTextLength(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "\n\n[注: 文本过长已截断]";
    }

    /**
     * 获取难度描述
     */
    private String getDifficultyDescription(String difficulty) {
        return switch (difficulty.toUpperCase()) {
            case "EASY" -> "简单（基础概念和定义）";
            case "MEDIUM" -> "中等（需要理解和应用）";
            case "HARD" -> "困难（需要综合分析和推理）";
            case "MIXED" -> "混合（包含简单、中等、困难各占1/3）";
            default -> "中等";
        };
    }

    /**
     * 获取题型分布描述
     */
    private String getTypeDistributionDescription(String typeDistribution) {
        if (typeDistribution == null || typeDistribution.isEmpty()) {
            return "单选题40%，多选题30%，简答题30%";
        }

        try {
            Map<String, Object> distribution = objectMapper.readValue(
                    typeDistribution,
                    new TypeReference<>() {
                    }
            );

            StringBuilder desc = new StringBuilder();
            distribution.forEach((type, percentage) -> {
                String typeName = switch (type) {
                    case "SINGLE_CHOICE" -> "单选题";
                    case "MULTIPLE_CHOICE" -> "多选题";
                    case "SHORT_ANSWER" -> "简答题";
                    case "CODE" -> "编程题";
                    default -> type;
                };
                desc.append(typeName).append(percentage).append("%，");
            });

            return desc.substring(0, desc.length() - 1);
        } catch (Exception e) {
            return "单选题40%，多选题30%，简答题30%";
        }
    }

    /**
     * 解析 AI 响应
     */
    private List<GeneratedQuestion> parseAIResponse(String response) throws Exception {
        // 去除可能的Markdown代码块标记
        String cleanedResponse = response.trim();
        if (cleanedResponse.startsWith("```json")) {
            cleanedResponse = cleanedResponse.substring(7);
        }
        if (cleanedResponse.startsWith("```")) {
            cleanedResponse = cleanedResponse.substring(3);
        }
        if (cleanedResponse.endsWith("```")) {
            cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
        }
        cleanedResponse = cleanedResponse.trim();

        // 解析JSON
        return objectMapper.readValue(
                cleanedResponse,
                new TypeReference<List<GeneratedQuestion>>() {}
        );
    }

    /**
     * 主观题智能评分
     */
    public String scoreSubjectiveAnswer(String question, String standardAnswer, String userAnswer) {
        String prompt = String.format("""
                你是一位专业的教师，请对学生的主观题答案进行评分和评价。

                【题目】
                %s

                【标准答案】
                %s

                【学生答案】
                %s

                【评分要求】
                1. 给出0-10的得分
                2. 指出答案的优点和不足
                3. 提供改进建议

                请以JSON格式输出：
                {
                  "score": 8,
                  "strengths": ["优点1", "优点2"],
                  "weaknesses": ["不足1", "不足2"],
                  "suggestions": "改进建议",
                  "feedback": "综合评价"
                }
                """, question, standardAnswer, userAnswer);

        return deepSeekModel.call(prompt);
    }
}
