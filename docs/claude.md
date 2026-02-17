# Project Overview
TestAgainAndAgain(T3A): An AI-native distributed online quiz platform. It supports AI-generated question banks from user materials, automated scoring for both objective and subjective questions, and knowledge gap analysis via LLMs.

---

## Tech Stack

### Back End (Spring Cloud Alibaba & AI)
- **Framework**: Spring Boot 3.2+ & Spring Cloud Alibaba 2023.0.x
- **AI Core**: Spring AI Alibaba (Integrating DeepSeek-V3 / Qwen-Max)
- **Runtime**: JDK 17 (Virtual Threads enabled for WebSocket/AI task processing)
- **Database**: MySQL 8.0+ — Persistent storage for questions, users, and results
- **Cache**: Redis 7.0+ — Session management and real-time score calculation
- **Messaging**: RocketMQ 5.x — Decoupling AI scoring and data analysis tasks
- **Communication**: WebSocket (STOMP) — Real-time progress and AI feedback delivery

### Front End (Modern Vibe Stack)
- **Runtime**: Bun 1.1+ (Package & Build tool)
- **Framework**: React 19 + Vite 6
- **Tooling**: Biome (Linting & Formatting)
- **Key Libraries**: 
  - `@monaco-editor/react` (For code-based questions)
  - `framer-motion` (Fluid quiz transitions and AI loading effects)
  - `shadcn/ui` (Rapid UI prototyping)

---

## Microservices Architecture

- **`quiz-gateway`**: Gateway for routing, auth, and Sentinel-based AI rate limiting.
- **`quiz-core`**: 
    - Question bank management (Default & Custom).
    - Randomization logic and session state tracking.
- **`quiz-ai`**: 
    - **Generation**: Parse text files to generate quizzes using RAG.
    - **Evaluation**: AI-powered scoring for subjective questions.
    - **Analyst**: Generating student knowledge graphs and improvement suggestions.
- **`quiz-communication`**: 
    - Managing real-time WebSocket sessions for live-score updates.

---

## Project Goals
- **AI Integration**: Implement full-lifecycle AI (Question Gen -> Scoring -> Analysis).
- **Asynchronous Flow**: Use RocketMQ to handle long-running AI tasks without blocking users.
- **Scalability**: Build a system that handles thousands of concurrent quiz-takers.
- **User Experience**: Ensure sub-second latency for UI updates during quizzes.

---

## Development Notes
- **Prerequisites**: JDK 17, Bun 1.1+, Nacos 2.3+, Redis 7, RocketMQ 5.
- **Environment**: Configure `AI_DASHSCOPE_API_KEY` or `DEEPSEEK_API_KEY` in Nacos.
- **Optimization**: All services must use `spring.threads.virtual.enabled=true`.