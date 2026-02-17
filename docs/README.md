# TestAgainAndAgain (T3A)

本项目是一个 AI 生成题库的微服务 Web 应用。该文档已按当前源码对齐，优先给出可直接运行和联调的路径。

## 当前技术栈（以代码为准）
- 后端: Spring Boot 3.2.2, Spring Cloud Alibaba 2023.0.1.0, Java 17
- 数据与中间件: MySQL 8, Redis 7, Nacos 2.3, RocketMQ 5
- 前端: React 18.3 + Vite 5.4 + TypeScript + Zustand + Axios

## 服务与端口
- `quiz-gateway`: `8080`
- `quiz-core`: `8081`（`context-path=/quiz`）
- `quiz-ai`: `8082`
- `quiz-communication`: `8083`
- `quiz-frontend`: `5173`

## 先看这个手册
- 推荐先读: [AGENT-HANDBOOK.md](./AGENT-HANDBOOK.md)
  - 包含真实 API 路径、代理改写规则、鉴权要点、已实现/未实现清单、常见坑。

## 快速启动（推荐）
在项目根目录执行：

```bash
# 1) 基础设施
docker compose -f docs/docker-compose.yml up -d

# 2) 后端编译
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"
mvn clean install -DskipTests

# 3) 分别启动 4 个后端服务（4 个终端）
cd quiz-gateway && mvn spring-boot:run
cd quiz-core && mvn spring-boot:run
cd quiz-ai && mvn spring-boot:run
cd quiz-communication && mvn spring-boot:run

# 4) 前端
cd quiz-frontend
npm install
npm run dev
```

## 访问地址
- 前端: `http://localhost:5173`
- 网关: `http://localhost:8080`
- Core API 文档: `http://localhost:8081/doc.html`
- AI API 文档: `http://localhost:8082/doc.html`
- Nacos: `http://localhost:8848/nacos`（`nacos/nacos`）

## 关键说明
- 前端开发默认通过 Vite 代理直连 core/ai，不经过 gateway。
- 回归脚本：
  - 页面冒烟：`node docs/scripts/e2e-pages-smoke.js`
  - 功能回归：`node docs/scripts/e2e-feature-regression.js`
