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
const OUT = '/tmp/t3a-pw/out-smoke';
const MOJIBAKE_REGEX = /\uFFFD|[ÃÂÐÑ][\u0080-\u00FF]|[åæç][\u0080-\u00FF]/;

(async () => {
  fs.mkdirSync(OUT, { recursive: true });
  const report = { ok: false, issues: [], shots: [] };

  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  page.on('dialog', async d => d.accept());

  const checkText = async (stage) => {
    const text = await page.locator('body').innerText();
    if (MOJIBAKE_REGEX.test(text)) report.issues.push({ stage, issue: 'mojibake' });
  };
  const shot = async (name) => {
    await page.screenshot({ path: path.join(OUT, name), fullPage: true });
    report.shots.push(name);
  };

  try {
    await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' });
    await page.fill('input[placeholder="用户名"]', 'admin');
    await page.fill('input[placeholder="密码"]', '123456');
    await page.click('button:has-text("登录")');
    await page.waitForURL('**/dashboard', { timeout: 30000 });
    await checkText('dashboard');
    await shot('01-dashboard.png');

    await page.goto(`${BASE}/banks`, { waitUntil: 'networkidle' });
    await checkText('banks');
    await shot('02-banks.png');

    const hasRedis = await page.locator('h3:has-text("Redis开发与运维")').count();
    if (!hasRedis) report.issues.push({ stage: 'banks', issue: 'missing-redis-bank' });

    await page.locator('button:has-text("Start Quiz")').first().click();
    await page.waitForURL('**/quiz/**', { timeout: 30000 });
    await page.waitForTimeout(1200);
    await checkText('quiz');
    const q = (await page.locator('h2').first().innerText().catch(() => '')).trim();
    if (!q) report.issues.push({ stage: 'quiz', issue: 'empty-question' });
    await shot('03-quiz.png');

    const submit = page.locator('button:has-text("Submit Quiz")').first();
    const next = page.locator('button:has-text("Next")').first();
    for (let i = 0; i < 12; i++) {
      if (await submit.isVisible().catch(() => false)) break;
      const a = page.locator('button').filter({ hasText: /^A\./ }).first();
      if (await a.isVisible().catch(() => false)) await a.click().catch(() => {});
      if (await next.isVisible().catch(() => false)) await next.click().catch(() => {});
      await page.waitForTimeout(150);
    }
    if (await submit.isVisible().catch(() => false)) {
      await submit.click();
      await page.waitForURL('**/result/**', { timeout: 30000 });
      await checkText('result');
      await shot('04-result.png');
    } else {
      report.issues.push({ stage: 'quiz', issue: 'submit-not-reachable' });
    }

    report.ok = report.issues.length === 0;
  } catch (e) {
    report.error = String(e);
  }

  fs.writeFileSync(path.join(OUT, 'report.json'), JSON.stringify(report, null, 2));
  await browser.close();
  console.log(JSON.stringify(report, null, 2));
  process.exit(report.ok ? 0 : 1);
})();
