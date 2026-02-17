-- T3A Quiz Platform Database Schema

USE t3a_quiz;

-- 题库表
CREATE TABLE IF NOT EXISTS t_question_bank (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '题库ID',
    name VARCHAR(100) NOT NULL COMMENT '题库名称',
    description VARCHAR(500) COMMENT '题库描述',
    category VARCHAR(50) COMMENT '分类',
    creator_id BIGINT COMMENT '创建者ID',
    is_public TINYINT(1) DEFAULT 0 COMMENT '是否公开',
    ai_generated TINYINT(1) DEFAULT 0 COMMENT '是否AI生成',
    deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_creator (creator_id),
    INDEX idx_category (category),
    INDEX idx_public (is_public)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库表';

-- 题目表
CREATE TABLE IF NOT EXISTS t_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '题目ID',
    bank_id BIGINT NOT NULL COMMENT '题库ID',
    question_type VARCHAR(20) NOT NULL COMMENT '题型: SINGLE_CHOICE/MULTIPLE_CHOICE/SHORT_ANSWER/CODE',
    content TEXT NOT NULL COMMENT '题目内容',
    options JSON COMMENT '选项(JSON格式)',
    correct_answer TEXT NOT NULL COMMENT '正确答案',
    explanation TEXT COMMENT '解析说明',
    difficulty VARCHAR(20) DEFAULT 'MEDIUM' COMMENT '难度: EASY/MEDIUM/HARD',
    tags VARCHAR(200) COMMENT '知识点标签(逗号分隔)',
    score INT DEFAULT 10 COMMENT '分值',
    ai_generated TINYINT(1) DEFAULT 0 COMMENT '是否AI生成',
    deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_bank (bank_id),
    INDEX idx_type (question_type),
    INDEX idx_difficulty (difficulty),
    FOREIGN KEY (bank_id) REFERENCES t_question_bank(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码(加密)',
    email VARCHAR(100) COMMENT '邮箱',
    nickname VARCHAR(50) COMMENT '昵称',
    avatar VARCHAR(200) COMMENT '头像URL',
    role VARCHAR(20) DEFAULT 'USER' COMMENT '角色: USER/ADMIN',
    status TINYINT(1) DEFAULT 1 COMMENT '状态: 0禁用 1启用',
    deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 测验会话表
CREATE TABLE IF NOT EXISTS t_quiz_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    bank_id BIGINT NOT NULL COMMENT '题库ID',
    session_key VARCHAR(100) UNIQUE NOT NULL COMMENT '会话标识',
    total_questions INT NOT NULL COMMENT '总题数',
    answered_count INT DEFAULT 0 COMMENT '已答题数',
    total_score INT COMMENT '总分',
    user_score DECIMAL(5,2) DEFAULT 0 COMMENT '用户得分',
    status VARCHAR(20) DEFAULT 'IN_PROGRESS' COMMENT '状态: IN_PROGRESS/COMPLETED/ABANDONED',
    start_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    submit_time DATETIME COMMENT '提交时间',
    time_spent INT COMMENT '耗时(秒)',
    deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user (user_id),
    INDEX idx_bank (bank_id),
    INDEX idx_session_key (session_key),
    INDEX idx_status (status),
    FOREIGN KEY (user_id) REFERENCES t_user(id),
    FOREIGN KEY (bank_id) REFERENCES t_question_bank(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测验会话表';

-- 用户答案表
CREATE TABLE IF NOT EXISTS t_user_answer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '答案ID',
    session_id BIGINT NOT NULL COMMENT '会话ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    user_answer TEXT COMMENT '用户答案',
    is_correct TINYINT(1) COMMENT '是否正确',
    score DECIMAL(5,2) DEFAULT 0 COMMENT '得分',
    ai_feedback TEXT COMMENT 'AI评分反馈(主观题)',
    answer_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '答题时间',
    deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_session (session_id),
    INDEX idx_question (question_id),
    FOREIGN KEY (session_id) REFERENCES t_quiz_session(id),
    FOREIGN KEY (question_id) REFERENCES t_question(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户答案表';

-- AI分析结果表
CREATE TABLE IF NOT EXISTS t_ai_analysis (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分析ID',
    session_id BIGINT NOT NULL COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    strengths JSON COMMENT '掌握好的知识点(JSON)',
    weaknesses JSON COMMENT '薄弱知识点(JSON)',
    knowledge_graph JSON COMMENT '知识图谱数据(JSON)',
    suggestions TEXT COMMENT 'AI学习建议',
    analysis_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '分析时间',
    deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_session (session_id),
    INDEX idx_user (user_id),
    FOREIGN KEY (session_id) REFERENCES t_quiz_session(id),
    FOREIGN KEY (user_id) REFERENCES t_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI分析结果表';

-- 插入测试数据
INSERT INTO t_user (username, password, email, nickname, role) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin@t3a.com', '管理员', 'ADMIN'),
('testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'user@t3a.com', '测试用户', 'USER');

INSERT INTO t_question_bank (name, description, category, creator_id, is_public, ai_generated) VALUES
('Spring Boot 基础', 'Spring Boot 核心概念和基础知识', 'Backend', 1, 1, 0),
('Spring Cloud Alibaba 微服务', '微服务架构与Spring Cloud Alibaba组件', 'Backend', 1, 1, 0);
