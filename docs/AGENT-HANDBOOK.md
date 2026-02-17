# T3A 源码速查手册（给维护者/Agent）

目标：基于当前代码，快速完成启动、联调、排障和二次开发。

## 1. 项目结构

```text
t3a/
├── quiz-core/            # 业务核心：认证、题库、题目、测验、仪表板
├── quiz-ai/              # AI 题目生成、分析
├── quiz-gateway/         # Spring Cloud Gateway 路由层
├── quiz-communication/   # STOMP WebSocket
├── quiz-common/          # 通用 Result/常量
├── quiz-frontend/        # React + Vite 前端
└── docs/                 # 运行脚本和文档
```

## 2. 启动与依赖

## 2.1 必需环境
- JDK 17+
- Maven 3.8+
- Node.js 18+
- Docker（建议，用于 MySQL/Redis/Nacos/RocketMQ）

## 2.2 建议启动顺序
```bash
# 项目根目录
docker compose -f docs/docker-compose.yml up -d
mvn clean install -DskipTests

# 4 个终端分别启动
cd quiz-gateway && mvn spring-boot:run
cd quiz-core && mvn spring-boot:run
cd quiz-ai && mvn spring-boot:run
cd quiz-communication && mvn spring-boot:run

# 前端
cd quiz-frontend
npm install
npm run dev
```

## 2.3 已知脚本坑
- `docs/t3a-manager.sh` 内 `PROJECT_ROOT` 解析为 `docs/` 目录，直接用于启动微服务会找不到 `quiz-*` 模块目录。
- 因此目前更稳妥的是按 2.2 手动启动，或先修该脚本再使用。

## 3. 真实请求链路

前端 `quiz-frontend/src/services/api.ts` 的 `baseURL` 是 `/api`。

Vite 代理（`quiz-frontend/vite.config.ts`）：
- `/api/quiz/*` -> `http://localhost:8081/quiz/*`
- `/api/ai/*` -> `http://localhost:8082/*`

说明：前端本地开发通常不经 `gateway:8080`，而是直连 `core/ai`。

## 4. 接口基线（按 Controller）

## 4.1 quiz-core (`8081`, context-path=`/quiz`)
- 认证（公开）:
  - `POST /quiz/auth/login`
  - `POST /quiz/auth/register`
  - `POST /quiz/auth/refresh`
- 题库:
  - `POST /quiz/bank/create`
  - `GET /quiz/bank/{id}`
  - `GET /quiz/bank/list`
  - `GET /quiz/bank/public`
  - `PUT /quiz/bank/update`
  - `DELETE /quiz/bank/{id}`
- 题目:
  - `GET /quiz/question/list?bankId=...`
  - `GET /quiz/question/random?bankId=...&count=...`
  - `POST /quiz/question/create`
  - `POST /quiz/question/batch`
  - `GET /quiz/question/{id}`
  - `PUT /quiz/question/update`
  - `DELETE /quiz/question/{id}`
- 测验:
  - `POST /quiz/start`
  - `POST /quiz/submit`
  - `GET /quiz/session/{sessionKey}`
  - `GET /quiz/session/{sessionKey}/current`
  - `POST /quiz/session/{sessionKey}/answer`
  - `GET /quiz/history/{userId}`
  - `GET /quiz/result/{sessionKey}`
- 仪表板:
  - `GET /quiz/dashboard`

## 4.2 quiz-ai (`8082`)
- 生成:
  - `POST /generation/generate`（`multipart/form-data`，字段 `file` + `request` JSON 字符串）
  - `GET /generation/status/{taskId}`
  - `GET /generation/chat?message=...`
- 分析:
  - `GET /ai/analysis/{sessionKey}`
  - `POST /ai/analysis/{sessionKey}/regenerate`

## 5. 鉴权与返回格式

## 5.1 鉴权
- `quiz-core` 使用 Spring Security + JWT。
- `/auth/**` 放行，其余接口默认需要 `Authorization: Bearer <token>`。
- 前端拦截器逻辑：`url` 不含 `/auth/` 才自动加 token。

## 5.2 返回格式
后端统一 `Result<T>`，前端在 `api.ts` 里会将 `response.data` 解包为 `data` 字段。
因此业务层拿到的是解包后的 payload，不是完整 `{code,message,data}`。

## 6. 功能完成度（重要）

## 6.1 已落地
- 注册/登录/JWT 刷新
- 题库和题目的基础 CRUD
- 创建测验会话、提交会话、结果查询
- AI 文档解析（PDF/TXT/DOC/DOCX）+ 调用 LLM 生成题目
- 生成任务状态写入 Redis（轮询查询）

## 6.2 尚未闭环（代码里有 TODO）
- AI 生成题目尚未真正回写 `quiz-core` 数据库（目前仅日志输出）
- Dashboard 统计是占位数据
- AI 分析接口返回占位数据
- 提交单题答案接口当前只做会话存在性检查，未完成计分/落库

## 7. WebSocket 约定
- 握手端点: `/ws-quiz`（SockJS）
- 订阅前缀: `/topic`
- 发送前缀: `/app`
- 示例:
  - 客户端发送: `/app/quiz/progress`
  - 广播订阅: `/topic/quiz/progress`

## 8. 数据库与初始化

初始化脚本：`quiz-core/src/main/resources/sql/schema.sql`
- 主要表：`t_user`, `t_question_bank`, `t_question`, `t_quiz_session`, `t_user_answer`, `t_ai_analysis`
- 包含基础测试数据（如 `admin`, `testuser`）

## 9. 日常排障清单
- 端口冲突: `lsof -i :8081` 等确认占用
- 前端 401 循环跳登录: 检查 token 是否存在、是否过期
- AI 生成报错:
  - 检查 `DEEPSEEK_API_KEY` 或 `quiz-ai` 配置
  - 检查 Redis 是否可用（任务状态依赖）
- `generation/status` 返回 `NOT_FOUND`: 常见于 taskId 过期（Redis TTL 1 小时）或任务未提交成功

## 10. 推荐阅读顺序
1. `quiz-frontend/src/services/*.ts`（先理解真实调用）
2. `quiz-core/src/main/java/com/t3a/core/controller/*.java`
3. `quiz-ai/src/main/java/com/t3a/ai/controller/*.java`
4. `quiz-core/src/main/java/com/t3a/core/service/*.java`
5. `quiz-ai/src/main/java/com/t3a/ai/service/*.java`

## 11. 文档同步检查清单（每次改代码后）
- 是否变更了接口路径、请求参数、响应结构
- 是否变更了端口、context-path、代理规则
- 是否变更了鉴权策略、公开接口、token 处理
- 是否新增/完成了 TODO 功能，完成度描述是否更新
- 是否同步更新以下文件：
  - `docs/README.md`
  - `docs/STARTUP-GUIDE.md`
  - `docs/ARCHITECTURE.md`
  - `docs/PROJECT-SUMMARY.md`
  - `docs/AGENT-HANDBOOK.md`
