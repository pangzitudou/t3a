const { execFileSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const CWD = '/Users/zeni/projects/t3a';
const SESSION = 't3a-full';
const OUT = '/tmp/t3a-agent-browser';
fs.mkdirSync(OUT, { recursive: true });

const report = {
  ok: false,
  startedAt: new Date().toISOString(),
  checks: {},
  issues: [],
  screenshots: [],
  meta: { tool: 'agent-browser' }
};

function ab(args, { allowFail = false, withSession = true } = {}) {
  const fullArgs = [...args];
  if (withSession) {
    fullArgs.push('--session', SESSION);
  }
  try {
    const out = execFileSync('npx', ['agent-browser', ...fullArgs], {
      cwd: CWD,
      encoding: 'utf8',
      stdio: ['pipe', 'pipe', 'pipe']
    });
    return out.trim();
  } catch (e) {
    const stdout = e.stdout ? String(e.stdout) : '';
    const stderr = e.stderr ? String(e.stderr) : '';
    if (allowFail) {
      return `${stdout}\n${stderr}`.trim();
    }
    throw new Error(`agent-browser failed: ${fullArgs.join(' ')}\n${stdout}\n${stderr}`);
  }
}

function evalJs(script) {
  return ab(['eval', script]);
}

function wait(ms) {
  ab(['wait', String(ms)]);
}

function waitUrlContains(fragment, maxAttempts = 30, sleepMs = 1000) {
  for (let i = 0; i < maxAttempts; i++) {
    const url = sanitize(ab(['get', 'url'], { allowFail: true }));
    if (url.includes(fragment)) return true;
    wait(sleepMs);
  }
  return false;
}

function shot(name) {
  const p = path.join(OUT, name);
  ab(['screenshot', p]);
  report.screenshots.push(p);
}

function fail(stage, issue, detail = '') {
  report.issues.push({ stage, issue, detail });
}

function toBool(output) {
  const v = output.trim();
  return v === 'true' || v === '1';
}

function sanitize(output) {
  const t = output.trim();
  const lines = t.split('\n').map((s) => s.trim()).filter(Boolean);
  return lines.length ? lines[lines.length - 1] : t;
}

(async () => {
  try {
    ab(['close'], { allowFail: true });

    // Login
    ab(['open', 'http://localhost:5173/login']);
    ab(['wait', '--load', 'networkidle']);
    shot('01-login.png');

    ab(['fill', "input[placeholder='用户名']", 'admin']);
    ab(['fill', "input[placeholder='密码']", '123456']);
    ab(['find', 'role', 'button', 'click', '--name', '登录']);
    ab(['wait', '--url', '**/dashboard']);
    shot('02-dashboard.png');
    report.checks.login = true;

    // Banks + search
    ab(['open', 'http://localhost:5173/banks']);
    ab(['wait', '--load', 'networkidle']);
    shot('03-banks.png');

    ab(['fill', "input[placeholder*='搜索题库']", '__not_exists__']);
    ab(['wait', '--text', '未找到匹配题库']);
    report.checks.bankSearch = true;
    ab(['fill', "input[placeholder*='搜索题库']", '']);

    // Start quiz
    ab(['find', 'first', "button:has-text('Start Quiz')", 'click']);
    ab(['wait', '--url', '**/quiz/**']);
    ab(['wait', '--load', 'networkidle']);
    shot('04-quiz.png');

    const hasPrematureExplanation = toBool(sanitize(evalJs(
      "document.body.innerText.includes('Explanation:')"
    )));
    if (hasPrematureExplanation) {
      fail('quiz', 'explanation-visible-before-submit');
    } else {
      report.checks.noPrematureExplanation = true;
    }

    // try click question number 2 to validate direct navigation
    const jumpOk = toBool(sanitize(evalJs(`(() => {
      const buttons = Array.from(document.querySelectorAll('button')).filter(b => /^\\d+$/.test((b.textContent||'').trim()));
      const target = buttons.find(b => (b.textContent||'').trim() === '2');
      if (!target) return false;
      target.click();
      return true;
    })()`)));
    report.checks.questionJump = jumpOk;

    // Answer loop
    // Navigate forward and answer until submit appears
    for (let i = 0; i < 25; i++) {
      const hasSubmit = toBool(sanitize(evalJs(\"document.body.innerText.includes('Submit Quiz')\")));
      if (hasSubmit) break;
      sanitize(evalJs(`(() => {
        const kind = document.body.innerText || '';
        const shortInput = document.querySelector('textarea');
        if (shortInput) {
          shortInput.value = '这是自动化测试回答：包含关键要点、定义与机制。';
          shortInput.dispatchEvent(new Event('input', { bubbles: true }));
        }
        const options = Array.from(document.querySelectorAll('button.w-full.text-left'));
        if (options.length > 0) {
          options[0].click();
          if (/多选题|Multiple Choice/i.test(kind) && options[1]) options[1].click();
        }
        const next = Array.from(document.querySelectorAll('button')).find(b => /Next/.test(b.textContent || ''));
        if (next) {
          next.click();
          return 'next';
        }
        return 'none';
      })()`));
      wait(500);
    }

    ab(['find', 'text', 'Submit Quiz', 'click'], { allowFail: true });
    const onResult = waitUrlContains('/result/', 20, 1000);
    if (!onResult) {
      fail('quiz', 'submit-button-not-reached');
    } else {
      ab(['wait', '--load', 'networkidle']);
      shot('05-result.png');
      report.checks.quizSubmit = true;
    }

    const resultChecks = sanitize(evalJs(`(() => JSON.stringify({
      hasRef: document.body.innerText.includes('参考答案'),
      hasStd: document.body.innerText.includes('标准答案')
    }))()`));
    try {
      let obj;
      try {
        obj = JSON.parse(resultChecks);
      } catch {
        obj = JSON.parse(resultChecks.replace(/^\"|\"$/g, '').replaceAll('\\\\\"', '\"'));
      }
      if (obj.hasRef && !obj.hasStd) {
        report.checks.resultLabel = true;
      } else {
        fail('result', 'reference-answer-label-invalid', JSON.stringify(obj));
      }
    } catch {
      fail('result', 'cannot-parse-result-label-check', resultChecks);
    }

    // Dashboard stats API (through auth token from browser)
    const tokenRaw = sanitize(evalJs('localStorage.getItem(\'token\')'));
    const token = tokenRaw.replace(/^"|"$/g, '');
    if (!token) {
      fail('dashboard', 'token-missing-after-login');
    } else {
      const dashboardRaw = execFileSync('curl', [
        '-sS',
        'http://localhost:8081/quiz/dashboard',
        '-H',
        `Authorization: Bearer ${token}`
      ], { encoding: 'utf8' }).trim();
      try {
        const obj = JSON.parse(dashboardRaw);
        const completed = obj?.data?.quizzesCompleted ?? 0;
        if (typeof completed === 'number' && completed >= 1) {
          report.checks.dashboardStats = true;
        } else {
          fail('dashboard', 'quizzesCompleted-not-updated', dashboardRaw);
        }
      } catch {
        fail('dashboard', 'dashboard-json-invalid', dashboardRaw);
      }
    }

    // Generate 20 questions
    const material = '/tmp/t3a-agent-material.txt';
    fs.writeFileSync(material, 'Redis cache, sentinel, persistence, replication, pipeline, bigkey, hotkey.', 'utf8');
    const customBank = `AGENT_BROWSER_${Date.now()}`;

    ab(['open', 'http://localhost:5173/generate']);
    ab(['wait', '--load', 'networkidle']);
    shot('06-generate.png');

    ab(['upload', "input[type='file']", material]);
    ab(['fill', "input[placeholder*='Redis 进阶题库']", customBank]);

    sanitize(evalJs(`(() => {
      const nums = Array.from(document.querySelectorAll('input[type="number"]'));
      if (nums.length < 3) return 'not-enough-number-inputs';
      nums[0].value = '8'; nums[0].dispatchEvent(new Event('input', { bubbles: true }));
      nums[1].value = '6'; nums[1].dispatchEvent(new Event('input', { bubbles: true }));
      nums[2].value = '6'; nums[2].dispatchEvent(new Event('input', { bubbles: true }));
      return 'ok';
    })()`));

    sanitize(evalJs(`(() => {
      const btn = Array.from(document.querySelectorAll('button')).find(b => /Generate Questions/i.test(b.textContent || ''));
      if (!btn) return 'no-generate-button';
      btn.click();
      return 'clicked';
    })()`));

    let generated = false;
    let generationFailed = false;
    for (let i = 0; i < 120; i++) {
      const url = sanitize(ab(['get', 'url']));
      if (url.includes('/banks')) {
        generated = true;
        break;
      }
      const genState = sanitize(evalJs(`(() => JSON.stringify({
        failed: /Failed to generate|生成失败|题目生成失败/i.test(document.body.innerText || ''),
        generating: /Generating\\.\\.\\.|Generating/i.test(document.body.innerText || '')
      }))()`));
      try {
        const s = JSON.parse(genState.replace(/^\"|\"$/g, '').replaceAll('\\\\\"', '\"'));
        if (s.failed) {
          generationFailed = true;
          break;
        }
      } catch {}
      wait(2000);
    }

    if (!generated) {
      fail('generate', generationFailed ? 'generation-failed' : 'generation-timeout-or-failed');
      shot('07-generate-timeout.png');
    } else {
      ab(['wait', '--load', 'networkidle']);
      ab(['fill', "input[placeholder*='搜索题库']", customBank]);
      const found = toBool(sanitize(evalJs(`(() => document.body.innerText.includes('${customBank}'))()`)));
      if (found) {
        report.checks.generate20 = true;
      } else {
        fail('generate', 'custom-bank-not-found-after-generate', customBank);
      }
      shot('07-banks-after-generate.png');
    }

    report.ok = report.issues.length === 0;
  } catch (e) {
    report.error = String(e.stack || e);
  } finally {
    report.finishedAt = new Date().toISOString();
    const p = path.join(OUT, 'report.json');
    fs.writeFileSync(p, JSON.stringify(report, null, 2));
    try { ab(['close'], { allowFail: true }); } catch {}
    console.log(JSON.stringify(report, null, 2));
  }
})();
