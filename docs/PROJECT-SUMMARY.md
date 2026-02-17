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
- 上传文档触发 AI 生成任务，轮询任务状态

进行中：
- AI 生成题目写入 core 题库（未闭环）
- Dashboard 真实统计计算
- AI 分析结果生成
- 单题提交后的细粒度评分与落库

## 主要风险点
- 文档和代码容易漂移（历史上已出现路径/端口不一致）
- `docs/t3a-manager.sh` 当前路径解析有偏差，建议先手动启动
- AI 依赖外部模型服务和 Redis，可用性受环境影响

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
