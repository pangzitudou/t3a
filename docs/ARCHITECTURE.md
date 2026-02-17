# T3A 架构说明（源码对齐版）

## 1. 总体结构

```text
Browser (React + Vite, :5173)
  └─ Vite Proxy
      ├─ /api/quiz/* -> quiz-core (:8081, /quiz/*)
      └─ /api/ai/*   -> quiz-ai   (:8082, /*)

Gateway (:8080)
  ├─ /api/quiz/** -> quiz-core
  ├─ /api/ai/**   -> quiz-ai
  └─ /api/ws/**   -> quiz-communication

quiz-core (:8081)
  ├─ Auth / Bank / Question / Quiz / Dashboard
  ├─ MySQL (持久化)
  └─ Redis (会话缓存)

quiz-ai (:8082)
  ├─ 文档解析 + LLM 题目生成
  ├─ 任务状态 Redis 轮询
  └─ 分析接口（当前为占位实现）

quiz-communication (:8083)
  └─ STOMP + SockJS
```

## 2. 核心模块

`quiz-core`
- `AuthController`: 登录/注册/刷新 token
- `QuestionBankController`: 题库 CRUD + public 列表
- `QuestionController`: 题目 CRUD + 随机抽题
- `QuizController`: 开始/提交/会话查询/结果
- `DashboardController`: 统计（当前占位数据）

`quiz-ai`
- `AIGenerationController`: `/generation/generate`、`/generation/status/{taskId}`
- `QuestionGenerationTaskService`: 异步任务、状态落 Redis
- `DocumentParserService`: `pdf/txt/doc/docx` 文本提取
- `AnalysisController`: `/ai/analysis/*`（当前占位数据）

`quiz-communication`
- WebSocket 端点: `/ws-quiz`
- 应用前缀: `/app`
- 订阅前缀: `/topic`

## 3. 关键链路

登录链路：
1. 前端请求 `/api/quiz/auth/login`
2. 代理转发到 `http://localhost:8081/quiz/auth/login`
3. 返回 `Result<LoginResponse>`，前端取 `data`
4. token 放入 `Authorization: Bearer ...`

AI 生成链路：
1. 前端上传 `multipart/form-data` 到 `/api/ai/generation/generate`
2. AI 服务解析文档 -> 调 LLM -> 更新 Redis 任务状态
3. 前端轮询 `/api/ai/generation/status/{taskId}`

## 4. 鉴权与协议
- `quiz-core` 使用 Spring Security + JWT。
- `/auth/**` 放行，其余默认鉴权。
- 前端 `api.ts` 拦截器：非 `/auth/` 请求自动注入 token。
- 统一响应外壳 `Result<T>`，前端已统一解包 `data`。

## 5. 当前能力边界

已实现：
- 用户认证（JWT）
- 题库/题目基础 CRUD
- 测验会话创建与提交
- AI 文档解析与题目生成任务状态查询

未闭环/占位：
- AI 题目未回写 `quiz-core`（TODO）
- Dashboard 统计占位
- AI 分析占位
- 单题提交接口未完成实质计分与落库

## 6. 配置要点
- `quiz-core`: `server.servlet.context-path=/quiz`
- 前端代理以 `vite.config.ts` 为准
- 网关 CORS 已关闭，主要由微服务自身处理

## 7. 文档同步约定
- 变更任一项（路由、端口、鉴权、代理、服务依赖）后，同步更新本文件和 `docs/AGENT-HANDBOOK.md`。
