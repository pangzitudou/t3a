# T3A 启动与联调指南（源码对齐版）

本文档按当前仓库代码整理，适用于本地开发联调。

## 1. 前置要求
- Java 17+
- Maven 3.8+
- Node.js 18+
- Docker / Docker Compose（建议）

快速检查：

```bash
java -version
mvn -version
node -v
docker -v
docker compose version
```

## 2. 推荐启动方式（手动，最稳定）

在项目根目录 `/Users/zeni/projects/t3a` 执行：

```bash
# 1) 启动基础设施
docker compose -f docs/docker-compose.yml up -d

# 2) 编译后端模块
mvn clean install -DskipTests
```

分别打开 4 个终端启动后端：

```bash
cd /Users/zeni/projects/t3a/quiz-gateway && mvn spring-boot:run
cd /Users/zeni/projects/t3a/quiz-core && mvn spring-boot:run
cd /Users/zeni/projects/t3a/quiz-ai && mvn spring-boot:run
cd /Users/zeni/projects/t3a/quiz-communication && mvn spring-boot:run
```

再启动前端：

```bash
cd /Users/zeni/projects/t3a/quiz-frontend
npm install
npm run dev
```

## 3. 服务地址
- 前端: `http://localhost:5173`
- Gateway: `http://localhost:8080`
- Core API 文档: `http://localhost:8081/doc.html`
- AI API 文档: `http://localhost:8082/doc.html`
- Nacos: `http://localhost:8848/nacos`（`nacos/nacos`）
- RocketMQ Console: `http://localhost:8180`

## 4. 核心端口与路径
- `quiz-core` 端口 `8081`，`context-path=/quiz`
- 前端 Vite 代理:
  - `/api/quiz/*` -> `http://localhost:8081/quiz/*`
  - `/api/ai/*` -> `http://localhost:8082/*`

## 5. 启动后自检

```bash
curl http://localhost:8081/doc.html
curl http://localhost:8082/doc.html
curl http://localhost:5173
```

登录接口（core 真实路径）：

```bash
curl -X POST http://localhost:8081/quiz/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"123456"}'
```

## 6. 常见问题

端口占用：
```bash
lsof -i :8081
kill -9 <PID>
```

AI 生成失败：
- 检查 `quiz-ai` 的 API Key 配置（`DEEPSEEK_API_KEY` 或 `application.yml`）。
- 检查 Redis 是否正常（任务状态依赖 Redis）。

`generation/status` 返回 `NOT_FOUND`：
- 任务 ID 不存在或已过期（TTL 1 小时）。

## 7. 关于脚本
- `docs/check-env.sh`、`docs/t3a-manager.sh` 可参考使用。
- 当前推荐“手动启动”作为主流程，避免脚本路径和环境差异导致失败。

## 8. 文档同步约定
- 修改控制器路由、端口、代理、鉴权、启动脚本后，必须同步更新：
  - `docs/README.md`
  - `docs/STARTUP-GUIDE.md`
  - `docs/ARCHITECTURE.md`
  - `docs/PROJECT-SUMMARY.md`
  - `docs/AGENT-HANDBOOK.md`
