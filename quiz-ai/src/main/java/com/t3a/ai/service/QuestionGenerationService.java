package com.t3a.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.t3a.ai.domain.dto.GeneratedQuestion;
import com.t3a.ai.domain.dto.GenerateQuestionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

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
        int requestedCount = request.getCount() == null ? 10 : request.getCount();
        log.info("开始生成题目: count={}, difficulty={}", requestedCount, request.getDifficulty());

        Map<String, Integer> requestedTypeCounts = parseTypeCountDistribution(request.getTypeDistribution());
        List<GeneratedQuestion> allQuestions;
        if (!requestedTypeCounts.isEmpty() && requestedTypeCounts.values().stream().mapToInt(Integer::intValue).sum() > 0) {
            allQuestions = generateByExactTypeCounts(textContent, request, requestedTypeCounts);
        } else {
            allQuestions = generateMixedQuestions(textContent, request, requestedCount);
        }

        allQuestions = ensureEnoughUniqueQuestions(textContent, request, allQuestions, requestedCount);
        log.info("成功生成 {} 道题目（请求 {}）", allQuestions.size(), requestedCount);
        return allQuestions;
    }

    private List<GeneratedQuestion> generateMixedQuestions(String textContent, GenerateQuestionRequest request, int requestedCount) {
        List<GeneratedQuestion> allQuestions = new ArrayList<>();
        int batchSize = 10;
        int totalBatches = (int) Math.ceil(requestedCount / (double) batchSize);

        for (int batch = 0; batch < totalBatches; batch++) {
            int currentBatchCount = Math.min(batchSize, requestedCount - allQuestions.size());
            if (currentBatchCount <= 0) {
                break;
            }
            allQuestions.addAll(generateSingleBatch(textContent, request, currentBatchCount, batch + 1, totalBatches, null));
        }
        return allQuestions;
    }

    private List<GeneratedQuestion> generateByExactTypeCounts(
            String textContent,
            GenerateQuestionRequest request,
            Map<String, Integer> typeCounts
    ) {
        List<GeneratedQuestion> allQuestions = new ArrayList<>();
        int totalBatches = typeCounts.values().stream()
                .mapToInt(v -> (int) Math.ceil(v / 10.0))
                .sum();
        int currentBatch = 0;

        for (Map.Entry<String, Integer> entry : typeCounts.entrySet()) {
            String type = entry.getKey();
            int required = Math.max(0, entry.getValue());
            while (required > 0) {
                int batchCount = Math.min(10, required);
                currentBatch++;
                List<GeneratedQuestion> batch = generateSingleBatch(
                        textContent,
                        request,
                        batchCount,
                        currentBatch,
                        totalBatches,
                        type
                );
                for (GeneratedQuestion q : batch) {
                    // 强制修正题型，保证和用户配置一致
                    q.setQuestionType(type);
                }
                allQuestions.addAll(batch);
                required -= batchCount;
            }
        }
        return allQuestions;
    }

    private List<GeneratedQuestion> generateSingleBatch(
            String textContent,
            GenerateQuestionRequest request,
            int batchCount,
            int batchIndex,
            int totalBatches,
            String forceType
    ) {
        String prompt = buildPrompt(textContent, request, batchCount, batchIndex, totalBatches, forceType);
        log.debug("第{}批生成Prompt长度: {}", batchIndex, prompt.length());

        Exception lastError = null;
        List<GeneratedQuestion> batchQuestions = new ArrayList<>();
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                String response = deepSeekModel.call(prompt);
                batchQuestions = parseAIResponse(response);
                if (forceType != null) {
                    batchQuestions = batchQuestions.stream()
                            .peek(q -> q.setQuestionType(forceType))
                            .collect(Collectors.toList());
                }
                if (batchQuestions.size() > batchCount) {
                    batchQuestions = batchQuestions.subList(0, batchCount);
                }
                if (!batchQuestions.isEmpty()) {
                    return batchQuestions;
                }
                lastError = new IllegalStateException("AI未返回有效题目");
            } catch (Exception e) {
                lastError = e;
                log.warn("第{}批第{}次生成失败: {}", batchIndex, attempt, e.getMessage());
            }
        }
        throw new RuntimeException("第" + batchIndex + "批题目生成失败: " +
                (lastError == null ? "未知错误" : lastError.getMessage()), lastError);
    }

    private List<GeneratedQuestion> ensureEnoughUniqueQuestions(
            String textContent,
            GenerateQuestionRequest request,
            List<GeneratedQuestion> allQuestions,
            int requestedCount
    ) {
        List<GeneratedQuestion> deduped = deduplicateByContent(allQuestions);
        int retry = 0;
        while (deduped.size() < requestedCount && retry < 3) {
            int missing = requestedCount - deduped.size();
            retry++;
            List<GeneratedQuestion> supplement = generateSingleBatch(
                    textContent,
                    request,
                    Math.min(10, missing),
                    retry,
                    3,
                    null
            );
            deduped.addAll(supplement);
            deduped = deduplicateByContent(deduped);
        }

        if (deduped.size() > requestedCount) {
            return new ArrayList<>(deduped.subList(0, requestedCount));
        }
        return deduped;
    }

    /**
     * 构建 Prompt
     */
    private String buildPrompt(String textContent, GenerateQuestionRequest request, int batchCount, int batchIndex, int totalBatches, String forceType) {
        // 限制文本长度，避免超出Token限制
        String limitedContent = limitTextLength(textContent, 8000);
        String typeRequirement = forceType == null
                ? getTypeDistributionDescription(request.getTypeDistribution())
                : ("本批仅生成 " + mapTypeName(forceType) + "，数量严格为 " + batchCount + " 道");

        return String.format("""
                你是一位专业的教育专家，擅长根据学习材料生成高质量的测验题目。

                请根据以下学习材料生成 %d 道测验题目（第 %d/%d 批）：

                【学习材料】
                %s

                【生成要求】
                1. 难度级别: %s
                2. 题型分布: %s
                3. 知识领域: %s

                【题型说明】
                - SINGLE_CHOICE: 单选题（4个选项，只有1个正确答案）
                - MULTIPLE_CHOICE: 多选题（4-5个选项，有2-3个正确答案）
                - SHORT_ANSWER: 简答题（需要文字描述回答，必须提供可评判的参考答案要点）

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
                1.1 不允许生成重复题目（题干语义不得重复）
                2. 选项要有迷惑性，但正确答案必须明确
                3. 每道题都要提供详细的解析
                3.1 主观题必须填写 correctAnswer，写成“要点1；要点2；要点3”
                4. 标注相关知识点标签，方便后续分析
                5. 只输出JSON数组，不要输出其他任何文字
                """,
                batchCount,
                batchIndex,
                totalBatches,
                limitedContent,
                getDifficultyDescription(request.getDifficulty()),
                typeRequirement,
                request.getCategory() != null ? request.getCategory() : "通用"
        );
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

            int sum = distribution.values().stream()
                    .map(v -> {
                        try {
                            return Integer.parseInt(String.valueOf(v));
                        } catch (Exception e) {
                            return 0;
                        }
                    })
                    .reduce(0, Integer::sum);
            boolean percentageMode = sum == 100;

            StringBuilder desc = new StringBuilder();
            distribution.forEach((type, percentage) -> {
                String typeName = switch (type) {
                    case "SINGLE_CHOICE" -> "单选题";
                    case "MULTIPLE_CHOICE" -> "多选题";
                    case "SHORT_ANSWER" -> "简答题";
                    case "CODE" -> "编程题";
                    default -> type;
                };
                int value;
                try {
                    value = Integer.parseInt(String.valueOf(percentage));
                } catch (Exception e) {
                    value = -1;
                }
                if (percentageMode && value >= 0) {
                    desc.append(typeName).append(value).append("%，");
                } else if (!percentageMode && value >= 0) {
                    desc.append(typeName).append(value).append("题，");
                } else {
                    desc.append(typeName).append(percentage).append("，");
                }
            });

            return desc.substring(0, desc.length() - 1);
        } catch (Exception e) {
            return "单选题40%，多选题30%，简答题30%";
        }
    }

    private Map<String, Integer> parseTypeCountDistribution(String typeDistribution) {
        if (typeDistribution == null || typeDistribution.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(typeDistribution, new TypeReference<>() {});
            Map<String, Integer> result = new LinkedHashMap<>();
            for (String key : List.of("SINGLE_CHOICE", "MULTIPLE_CHOICE", "SHORT_ANSWER")) {
                Object value = raw.get(key);
                if (value == null) {
                    continue;
                }
                int count;
                try {
                    count = Integer.parseInt(String.valueOf(value));
                } catch (Exception e) {
                    count = 0;
                }
                if (count > 0) {
                    result.put(key, count);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("解析题型分布失败: {}", e.getMessage());
            return Map.of();
        }
    }

    /**
     * 解析 AI 响应
     */
    private List<GeneratedQuestion> parseAIResponse(String response) throws Exception {
        String cleanedResponse = sanitizeToJsonArrayText(response);
        JsonNode root;
        try {
            root = objectMapper.readTree(cleanedResponse);
        } catch (Exception firstParseException) {
            log.warn("AI响应不是合法JSON，尝试自动修复后重试解析");
            String repaired = repairJsonWithAI(cleanedResponse);
            String repairedCleaned = sanitizeToJsonArrayText(repaired);
            root = objectMapper.readTree(repairedCleaned);
        }
        if (!root.isArray()) {
            throw new IllegalArgumentException("AI返回格式错误，必须是JSON数组");
        }

        List<GeneratedQuestion> questions = new java.util.ArrayList<>();
        for (JsonNode questionNode : root) {
            ObjectNode normalizedNode = questionNode.deepCopy();
            JsonNode correctAnswerNode = normalizedNode.get("correctAnswer");
            if (correctAnswerNode != null && correctAnswerNode.isArray()) {
                String normalizedAnswer = java.util.stream.StreamSupport.stream(correctAnswerNode.spliterator(), false)
                        .map(JsonNode::asText)
                        .collect(Collectors.joining(","));
                normalizedNode.put("correctAnswer", normalizedAnswer);
            }
            questions.add(objectMapper.treeToValue(normalizedNode, GeneratedQuestion.class));
        }
        return questions;
    }

    private String sanitizeToJsonArrayText(String response) {
        String cleaned = response == null ? "" : response.trim();
        cleaned = cleaned.replace("```json", "").replace("```", "").trim();
        int first = cleaned.indexOf('[');
        int last = cleaned.lastIndexOf(']');
        if (first >= 0 && last > first) {
            cleaned = cleaned.substring(first, last + 1);
        }
        return cleaned;
    }

    private String repairJsonWithAI(String rawContent) {
        String repairPrompt = """
                你是JSON修复助手。请把下面内容修复成合法的JSON数组。
                要求：
                1. 仅输出JSON数组，不要输出解释。
                2. 保持原始题目语义不变。
                3. 如果字段缺失，补齐为合理默认值。
                4. correctAnswer 如果是数组，请转为逗号分隔字符串。

                待修复内容：
                """ + rawContent;
        return deepSeekModel.call(repairPrompt);
    }

    private List<GeneratedQuestion> deduplicateByContent(List<GeneratedQuestion> questions) {
        if (questions == null || questions.isEmpty()) {
            return questions;
        }
        Map<String, GeneratedQuestion> deduped = new LinkedHashMap<>();
        for (GeneratedQuestion q : questions) {
            if (q == null) {
                continue;
            }
            String key = q.getContent() == null
                    ? ""
                    : q.getContent().trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
            if (!deduped.containsKey(key)) {
                deduped.put(key, q);
            }
        }
        return new ArrayList<>(deduped.values());
    }

    private String mapTypeName(String type) {
        return switch (type) {
            case "SINGLE_CHOICE" -> "单选题";
            case "MULTIPLE_CHOICE" -> "多选题";
            case "SHORT_ANSWER" -> "简答题";
            case "CODE" -> "编程题";
            default -> type;
        };
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
