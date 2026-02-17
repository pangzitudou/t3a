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
  └─ 结果分析与点评

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
2. AI 服务解析文档 -> 分批调用 LLM -> 去重/补题 -> 回写 core 题库 -> 更新 Redis 任务状态
3. 前端轮询 `/api/ai/generation/status/{taskId}`

## 4. 鉴权与协议
- `quiz-core` 使用 Spring Security + JWT。
- `/auth/**` 放行，其余默认鉴权。
- 前端 `api.ts` 拦截器：非 `/auth/` 请求自动注入 token。
- 统一响应外壳 `Result<T>`，前端已统一解包 `data`。

## 5. 当前能力边界

已实现：
- 用户认证（JWT）
- 题库/题目 CRUD、题目去重（入库与抽题）
- 测验会话创建/提交/退出（退出不保留记录）
- 单题提交落库与客观题/主观题评分
- AI 文档解析、分批生成、任务状态查询、回写题库
- Dashboard 真实统计聚合

边界与注意：
- 主观题评分为“要点命中”规则，建议继续迭代更细粒度语义评分
- LLM 输出仍可能抖动，需依赖解析修复与重试策略

## 6. 配置要点
- `quiz-core`: `server.servlet.context-path=/quiz`
- 前端代理以 `vite.config.ts` 为准
- 网关 CORS 已关闭，主要由微服务自身处理

## 7. 文档同步约定
- 变更任一项（路由、端口、鉴权、代理、服务依赖）后，同步更新本文件和 `docs/AGENT-HANDBOOK.md`。
