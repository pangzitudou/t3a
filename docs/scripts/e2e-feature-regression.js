const fs = require('fs');
const path = require('path');
function loadPlaywright() {
  try {
    return require('playwright');
  } catch (_) {
    return require(require.resolve('playwright', { paths: ['/tmp/t3a-pw/node_modules'] }));
  }
}
const { chromium } = loadPlaywright();

const BASE = 'http://localhost:5173';
const OUT = '/tmp/t3a-pw/feature-regression';

const fail = (report, stage, issue, detail) => report.issues.push({ stage, issue, detail: detail || '' });
const norm = (s) => String(s || '').trim().replace(/\s+/g, ' ').toLowerCase();

(async () => {
  fs.mkdirSync(OUT, { recursive: true });
  const report = { ok: false, issues: [], checks: {}, shots: [] };

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, reducedMotion: 'reduce' });
  const page = await context.newPage();
  page.on('dialog', async d => d.accept());

  const shot = async (name) => {
    const p = path.join(OUT, name);
    await page.screenshot({ path: p, fullPage: true });
    report.shots.push(p);
  };

  try {
    // 1) Login
    await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
    await page.fill('input[placeholder="用户名"]', 'admin');
    await page.fill('input[placeholder="密码"]', '123456');
    await page.click('button:has-text("登录")');
    await page.waitForURL('**/dashboard', { timeout: 30000 });
    await shot('01-dashboard.png');

    const token = await page.evaluate(() => localStorage.getItem('token'));
    if (!token) {
      fail(report, 'login', 'missing-token');
      throw new Error('token missing');
    }
    const authHeaders = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' };

    // 2) Search in banks page
    await page.goto(`${BASE}/banks`, { waitUntil: 'networkidle' });
    await page.fill('input[placeholder*="搜索题库"]', '__unlikely__keyword__');
    const noResult = page.locator('text=未找到匹配题库');
    if (!(await noResult.isVisible({ timeout: 8000 }).catch(() => false))) {
      fail(report, 'banks-search', 'no-empty-state');
    } else {
      report.checks.banksSearch = true;
    }
    await shot('02-banks-search.png');

    // 3) API: duplicate question protection
    const dupBankName = `E2E_DUP_${Date.now()}`;
    const createDupBank = await context.request.post(`${BASE}/api/quiz/bank/create`, {
      headers: authHeaders,
      data: { name: dupBankName, description: 'dup-test', category: 'E2E', creatorId: 1, isPublic: true, aiGenerated: false }
    });
    const dupBankJson = await createDupBank.json();
    const dupBankId = dupBankJson?.data?.id;
    if (!dupBankId) {
      fail(report, 'dup', 'create-bank-failed', JSON.stringify(dupBankJson));
      throw new Error('dup bank create failed');
    }

    const dupPayload = [
      {
        bankId: dupBankId,
        questionType: 'SINGLE_CHOICE',
        content: '【E2E_DUP】Redis 持久化 RDB 触发方式是？',
        options: JSON.stringify(['AOF always', 'save 规则', '主从同步', 'sentinel']),
        correctAnswer: 'B',
        explanation: 'save 规则触发 RDB',
        difficulty: 'MEDIUM',
        score: 10
      },
      {
        bankId: dupBankId,
        questionType: 'SINGLE_CHOICE',
        content: '【E2E_DUP】Redis 持久化 RDB 触发方式是？',
        options: JSON.stringify(['AOF always', 'save 规则', '主从同步', 'sentinel']),
        correctAnswer: 'B',
        explanation: '重复题干',
        difficulty: 'MEDIUM',
        score: 10
      },
      {
        bankId: dupBankId,
        questionType: 'SHORT_ANSWER',
        content: '【E2E_DUP】简述缓存穿透。',
        options: JSON.stringify([]),
        correctAnswer: '缓存和数据库都不存在被频繁访问；可用布隆过滤器。',
        explanation: '定义+方案',
        difficulty: 'MEDIUM',
        score: 10
      }
    ];
    const batchResp = await context.request.post(`${BASE}/api/quiz/question/batch`, { headers: authHeaders, data: dupPayload });
    const batchJson = await batchResp.json();
    if (batchJson?.code !== 200) {
      fail(report, 'dup', 'batch-create-failed', JSON.stringify(batchJson));
    }

    const listResp = await context.request.get(`${BASE}/api/quiz/question/list?bankId=${dupBankId}`, { headers: { Authorization: `Bearer ${token}` } });
    const listJson = await listResp.json();
    const listData = listJson?.data || [];
    const unique = new Set(listData.map(q => norm(q.content)));
    if (unique.size !== listData.length) {
      fail(report, 'dup', 'list-still-has-duplicates', `len=${listData.length},unique=${unique.size}`);
    } else {
      report.checks.duplicateGuard = true;
    }

    // 4) API+UI: create quiz bank with single/multi/short and verify ref answer in result
    const quizBankName = `E2E_QUIZ_${Date.now()}`;
    const createQuizBank = await context.request.post(`${BASE}/api/quiz/bank/create`, {
      headers: authHeaders,
      data: { name: quizBankName, description: 'quiz-flow-test', category: 'E2E', creatorId: 1, isPublic: true, aiGenerated: false }
    });
    const quizBankJson = await createQuizBank.json();
    const quizBankId = quizBankJson?.data?.id;
    if (!quizBankId) {
      fail(report, 'quiz-flow', 'create-bank-failed', JSON.stringify(quizBankJson));
      throw new Error('quiz bank create failed');
    }

    const quizQuestions = [
      {
        bankId: quizBankId,
        questionType: 'SINGLE_CHOICE',
        content: '【E2E-SINGLE】Redis 单线程主要负责什么？',
        options: JSON.stringify(['AOF重写', '命令执行', '哨兵选举', '集群分片']),
        correctAnswer: 'B',
        explanation: '核心是命令处理',
        difficulty: 'MEDIUM',
        score: 10
      },
      {
        bankId: quizBankId,
        questionType: 'MULTIPLE_CHOICE',
        content: '【E2E-MULTI】哪些属于缓存雪崩应对策略？',
        options: JSON.stringify(['随机过期时间', '多级缓存', '强制全量失效', '降级限流']),
        correctAnswer: 'A,B,D',
        explanation: 'A/B/D 都是常见方案',
        difficulty: 'MEDIUM',
        score: 10
      },
      {
        bankId: quizBankId,
        questionType: 'SHORT_ANSWER',
        content: '【E2E-SHORT】简述 Sentinel 客观下线判定。',
        options: JSON.stringify([]),
        correctAnswer: 'quorum；主观下线后由多个哨兵投票；达到阈值判客观下线。',
        explanation: '包含 quorum、多节点共识、阈值。',
        difficulty: 'MEDIUM',
        score: 10
      }
    ];
    const quizBatch = await context.request.post(`${BASE}/api/quiz/question/batch`, { headers: authHeaders, data: quizQuestions });
    const quizBatchJson = await quizBatch.json();
    if (quizBatchJson?.code !== 200) {
      fail(report, 'quiz-flow', 'seed-questions-failed', JSON.stringify(quizBatchJson));
    }

    const startResp = await context.request.post(`${BASE}/api/quiz/start`, {
      headers: authHeaders,
      data: { userId: 1, bankId: quizBankId, questionCount: 3 }
    });
    const startJson = await startResp.json();
    const sessionKey = startJson?.data?.sessionKey;
    if (!sessionKey) {
      fail(report, 'quiz-flow', 'start-session-failed', JSON.stringify(startJson));
      throw new Error('session start failed');
    }

    await page.goto(`${BASE}/quiz/${sessionKey}`, { waitUntil: 'networkidle' });
    if (await page.locator('text=Explanation:').first().isVisible().catch(() => false)) {
      fail(report, 'quiz-flow', 'explanation-visible-before-submit');
    }

    const safeClick = async (locator) => {
      for (let i = 0; i < 3; i++) {
        try {
          await locator.click({ timeout: 5000, force: true });
          return true;
        } catch (_) {
          await page.waitForTimeout(200);
        }
      }
      return false;
    };

    const answerQuestion = async (index) => {
      await page.locator('button', { hasText: String(index) }).first().click({ timeout: 5000 }).catch(() => {});
      await page.waitForTimeout(250);
      const text = await page.locator('h2').first().innerText().catch(() => '');
      const optionButtons = page.locator('button.w-full.text-left');
      if (text.includes('E2E-SINGLE')) {
        await safeClick(optionButtons.nth(1));
      } else if (text.includes('E2E-MULTI')) {
        await safeClick(optionButtons.nth(0));
        await safeClick(optionButtons.nth(1));
        await safeClick(optionButtons.nth(3));
      } else if (text.includes('E2E-SHORT')) {
        await page.fill('textarea', '需要多个哨兵达成 quorum 共识后才客观下线');
      }
    };

    await answerQuestion(1);
    await answerQuestion(2);
    await answerQuestion(3);

    const submitBtn = page.locator('button:has-text("Submit Quiz")').first();
    await submitBtn.click();
    await page.waitForURL('**/result/**', { timeout: 30000 });
    await shot('03-result.png');

    const hasRefLabel = await page.locator('text=参考答案').first().isVisible({ timeout: 8000 }).catch(() => false);
    const hasStdLabel = await page.locator('text=标准答案').first().isVisible().catch(() => false);
    if (!hasRefLabel || hasStdLabel) {
      fail(report, 'result', 'reference-label-mismatch', `hasRef=${hasRefLabel},hasStd=${hasStdLabel}`);
    }

    const shortBlock = page.locator('div').filter({ hasText: 'E2E-SHORT' }).first();
    const shortText = await shortBlock.innerText().catch(() => '');
    if (!shortText.includes('参考答案') || shortText.includes('暂无参考答案')) {
      fail(report, 'result', 'short-reference-missing');
    } else {
      report.checks.referenceAnswer = true;
    }

    // 5) Exit quiz should not keep record (abandon)
    const start2 = await context.request.post(`${BASE}/api/quiz/start`, {
      headers: authHeaders,
      data: { userId: 1, bankId: quizBankId, questionCount: 3 }
    });
    const start2Json = await start2.json();
    const session2 = start2Json?.data?.sessionKey;
    await page.goto(`${BASE}/quiz/${session2}`, { waitUntil: 'networkidle' });
    await page.locator('button:has-text("退出测验")').click();
    await page.waitForURL('**/banks', { timeout: 15000 });

    const histResp = await context.request.get(`${BASE}/api/quiz/history/1`, { headers: { Authorization: `Bearer ${token}` } });
    const histJson = await histResp.json();
    const history = histJson?.data || [];
    const hasAbandoned = history.some(s => s.sessionKey === session2);
    if (hasAbandoned) {
      fail(report, 'abandon', 'session-still-in-history', session2);
    } else {
      report.checks.abandonNoRecord = true;
    }

    // 6) Generate page should submit exact type counts + custom bankName
    await page.goto(`${BASE}/generate`, { waitUntil: 'networkidle' });
    const tmpFile = '/tmp/t3a-e2e-material.txt';
    fs.writeFileSync(tmpFile, 'Redis persistence, replication, sentinel, cache breakdown.', 'utf8');

    const requestCapture = { hit: false, parsed: null };
    await page.route('**/api/ai/generation/generate', async route => {
      const req = route.request();
      const body = req.postData() || '';
      const m = body.match(/name="request"\r\n\r\n([\s\S]*?)\r\n--/);
      if (m && m[1]) {
        try {
          requestCapture.parsed = JSON.parse(m[1]);
        } catch (_) {}
      }
      requestCapture.hit = true;
      await route.continue();
    });

    const fileInput = page.locator('input[type="file"]');
    await fileInput.setInputFiles(tmpFile);
    await page.fill('input[placeholder*="Redis 进阶题库"]', 'E2E_Custom_Bank_Name');

    const numberInputs = page.locator('input[type="number"]');
    await numberInputs.nth(0).fill('2');
    await numberInputs.nth(1).fill('1');
    await numberInputs.nth(2).fill('1');

    await page.locator('button:has-text("Generate Questions")').click();
    await page.waitForTimeout(3000);

    if (!requestCapture.hit || !requestCapture.parsed) {
      fail(report, 'generate', 'request-not-captured');
    } else {
      const req = requestCapture.parsed;
      const td = JSON.parse(req.typeDistribution || '{}');
      const ok = req.count === 4
        && req.bankName === 'E2E_Custom_Bank_Name'
        && td.SINGLE_CHOICE === 2
        && td.MULTIPLE_CHOICE === 1
        && td.SHORT_ANSWER === 1;
      if (!ok) {
        fail(report, 'generate', 'request-payload-mismatch', JSON.stringify(req));
      } else {
        report.checks.generateRequestPayload = true;
      }
    }

    await shot('04-generate.png');

    report.ok = report.issues.length === 0;
  } catch (e) {
    report.error = String(e);
  }

  fs.writeFileSync(path.join(OUT, 'report.json'), JSON.stringify(report, null, 2));
  await browser.close();
  console.log(JSON.stringify(report, null, 2));
  process.exit(report.ok ? 0 : 1);
})();
