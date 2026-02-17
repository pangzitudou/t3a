# QA 执行记录（2026-02-17）

## 一、发现的问题
1. 页面乱码：用户昵称等字段出现 `ç®¡ç†å‘˜`。
2. 题库“有库无题/显示 0 题”：`bank/list` 返回 `questionCount` 为空，前端显示为 `0`。
3. 进入测验无题：前端题目数据结构与后端返回结构不一致（未做字段映射）。
4. AI 生成不稳定：
   - 异步读取上传临时文件偶发 `NoSuchFileException`
   - `correct_answer` 字段长度不足导致 500
   - AI 偶发返回非严格 JSON 导致解析失败
5. 题量不足题库残留（历史脏数据）。

## 二、修复动作
1. 字符集与脏数据
   - 增加清洗脚本：`docs/sql/fix-mojibake.sql`
   - 执行后修复 `t_user.nickname` 等乱码，保留备份表
2. 题库题量与列表
   - `QuestionBank` 增加运行时字段 `questionCount`
   - `QuestionBankService` 在列表/详情接口动态填充题目数
3. 前端题目映射
   - `quiz-frontend/src/services/quiz.ts` 增加后端题目到前端题型映射（`questionType/options/correctAnswer/tags`）
   - `QuizPage` 改为按会话的 `bankId` 拉取随机题，并正确渲染
4. AI 生成稳定性
   - 提交任务前缓存上传文件字节，避免临时文件失效
   - AI 响应解析增加“清洗 + 修复重试”
   - 限制单次题量上限为 10，减少模型长输出截断风险
   - `t_question.correct_answer` 扩容为 `TEXT`
5. 题量不足题库处理
   - 对题量 `< 10` 的题库执行逻辑删除（`deleted=1`），避免用户误点失败

## 三、验证结果
1. API 验证
   - `GET /api/quiz/bank/list` 已返回非空 `questionCount`
   - 《Redis开发与运维》题库可查到 `questionCount=10`
   - `GET /api/quiz/question/list?bankId=<redisBankId>` 返回 10 题
2. 页面回归（Playwright）
   - 报告：`/tmp/t3a-pw/out-smoke/report.json`
   - 结果：`ok: true`
   - 截图：`01-dashboard.png`、`02-banks.png`、`03-quiz.png`、`04-result.png`
   - 检查项：无乱码、可进测验、题目正常、可提交到结果页

## 四、当前结论
- 本轮覆盖页面与关键功能已通过。
- 《Redis开发与运维》PDF 对应题库链路（生成/展示/开考）可用。

## 五、补充修复（答题页，2026-02-17）
1. 问题
   - 单选题页面出现“只有题干没有选项”。
   - 题目切换只能依赖 `Previous/Next` 按钮，无法直接点题号跳转。
2. 根因
   - 前端将后端 `SHORT_ANSWER` 默认映射为 `single`，导致问答题被当成单选渲染。
   - 进度条中的题号是 `div`，仅展示不可点击。
3. 修复
   - `quiz-frontend/src/services/quiz.ts`
     - 增加 `SHORT_ANSWER -> short` 映射。
   - `quiz-frontend/src/pages/QuizPage.tsx`
     - 新增 `short` 题型输入框（`textarea`），并保存答案。
     - 进度条题号点击后可直接跳转到指定题目。
   - `quiz-frontend/src/components/quiz/QuizProgress.tsx`
     - 题号改为可点击按钮，新增 `onQuestionClick` 与 `answeredIndexes`，按真实作答索引高亮。
4. 验证
   - 构建通过：`npm run build`（frontend）。
   - Playwright 冒烟：`node docs/scripts/e2e-pages-smoke.js`，结果 `ok: true`。
   - 定向验证：
     - `clickJumpWorks: true`（可点题号跳转）
     - `seenSingle: true` 且 `singleHasOptions: true`（单选题有选项）

## 六、补充修复（多选与结果页，2026-02-17）
1. 问题
   - 多选题只能选一项。
   - 提交后经常显示 0 分，结果页缺少“参考答案/解析”。
2. 根因
   - 前端多选交互仍按单选处理（点击后覆盖旧答案）。
   - 提交时未稳定传递有效分数，后端返回分数与明细不完整时前端无兜底。
3. 修复
   - `quiz-frontend/src/pages/QuizPage.tsx`
     - 多选改为数组切换（可增删选项），并实时提交数组答案。
     - 提交前统一落盘当前题答案（short/code），避免最后一题未保存。
     - 提交时前端计算客观题得分并显式传给 `/quiz/submit`。
     - 结果快照写入 `sessionStorage(quiz_result_local_<sessionKey>)`，用于结果页兜底展示。
   - `quiz-frontend/src/pages/ResultPage.tsx`
     - 当后端分数/答案明细缺失时，自动使用本地结果快照补齐分数、正确率、答题明细。
     - 新增“参考答案与解析”卡片，逐题展示：题干、你的答案、标准答案、解析、正误。
4. 验证
   - 构建通过：`npm run build`（frontend）。
   - 冒烟通过：`node docs/scripts/e2e-pages-smoke.js`，结果 `ok: true`。
   - 定向脚本：`foundMultiple: true`、`multiSelectWorks: true`。

## 七、收尾清理与提交前验证（2026-02-17）
1. 测试数据清理
   - 删除自动化测试题库：`E2E_*`（含 `E2E Redis Test Bank`）
   - 保留业务题库：《Redis开发与运维》题库（id=20）
2. 文档清理
   - 废弃 `docs/claude.md` 的旧状态描述，改为指向主文档
   - 更新 `README/STARTUP-GUIDE/ARCHITECTURE/PROJECT-SUMMARY/AGENT-HANDBOOK/QA_*`
3. 回归结果
   - `node docs/scripts/e2e-feature-regression.js`
   - 报告：`/tmp/t3a-pw/feature-regression/report.json`
   - 结果：`ok: true`（覆盖搜索、去重、参考答案、退出不留记录、生成参数）
