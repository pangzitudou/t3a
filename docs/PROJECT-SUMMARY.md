# TestAgainAndAgain (T3A) - 项目总结

AI驱动的分布式在线答题平台，支持AI生成题库、自动评分、知识差距分析。

---

## 一、项目架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Browser (Client)                            │
│  React 18 + Vite 5 + TypeScript running on http://localhost:5173   │
└────────────────────────┬────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     quiz-gateway (Port: 8080)                       │
│  Spring Cloud Gateway + Sentinel 限流 + JWT认证                     │
└────────────────────────┬────────────────────────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        ▼                ▼                ▼
┌───────────────┐ ┌───────────────┐ ┌───────────────┐
│  quiz-core    │ │   quiz-ai     │ │quiz-communication│
│  (Port: 8081) │ │  (Port: 8082) │ │   (Port: 8083)   │
│               │ │               │ │                  │
│ 用户认证      │ │ AI题目生成    │ │ WebSocket管理    │
│ 题库管理      │ │ AI评分        │ │ 实时进度推送     │
│ 答题会话      │ │ 知识分析      │ │                  │
└───────┬───────┘ └───────┬───────┘ └────────┬─────────┘
        │                 │                  │
        └────────────────┬┴──────────────────┘
                         │
        ┌────────────────┼────────────────┐
        ▼                ▼                ▼
   ┌─────────┐     ┌─────────┐     ┌───────────┐
   │ MySQL   │     │ Redis   │     │ RocketMQ  │
   │  8.0+   │     │  7.0+   │     │   5.x     │
   └─────────┘     └─────────┘     └───────────┘
```

---

## 二、微服务详解

### 2.1 quiz-gateway (API网关 - 8080)

**职责**: 统一入口、路由分发、认证鉴权、限流熔断

| 路由路径 | 目标服务 | 说明 |
|---------|---------|------|
| `/api/quiz/**` | quiz-core:8081 | 题库、答题、用户 |
| `/api/ai/**` | quiz-ai:8082 | AI生成、分析 |
| `/api/ws/**` | quiz-communication:8083 | WebSocket |

### 2.2 quiz-core (核心服务 - 8081)

**职责**: 用户管理、题库管理、答题会话

**核心模块**:
- `AuthController` - 用户登录/注册
- `QuestionBankController` - 题库CRUD
- `QuestionController` - 题目管理
- `QuizController` - 答题会话
- `DashboardController` - 仪表板数据

### 2.3 quiz-ai (AI服务 - 8082)

**职责**: AI题目生成、智能评分、知识分析

**核心模块**:
- `AIGenerationController` - 题目生成API
- `QuestionGenerationService` - Spring AI + RAG 生成逻辑
- `DocumentParserService` - 文档解析
- `AnalysisController` - 知识分析API

### 2.4 quiz-communication (通信服务 - 8083)

**职责**: WebSocket实时通信

**技术**: STOMP over SockJS

---

## 三、前端结构

### 3.1 技术栈

| 类别 | 技术 | 版本 |
|-----|------|-----|
| 框架 | React | 18.3 |
| 构建 | Vite | 5.4 |
| 语言 | TypeScript | 5.7 |
| 路由 | react-router-dom | 7.1 |
| 状态 | Zustand | 5.0 |
| 样式 | Tailwind CSS | 3.4 |
| 动画 | Framer Motion | 11.18 |
| HTTP | Axios | 1.7 |
| 图表 | Recharts | 3.7 |

### 3.2 页面路由

| 路由 | 页面 | 权限 | 说明 |
|-----|------|-----|------|
| `/` | HomePage | 公开 | 首页/落地页 |
| `/login` | LoginPage | 公开 | 用户登录 |
| `/register` | RegisterPage | 公开 | 用户注册 |
| `/dashboard` | DashboardPage | 受保护 | 用户仪表板 |
| `/banks` | BanksPage | 受保护 | 题库列表 |
| `/generate` | GeneratePage | 受保护 | AI生成题库 |
| `/quiz/:sessionKey` | QuizPage | 受保护 | 答题页面 |
| `/result/:sessionKey` | ResultPage | 受保护 | 结果+分析 |
| `/debug` | DebugApiPage | 受保护 | API调试 |

### 3.3 服务层结构

```
services/
├── api.ts         # Axios实例、拦截器、公共API
├── auth.ts        # 认证API
├── quiz.ts        # 题库/答题API
├── ai.ts          # AI服务API
├── websocket.ts   # WebSocket封装
└── utils.ts       # 工具函数
```

### 3.4 状态管理

| Store | 状态 | 持久化 |
|-------|------|--------|
| `authStore` | user, token, isAuthenticated | localStorage |
| `quizStore` | session, questions, answers, timeRemaining | 内存 |

---

## 四、核心实体

### 4.1 User (用户)

```java
Long id;
String username;
String password;  // 加密
String email;
String nickname;
String avatar;
LocalDateTime createTime;
```

### 4.2 QuestionBank (题库)

```java
Long id;
String name;
String description;
String category;
Long creatorId;
Boolean isPublic;
Boolean aiGenerated;
LocalDateTime createTime;
```

### 4.3 Question (题目)

```java
Long id;
Long bankId;
String questionType;  // SINGLE_CHOICE, MULTIPLE_CHOICE, SHORT_ANSWER, CODE
String content;
String options;       // JSON
String correctAnswer;
String explanation;
String difficulty;    // EASY, MEDIUM, HARD
String tags;
Integer score;
Boolean aiGenerated;
```

### 4.4 QuizSession (答题会话)

```java
Long id;
Long userId;
Long bankId;
String sessionKey;
Integer totalQuestions;
Integer answeredCount;
Integer totalScore;
BigDecimal userScore;
String status;        // IN_PROGRESS, COMPLETED, ABANDONED
LocalDateTime startTime;
LocalDateTime submitTime;
Integer timeSpent;
```

---

## 五、API端点汇总

### 5.1 认证 (quiz-core:8081)

| Method | Endpoint | 说明 |
|--------|----------|------|
| POST | `/quiz/auth/login` | 用户登录 |
| POST | `/quiz/auth/register` | 用户注册 |

### 5.2 题库 (quiz-core:8081)

| Method | Endpoint | 说明 |
|--------|----------|------|
| GET | `/quiz/bank/list` | 题库列表 |
| POST | `/quiz/bank/create` | 创建题库 |
| GET | `/quiz/bank/{id}` | 题库详情 |
| PUT | `/quiz/bank/update` | 更新题库 |
| DELETE | `/quiz/bank/{id}` | 删除题库 |

### 5.3 题目 (quiz-core:8081)

| Method | Endpoint | 说明 |
|--------|----------|------|
| GET | `/quiz/question/list` | 题目列表 |
| GET | `/quiz/question/random` | 随机题目 |
| POST | `/quiz/question/create` | 创建题目 |
| POST | `/quiz/question/batch` | 批量创建 |

### 5.4 答题会话 (quiz-core:8081)

| Method | Endpoint | 说明 |
|--------|----------|------|
| POST | `/quiz/start` | 开始答题 |
| GET | `/quiz/session/{key}` | 会话详情 |
| POST | `/quiz/session/{key}/answer` | 提交答案 |
| POST | `/quiz/submit` | 提交试卷 |
| GET | `/quiz/result/{key}` | 答题结果 |
| GET | `/quiz/history/{uid}` | 答题历史 |

### 5.5 仪表板 (quiz-core:8081)

| Method | Endpoint | 说明 |
|--------|----------|------|
| GET | `/quiz/dashboard` | 统计数据 |

### 5.6 AI生成 (quiz-ai:8082)

| Method | Endpoint | 说明 |
|--------|----------|------|
| POST | `/generation/generate` | 生成题目 |
| GET | `/generation/status/{id}` | 生成进度 |

### 5.7 AI分析 (quiz-ai:8082)

| Method | Endpoint | 说明 |
|--------|----------|------|
| GET | `/ai/analysis/{key}` | 知识分析 |
| POST | `/ai/analysis/{key}/regenerate` | 重新分析 |

---

## 六、数据流

### 6.1 登录流程

```
用户输入 → LoginPage → POST /quiz/auth/login → JWT Token
    → authStore.setAuth() → localStorage持久化 → 跳转Dashboard
```

### 6.2 AI生成题目流程

```
上传文件 → GeneratePage → POST /generation/generate
    → quiz-ai解析文档 → Spring AI调用LLM → 生成题目
    → 轮询 /generation/status/{taskId} → 完成后跳转Banks
```

### 6.3 答题流程

```
选择题库 → POST /quiz/start → 获取sessionKey
    → QuizPage加载题目 → WebSocket连接
    → 用户答题 → POST /quiz/session/{key}/answer
    → 提交试卷 → POST /quiz/submit
    → 跳转ResultPage → 获取AI分析
```

---

## 七、功能状态

### 7.1 已完成 ✅

- [x] 用户注册/登录 (JWT)
- [x] 题库CRUD
- [x] 题目管理
- [x] AI生成题目
- [x] 答题会话
- [x] WebSocket实时同步
- [x] 计时器/倒计时
- [x] 代码题编辑器 (Monaco)
- [x] 结果展示
- [x] 仪表板统计

### 7.2 待完善 🚧

- [ ] AI知识分析完善 (后端API待实现)
- [ ] 主观题AI评分
- [ ] 代码题自动评测
- [ ] 答题历史完善
- [ ] 知识图谱可视化
- [ ] 用户头像上传
- [ ] 题库分享功能

---

## 八、开发指南

### 8.1 本地启动

```bash
# 1. 启动基础设施
cd docs && docker-compose up -d

# 2. 启动后端
./t3a-manager.sh start

# 3. 启动前端
cd quiz-frontend && npm run dev
```

### 8.2 访问地址

| 服务 | 地址 |
|-----|------|
| 前端 | http://localhost:5173 |
| Gateway | http://localhost:8080 |
| Core API文档 | http://localhost:8081/doc.html |
| AI API文档 | http://localhost:8082/doc.html |
| Nacos | http://localhost:8848/nacos |
| RocketMQ | http://localhost:8180 |

### 8.3 关键配置

**后端** (application.yml):
```yaml
spring:
  threads:
    virtual:
      enabled: true  # 启用虚拟线程
  cloud:
    nacos:
      discovery:
        namespace: test-again-and-again
```

**前端** (vite.config.ts):
```typescript
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:7777',  // Gateway
      changeOrigin: true,
    },
  },
}
```

---

## 九、文件结构

```
test-again-and-again/
├── docs/                          # 文档
│   ├── ARCHITECTURE.md            # API架构详解
│   ├── STARTUP-GUIDE.md           # 启动指南
│   ├── PROJECT-SUMMARY.md         # 本文档
│   └── docker-compose.yml         # 基础设施编排
├── quiz-common/                   # 共享模块
│   └── src/main/java/com/t3a/common/
│       ├── constant/              # 常量
│       └── domain/Result.java     # 统一响应
├── quiz-gateway/                  # API网关 (8080)
├── quiz-core/                     # 核心服务 (8081)
│   └── src/main/java/com/t3a/core/
│       ├── controller/            # REST控制器
│       ├── service/               # 业务逻辑
│       ├── mapper/                # MyBatis映射
│       ├── domain/                # 实体/DTO/VO
│       ├── config/                # 配置类
│       └── security/              # JWT安全
├── quiz-ai/                       # AI服务 (8082)
│   └── src/main/java/com/t3a/ai/
│       ├── controller/            # AI控制器
│       ├── service/               # AI服务
│       └── domain/dto/            # DTO
├── quiz-communication/            # 通信服务 (8083)
└── quiz-frontend/                 # 前端项目
    └── src/
        ├── pages/                 # 页面组件
        ├── components/            # UI组件
        ├── services/              # API服务
        ├── stores/                # 状态管理
        └── main.tsx               # 入口
```

---

**最后更新**: 2026-02-12  
**版本**: 1.0.0
