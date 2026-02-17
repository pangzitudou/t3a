# T3A 全量测试计划（前后端）

更新时间：2026-02-17

## 1. 目标
- 消除页面乱码（中文显示正常，无 mojibake）。
- 确保题库可用（列表题量正确、进入测验有题目、可提交结果）。
- 使用《Redis开发与运维》PDF 验证 AI 题库功能。
- 覆盖功能回归：退出测验、题型数量配置、题库搜索、参考答案、自定义题库名、去重。

## 2. 范围
- 前端页面：`/login`、`/dashboard`、`/banks`、`/generate`、`/quiz/:sessionKey`、`/result/:sessionKey`
- 后端接口：认证、题库列表、题目列表、开始测验、提交测验、AI 生成任务、任务状态查询
- 数据层：MySQL 字符集、历史脏数据、题库与题目一致性

## 3. 验收标准
- 页面文本无乱码（不出现 `�`、`Ãxx`、`çxx`、`åxx` 类型 mojibake）。
- 题库列表返回 `questionCount`，且与实际题目数一致。
- 至少一个《Redis开发与运维》题库 `questionCount >= 10`。
- 点击 `Start Quiz` 后可看到题干与选项，完成提交后进入结果页。
- 关键接口无 4xx/5xx（测试流程内）。
- 退出测验后，会话不出现在 `GET /api/quiz/history/{userId}`。
- 生成请求中 `typeDistribution` 为具体题数，且支持自定义 `bankName`。
- 结果页显示“参考答案”，不显示“标准答案”。
- 同题库题干不重复（入库/抽题均去重）。

## 4. 执行步骤
1. 校验服务进程与端口（`5173/7777/8081/8082/8083`）。
2. 校验数据库字符集与排序规则（server/db/table/connection）。
3. 修复连接配置与历史脏数据（先备份后清洗）。
4. 校验题库和题目一致性（删除/隐藏题量不足题库）。
5. 执行 API 冒烟（登录、题库列表、题目列表、开始测验）。
6. 执行 Playwright 页面回归（登录→题库→开考→结果）。
7. 记录问题、修复后重跑，直到通过。

## 5. 回归命令
- 前端构建：`cd quiz-frontend && npm run build`
- 后端编译（JDK17）：`export JAVA_HOME=$(/usr/libexec/java_home -v 17) && mvn -pl quiz-core,quiz-ai -DskipTests compile`
- 页面冒烟脚本：`node docs/scripts/e2e-pages-smoke.js`
- 功能回归脚本（UI + API）：`node docs/scripts/e2e-feature-regression.js`
- 题库题量检查：
  - 登录获取 token 后调用：`GET /api/quiz/bank/list`
  - 再调用：`GET /api/quiz/question/list?bankId=<id>`
