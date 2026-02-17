-- H2 Database Schema for Testing

-- Users table
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL,
    nickname VARCHAR(50),
    role VARCHAR(20) DEFAULT 'USER',
    status INT DEFAULT 1,
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Question Banks table
CREATE TABLE IF NOT EXISTS t_question_bank (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    creator_id BIGINT,
    is_public BOOLEAN DEFAULT FALSE,
    ai_generated BOOLEAN DEFAULT FALSE,
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Questions table
CREATE TABLE IF NOT EXISTS t_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bank_id BIGINT NOT NULL,
    question_type VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    options TEXT,
    correct_answer TEXT,
    explanation TEXT,
    difficulty VARCHAR(20),
    tags VARCHAR(255),
    score INT DEFAULT 10,
    ai_generated BOOLEAN DEFAULT FALSE,
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bank_id) REFERENCES t_question_bank(id)
);

-- Quiz Sessions table
CREATE TABLE IF NOT EXISTS t_quiz_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    bank_id BIGINT NOT NULL,
    session_key VARCHAR(100) NOT NULL UNIQUE,
    total_questions INT NOT NULL,
    answered_count INT DEFAULT 0,
    total_score INT,
    user_score DECIMAL(10, 2),
    status VARCHAR(20) DEFAULT 'IN_PROGRESS',
    start_time TIMESTAMP,
    submit_time TIMESTAMP,
    time_spent INT,
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES t_user(id),
    FOREIGN KEY (bank_id) REFERENCES t_question_bank(id)
);

-- Indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_question_bank_creator ON t_question_bank(creator_id);
CREATE INDEX IF NOT EXISTS idx_question_bank_category ON t_question_bank(category);
CREATE INDEX IF NOT EXISTS idx_question_bank_id ON t_question_bank(id);
CREATE INDEX IF NOT EXISTS idx_question_bank_id ON t_question(bank_id);
CREATE INDEX IF NOT EXISTS idx_quiz_session_user ON t_quiz_session(user_id);
CREATE INDEX IF NOT EXISTS idx_quiz_session_key ON t_quiz_session(session_key);
