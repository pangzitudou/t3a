# T3A Agent 使用指南 (claude.md)

## 执行计划

**目标**: 将Agent使用指南创建为项目根目录的 `claude.md` 文件

**操作**:
1. 将本文档内容复制到 `/Users/zeni/projects/t3a/claude.md`
2. 该文件将作为Claude Agent了解T3A项目的快速参考指南

---

## 项目概述

**T3A (Test Again And Again)** 是一个AI驱动的在线测验平台，采用微服务架构。

**项目类型**: 全栈微服务应用 (Java Spring Boot + React TypeScript)

**主要功能**:
- 用户认证与授权 (JWT)
- 题库和题目管理 (CRUD)
- 测验会话管理 (创建、答题、提交、结果)
- AI题目生成 (文档解析 + LLM生成)
- 实时通信 (WebSocket - 默认关闭)
- 仪表板统计

---

## 技术栈

| 层级 | 技术 |
|------|------|
| **后端** | Java 17, Spring Boot 3.2.2, Spring Cloud Alibaba |
| **前端** | React 18, TypeScript, Vite 5.4, Tailwind CSS |
| **数据库** | MySQL 8, MyBatis Plus |
| **缓存** | Redis 7 |
| **中间件** | Nacos (服务发现), RocketMQ (消息队列, 可选) |
| **AI** | DeepSeek API (通过 Spring AI Alibaba) |
| **API文档** | Knife4j (OpenAPI 3) |
| **状态管理** | Zustand + React Query |
| **鉴权** | JWT + Spring Security |

---

## 项目结构

```
t3a/
├── quiz-core/            # 核心业务服务 (8081)
├── quiz-ai/              # AI生成服务 (8082)
├── quiz-gateway/         # API网关 (8080)
├── quiz-communication/   # WebSocket服务 (8083)
├── quiz-common/          # 公共模块 (Result, 常量)
├── quiz-frontend/        # React前端 (5173)
├── docker/               # Docker配置 (RocketMQ)
├── docs/                 # 项目文档
└── logs/                 # 日志文件
```

---

## 服务端口与代理规则

### 端口分配

| 服务 | 端口 | Context Path | 说明 |
|------|------|--------------|------|
| quiz-gateway | 8080 | - | API网关 (本地开发一般不经过) |
| quiz-core | 8081 | `/quiz` | 核心业务 |
| quiz-ai | 8082 | - | AI生成 |
| quiz-communication | 8083 | - | WebSocket |
| quiz-frontend | 5173 | - | 开发服务器 |

### Vite代理配置 (本地开发)

前端请求路径 → 后端实际路径:

| 前端路径 | 代理目标 |
|----------|----------|
| `/api/quiz/*` | `http://localhost:8081/quiz/*` |
| `/api/ai/*` | `http://localhost:8082/*` |

**重要**: 本地开发默认前端直连 core/ai，不经过 gateway。gateway 主要用于生产环境统一入口。

---

## 核心API路由

### quiz-core (8081, context-path=/quiz)

**认证 (公开)**:
- `POST /quiz/auth/login` - 用户登录
- `POST /quiz/auth/register` - 用户注册
- `POST /quiz/auth/refresh` - 刷新token

**题库**:
- `POST /quiz/bank/create` - 创建题库
- `GET /quiz/bank/{id}` - 获取题库详情
- `GET /quiz/bank/list` - 我的题库列表
- `GET /quiz/bank/public` - 公开题库
- `PUT /quiz/bank/update` - 更新题库
- `DELETE /quiz/bank/{id}` - 删除题库

**题目**:
- `GET /quiz/question/list?bankId=...` - 题目列表
- `GET /quiz/question/random?bankId=...&count=...` - 随机抽题
- `POST /quiz/question/create` - 创建题目
- `POST /quiz/question/batch` - 批量创建
- `GET /quiz/question/{id}` - 获取题目
- `PUT /quiz/question/update` - 更新题目
- `DELETE /quiz/question/{id}` - 删除题目

**测验**:
- `POST /quiz/start` - 开始测验
- `POST /quiz/submit` - 提交测验
- `GET /quiz/session/{sessionKey}` - 获取会话
- `GET /quiz/session/{sessionKey}/current` - 获取当前题目
- `POST /quiz/session/{sessionKey}/answer` - 提交单题答案
- `GET /quiz/history/{userId}` - 历史记录
- `GET /quiz/result/{sessionKey}` - 测验结果

**仪表板**:
- `GET /quiz/dashboard` - 统计数据

### quiz-ai (8082)

**生成**:
- `POST /generation/generate` - 触发文档解析和题目生成 (multipart/form-data)
- `GET /generation/status/{taskId}` - 查询生成任务状态
- `GET /generation/chat?message=...` - AI对话 (当前实现)

**分析**:
- `GET /ai/analysis/{sessionKey}` - 分析结果
- `POST /ai/analysis/{sessionKey}/regenerate` - 重新生成分析

---

## 鉴权机制

### 后端 (Spring Security + JWT)
- `/auth/**` 端点公开访问
- 其他端点需要 `Authorization: Bearer <token>` header
- Token 包含用户ID和角色信息

### 前端 (Axios拦截器)
- 请求路径包含 `/auth/` 时不加token
- 其他请求自动从localStorage获取token并添加到header
- 401响应时自动跳转登录页

### 响应格式
```typescript
// 后端返回: Result<T>
{
  code: 200,
  message: "success",
  data: T
}

// 前端自动解包，业务层直接使用 data
```

---

## 题目类型

| 类型 | code | 说明 |
|------|------|------|
| 单选题 | SINGLE_CHOICE | 唯一正确答案 |
| 多选题 | MULTIPLE_CHOICE | 多个正确答案 |
| 简答题 | SHORT_ANSWER | 主观题，要点评分 |
| 代码题 | CODE | 编程题，代码编辑器 |

---

## 启动与调试

### 环境要求
- **JDK 17** (JDK 25会导致Lombok编译错误)
- Maven 3.8+
- Node.js 18+
- Docker / Docker Compose (用于基础设施)

### 启动步骤

```bash
# 项目根目录
cd /Users/zeni/projects/t3a

# 1. 启动基础设施 (MySQL, Redis, Nacos, RocketMQ)
docker compose -f docs/docker-compose.yml up -d

# 2. 设置JDK 17 (如果有多个JDK)
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"

# 3. 编译后端
mvn clean install -DskipTests

# 4. 启动后端服务 (4个终端)
cd quiz-gateway && mvn spring-boot:run
cd quiz-core && mvn spring-boot:run
cd quiz-ai && mvn spring-boot:run
cd quiz-communication && mvn spring-boot:run

# 5. 启动前端
cd quiz-frontend
npm install
npm run dev
```

### 访问地址
- 前端: `http://localhost:5173`
- Core API文档: `http://localhost:8081/doc.html`
- AI API文档: `http://localhost:8082/doc.html`
- Nacos: `http://localhost:8848/nacos` (nacos/nacos)

---

## 常见问题排查

### 编译错误
- **`TypeTag :: UNKNOWN`**: 切换到JDK 17
- **端口占用**: `lsof -i :8081` 查看占用进程

### 运行时错误
- **401循环跳登录**: 检查token是否有效、localStorage是否为空
- **AI生成失败**: 检查 `DEEPSEEK_API_KEY` 和Redis连接
- **任务状态NOT_FOUND**: taskId过期(TTL 1小时)或任务未提交成功
- **字符乱码(mojibake)**: 检查数据库字符集和连接配置

### 注意事项
- `docs/t3a-manager.sh` 脚本存在路径解析问题，建议手动启动
- WebSocket默认在前端关闭，避免开发噪音
- 历史会话未提交的数据可能导致Dashboard统计异常

---

## 数据库

### 初始化脚本
`quiz-core/src/main/resources/sql/schema.sql`

### 主要表结构
- `t_user` - 用户表
- `t_question_bank` - 题库表
- `t_question` - 题目表
- `t_quiz_session` - 测验会话
- `t_user_answer` - 用户答案
- `t_ai_analysis` - AI分析结果

---

## 代码阅读顺序

推荐阅读顺序:
1. `quiz-frontend/src/services/api.ts` - 理解前端API调用
2. `quiz-frontend/src/services/*.ts` - 各模块服务层
3. `quiz-core/src/main/java/com/t3a/core/controller/*.java` - 后端控制器
4. `quiz-core/src/main/java/com/t3a/core/service/*.java` - 业务逻辑
5. `quiz-ai/src/main/java/com/t3a/ai/controller/*.java` - AI服务
6. `quiz-frontend/src/pages/*.tsx` - 页面组件

---

## AI生成流程

1. 前端上传文档 → `POST /api/ai/generation/generate`
2. AI服务解析文档 (PDF/TXT/DOC/DOCX)
3. 分批调用LLM生成题目
4. 去重和补题逻辑
5. 回写core题库 (`POST /api/quiz/question/batch`)
6. 更新Redis任务状态
7. 前端轮询状态 → `GET /api/ai/generation/status/{taskId}`

---

## 文档同步规则

以下任一变更时必须同步更新文档:
- ✅ API路径/请求参数/响应结构变化
- ✅ 端口/context-path/代理规则变化
- ✅ 鉴权策略/公开接口/token处理变化
- ✅ 启动方式/脚本变化
- ✅ 功能完成度变化 (TODO → 已完成)

需要更新的文件:
- `docs/README.md`
- `docs/STARTUP-GUIDE.md`
- `docs/ARCHITECTURE.md`
- `docs/PROJECT-SUMMARY.md`
- `docs/AGENT-HANDBOOK.md`

---

## 功能完成度

### 已实现 ✅
- 用户认证 (注册/登录/JWT刷新)
- 题库/题目 CRUD + 去重
- 测验会话创建/提交/结果查询
- AI文档解析 + 题目生成
- 题目类型: 单选/多选/简答/代码
- 主观题要点评分与点评
- Dashboard真实统计
- 结果页参考答案展示

### 已知限制
- 主观题评分为规则匹配(要点命中)，非深度语义评分
- LLM输出质量波动，依赖重试与去重策略
- WebSocket默认关闭
- Dashboard可能受历史脏数据影响

---

## 回归测试脚本

```bash
# 页面冒烟测试
node docs/scripts/e2e-pages-smoke.js

# 功能回归测试
node docs/scripts/e2e-feature-regression.js
```

---

## 推荐入口文档

如果你需要更详细的信息，请参考:
- `docs/AGENT-HANDBOOK.md` - Agent开发者手册
- `docs/README.md` - 项目README
- `docs/STARTUP-GUIDE.md` - 启动指南
- `docs/ARCHITECTURE.md` - 架构说明
