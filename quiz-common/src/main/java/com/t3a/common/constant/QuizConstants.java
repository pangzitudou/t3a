package com.t3a.common.constant;

/**
 * 业务常量
 */
public class QuizConstants {

    // Redis Key 前缀
    public static final String REDIS_KEY_QUIZ_SESSION = "quiz:session:";
    public static final String REDIS_KEY_USER_TOKEN = "user:token:";
    public static final String REDIS_KEY_QUIZ_SCORE = "quiz:score:";

    // RocketMQ Topic
    public static final String TOPIC_AI_GENERATION = "quiz-ai-generation";
    public static final String TOPIC_AI_SCORING = "quiz-ai-scoring";
    public static final String TOPIC_AI_ANALYSIS = "quiz-ai-analysis";

    // WebSocket 目的地
    public static final String WS_DESTINATION_PROGRESS = "/topic/quiz/progress";
    public static final String WS_DESTINATION_SCORE = "/topic/quiz/score";

    // 题目类型
    public static final String QUESTION_TYPE_SINGLE = "SINGLE_CHOICE";
    public static final String QUESTION_TYPE_MULTIPLE = "MULTIPLE_CHOICE";
    public static final String QUESTION_TYPE_SHORT_ANSWER = "SHORT_ANSWER";
    public static final String QUESTION_TYPE_CODE = "CODE";

    // 难度级别
    public static final String DIFFICULTY_EASY = "EASY";
    public static final String DIFFICULTY_MEDIUM = "MEDIUM";
    public static final String DIFFICULTY_HARD = "HARD";
}
