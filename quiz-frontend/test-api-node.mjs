import axios from 'axios';

const results = {
  timestamp: new Date().toISOString(),
  tests: []
};

function log(message, data = null) {
  const timestamp = new Date().toLocaleTimeString();
  console.log(`[${timestamp}] ${message}`);
  if (data) {
    console.log(JSON.stringify(data, null, 2));
  }
}

async function testDirectAPI() {
  log('\n🚀 Starting API Tests...\n');

  const timestamp = Date.now();
  const userData = {
    username: `nodetest-${timestamp}`,
    password: '123456',
    email: `nodetest-${timestamp}@test.com`,
    nickname: 'NodeTest'
  };

  // Test 1: Direct request to backend (port 8081)
  log('\n📝 Test 1: Direct request to quiz-core (port 8081)');
  try {
    const response = await axios.post('http://localhost:8081/quiz/auth/register', userData, {
      headers: {
        'Content-Type': 'application/json'
      }
    });
    log('✅ SUCCESS', { status: response.status, data: response.data });
    results.tests.push({ test: 1, name: 'Direct to Core', status: 'success', data: response.data });
  } catch (error) {
    log('❌ FAILED', {
      status: error.response?.status,
      data: error.response?.data,
      message: error.message
    });
    results.tests.push({ test: 1, name: 'Direct to Core', status: 'failed', error: error.message });
  }

  // Test 2: Request through gateway (port 8080)
  log('\n📝 Test 2: Request through gateway (port 8080)');
  try {
    const response = await axios.post('http://localhost:8080/quiz/auth/register', userData, {
      headers: {
        'Content-Type': 'application/json'
      }
    });
    log('✅ SUCCESS', { status: response.status, data: response.data });
    results.tests.push({ test: 2, name: 'Through Gateway', status: 'success', data: response.data });
  } catch (error) {
    log('❌ FAILED', {
      status: error.response?.status,
      data: error.response?.data,
      message: error.message
    });
    results.tests.push({ test: 2, name: 'Through Gateway', status: 'failed', error: error.message });
  }

  // Test 3: Request through gateway with /api prefix (like frontend does)
  log('\n📝 Test 3: Request with /api prefix (simulating frontend proxy)');
  try {
    const response = await axios.post('http://localhost:8080/api/quiz/auth/register', userData, {
      headers: {
        'Content-Type': 'application/json'
      }
    });
    log('✅ SUCCESS', { status: response.status, data: response.data });
    results.tests.push({ test: 3, name: 'With /api prefix', status: 'success', data: response.data });
  } catch (error) {
    log('❌ FAILED', {
      status: error.response?.status,
      data: error.response?.data,
      message: error.message
    });
    results.tests.push({ test: 3, name: 'With /api prefix', status: 'failed', error: error.message });
  }

  // Test 4: Test with Authorization header (simulating logged-in user trying to register)
  log('\n📝 Test 4: Request with Authorization header (should fail for registration)');
  try {
    const response = await axios.post('http://localhost:8080/quiz/auth/register', userData, {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer invalid-token-12345'
      }
    });
    log('✅ SUCCESS (unexpected)', { status: response.status, data: response.data });
    results.tests.push({ test: 4, name: 'With Auth Header', status: 'success', data: response.data });
  } catch (error) {
    log('❌ FAILED (expected)', {
      status: error.response?.status,
      data: error.response?.data,
      message: error.message
    });
    results.tests.push({ test: 4, name: 'With Auth Header', status: 'failed', error: error.message });
  }

  // Test 5: Test OPTIONS request (CORS preflight)
  log('\n📝 Test 5: OPTIONS request (CORS preflight)');
  try {
    const response = await axios.options('http://localhost:8080/quiz/auth/register', {
      headers: {
        'Origin': 'http://localhost:7782',
        'Access-Control-Request-Method': 'POST',
        'Access-Control-Request-Headers': 'Content-Type'
      }
    });
    log('✅ SUCCESS', { status: response.status, headers: response.headers });
    results.tests.push({ test: 5, name: 'OPTIONS Preflight', status: 'success', data: response.headers });
  } catch (error) {
    log('❌ FAILED', {
      status: error.response?.status,
      data: error.response?.data,
      message: error.message
    });
    results.tests.push({ test: 5, name: 'OPTIONS Preflight', status: 'failed', error: error.message });
  }

  // Summary
  console.log('\n' + '='.repeat(60));
  console.log('📊 TEST SUMMARY');
  console.log('='.repeat(60));

  const successCount = results.tests.filter(t => t.status === 'success').length;
  const failCount = results.tests.filter(t => t.status === 'failed').length;

  results.tests.forEach(test => {
    const icon = test.status === 'success' ? '✅' : '❌';
    console.log(`${icon} Test ${test.test}: ${test.name}`);
  });

  console.log('\n' + '-'.repeat(60));
  console.log(`Total: ${results.tests.length} | ✅ Success: ${successCount} | ❌ Failed: ${failCount}`);
  console.log('='.repeat(60) + '\n');

  return results;
}

testDirectAPI().then(results => {
  process.exit(results.tests.filter(t => t.status === 'success').length > 0 ? 0 : 1);
}).catch(error => {
  console.error('Fatal error:', error);
  process.exit(1);
});
