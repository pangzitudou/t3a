# T3A 项目总结（源码对齐版）

## 项目定位
T3A 是一个 AI 生成题库 + 在线答题的微服务系统，包含前端应用、核心业务服务、AI 服务和 WebSocket 通信服务。

## 技术与模块
- 后端: Spring Boot 3.2.2, Java 17
- 前端: React 18.3, Vite 5.4, TypeScript
- 基础设施: MySQL, Redis, Nacos, RocketMQ

模块：
- `quiz-core`: 认证、题库、题目、测验、仪表板
- `quiz-ai`: 文档解析、题目生成、分析
- `quiz-gateway`: 网关路由
- `quiz-communication`: STOMP WebSocket
- `quiz-frontend`: 用户界面

## 真实运行形态
- 本地开发默认“前端 -> Vite 代理 -> core/ai 直连”
- 网关主要用于统一入口场景，不是前端本地调试的必经路径

## 当前完成度

已可用：
- 注册/登录/JWT 刷新
- 题库 CRUD、题目 CRUD、随机抽题
- 创建/提交测验会话、查看结果
- 测验退出（不保留记录）
- 上传文档触发 AI 生成任务，轮询任务状态并回写题库
- 单题提交落库（含主观题要点评分与点评）
- Dashboard 真实统计
- 结果页参考答案与解析展示

持续优化：
- 主观题评分策略（当前为要点命中规则）
- 复杂场景下的模型输出稳定性与质量控制

## 主要风险点
- 文档和代码容易漂移（历史上已出现路径/端口不一致）
- `docs/t3a-manager.sh` 当前路径解析有偏差，建议先手动启动
- AI 依赖外部模型服务和 Redis，可用性受环境影响
- 本地若使用 JDK 25 可能触发 Lombok 编译异常，建议固定 JDK 17

## 推荐入口文档
- `/Users/zeni/projects/t3a/docs/README.md`
- `/Users/zeni/projects/t3a/docs/STARTUP-GUIDE.md`
- `/Users/zeni/projects/t3a/docs/ARCHITECTURE.md`
- `/Users/zeni/projects/t3a/docs/AGENT-HANDBOOK.md`

## 文档维护规则
- 代码变更触发文档同步，至少覆盖：
  1. 路由/API 变化
  2. 端口/代理变化
  3. 鉴权策略变化
  4. 启动方式变化
  5. 功能完成度变化（TODO 变为已完成）
