import { chromium } from 'playwright';
import { writeFile } from 'fs/promises';

const results = {
  timestamp: new Date().toISOString(),
  success: false,
  steps: [],
  networkRequests: [],
  consoleLogs: [],
  errors: []
};

async function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function runTest() {
  console.log('🚀 Starting Playwright registration test...\n');

  const browser = await chromium.launch({
    headless: false, // Use headed mode to see what's happening
    slowMo: 100 // Slow down actions for better visibility
  });

  const context = await browser.newContext({
    viewport: { width: 1280, height: 720 }
  });

  const page = await context.newPage();

  // Collect console logs
  page.on('console', msg => {
    const text = msg.text();
    const type = msg.type();
    console.log(`[Browser Console ${type}]:`, text);
    results.consoleLogs.push({ type, text, timestamp: new Date().toISOString() });
  });

  // Collect network requests
  page.on('request', request => {
    const url = request.url();
    const method = request.method();
    const headers = request.headers();

    if (url.includes('/auth/register') || url.includes('/api/')) {
      console.log(`[Network Request] ${method} ${url}`);
      console.log('  Headers:', JSON.stringify(headers, null, 2));

      results.networkRequests.push({
        type: 'request',
        method,
        url,
        headers,
        timestamp: new Date().toISOString()
      });
    }
  });

  // Collect network responses
  page.on('response', async (response) => {
    const url = response.url();
    const status = response.status();
    const headers = response.headers();

    if (url.includes('/auth/register') || url.includes('/api/')) {
      console.log(`\n[Network Response] ${status} ${url}`);
      console.log('  Headers:', JSON.stringify(headers, null, 2));

      let body = null;
      try {
        body = await response.text();
        console.log('  Body:', body);
      } catch (e) {
        console.log('  Body: [Unable to read]');
      }

      results.networkRequests.push({
        type: 'response',
        status,
        url,
        headers,
        body,
        timestamp: new Date().toISOString()
      });
    }
  });

  // Listen for page errors
  page.on('pageerror', error => {
    console.log('[Page Error]:', error.message);
    results.errors.push({
      message: error.message,
      stack: error.stack,
      timestamp: new Date().toISOString()
    });
  });

  try {
    // Step 1: Navigate to home page
    console.log('\n📸 Step 1: Navigating to homepage...');
    await page.goto('http://localhost:7782', { waitUntil: 'networkidle' });
    await sleep(1000);
    await page.screenshot({ path: 'quiz-frontend/screenshots/01-homepage.png' });
    results.steps.push({ step: 1, name: 'Homepage', status: 'success', url: page.url() });
    console.log('✅ Homepage loaded:', page.url());

    // Step 2: Navigate to registration page
    console.log('\n📸 Step 2: Navigating to registration page...');
    const registerLink = page.locator('a[href="/register"]');
    if (await registerLink.isVisible()) {
      await registerLink.click();
    } else {
      await page.goto('http://localhost:7782/register');
    }

    await sleep(1000);
    await page.screenshot({ path: 'quiz-frontend/screenshots/02-register-page.png' });
    results.steps.push({ step: 2, name: 'Register Page', status: 'success', url: page.url() });
    console.log('✅ Register page loaded:', page.url());

    // Step 3: Fill registration form
    console.log('\n📸 Step 3: Filling registration form...');
    const timestamp = Date.now();
    const username = `test-${timestamp}`;
    const email = `test-${timestamp}@test.com`;

    // Wait for form to be ready
    await page.waitForSelector('input[placeholder="用户名"]', { timeout: 5000 });

    await page.fill('input[placeholder="用户名"]', username);
    console.log('  ✓ Username:', username);

    await page.fill('input[placeholder="密码"]', '123456');
    console.log('  ✓ Password: ***');

    await page.fill('input[placeholder="邮箱"]', email);
    console.log('  ✓ Email:', email);

    await page.fill('input[placeholder="昵称"]', 'AutoTest');
    console.log('  ✓ Nickname: AutoTest');

    await sleep(500);
    await page.screenshot({ path: 'quiz-frontend/screenshots/03-form-filled.png' });
    results.steps.push({ step: 3, name: 'Form Filled', status: 'success', data: { username, email } });
    console.log('✅ Form filled');

    // Step 4: Submit form
    console.log('\n📸 Step 4: Submitting registration form...');

    // Clear any existing localStorage token before submitting
    await page.evaluate(() => {
      const existingToken = localStorage.getItem('token');
      if (existingToken) {
        console.log('[Test] Clearing existing token:', existingToken);
        localStorage.removeItem('token');
      }
    });

    // Click submit button
    await page.click('button[type="submit"]');
    console.log('  ✓ Submit button clicked');

    // Wait for response
    console.log('  ⏳ Waiting for response...');
    await sleep(5000);

    await page.screenshot({ path: 'quiz-frontend/screenshots/04-after-submit.png' });

    const finalUrl = page.url();
    console.log('  Final URL:', finalUrl);

    // Check if registration was successful
    if (finalUrl.includes('/dashboard')) {
      results.steps.push({ step: 4, name: 'Registration Successful', status: 'success', url: finalUrl });
      results.success = true;
      console.log('✅ Registration successful! Redirected to dashboard');
    } else {
      results.steps.push({ step: 4, name: 'Registration Failed', status: 'failed', url: finalUrl });
      console.log('❌ Registration failed. Not redirected to dashboard');
    }

  } catch (error) {
    console.error('\n❌ Test failed with error:', error.message);
    results.errors.push({
      message: error.message,
      stack: error.stack,
      timestamp: new Date().toISOString()
    });
  } finally {
    await browser.close();
  }

  // Save results
  await writeFile('quiz-frontend/test-results.json', JSON.stringify(results, null, 2));
  console.log('\n📊 Test results saved to quiz-frontend/test-results.json');

  return results;
}

// Run the test
runTest().then(results => {
  console.log('\n' + '='.repeat(60));
  if (results.success) {
    console.log('✅ TEST PASSED: Registration successful!');
  } else {
    console.log('❌ TEST FAILED: Registration did not complete successfully');
    console.log('\nCheck the following for issues:');
    console.log('  1. quiz-frontend/screenshots/*.png - Visual screenshots');
    console.log('  2. quiz-frontend/test-results.json - Detailed logs');
  }
  console.log('='.repeat(60));
  process.exit(results.success ? 0 : 1);
}).catch(error => {
  console.error('Fatal error:', error);
  process.exit(1);
});
