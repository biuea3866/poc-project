/**
 * k6 부하 테스트 (전체 API)
 * stages: 30s ramp-up to 50 → 60s sustain at 100 → 30s ramp-down
 * HTTP 에러율 < 5%
 * p99 응답시간 < 5000ms
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { API_BASE, DEFAULT_HEADERS, authHeaders, signupAndLogin, randomSuffix } from './config.js';

const httpErrorRate = new Rate('http_error_rate');
const apiCallErrors = new Counter('api_call_errors');
const apiCallDuration = new Trend('api_call_duration', true);

export const options = {
  stages: [
    { duration: '30s', target: 50 },
    { duration: '60s', target: 100 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(99)<5000'],
    http_error_rate: ['rate<0.05'],
  },
};

export default function () {
  const suffix = randomSuffix();

  // === 인증 API ===
  // 회원가입
  const signupRes = http.post(
    `${API_BASE}/auth/signup`,
    JSON.stringify({
      email: `load-${suffix}@test.com`,
      name: `Load User ${suffix}`,
      password: 'Test1234!',
    }),
    { headers: DEFAULT_HEADERS, tags: { api: 'auth', endpoint: 'signup' } }
  );

  httpErrorRate.add(signupRes.status >= 500 ? 1 : 0);

  if (signupRes.status !== 201) {
    apiCallErrors.add(1);
    return;
  }

  sleep(0.2);

  // 로그인
  const loginRes = http.post(
    `${API_BASE}/auth/login`,
    JSON.stringify({ email: `load-${suffix}@test.com`, password: 'Test1234!' }),
    { headers: DEFAULT_HEADERS, tags: { api: 'auth', endpoint: 'login' } }
  );

  httpErrorRate.add(loginRes.status >= 500 ? 1 : 0);
  check(loginRes, {
    'login: 200': (r) => r.status === 200,
  });

  if (loginRes.status !== 200) {
    apiCallErrors.add(1);
    return;
  }

  const { accessToken, refreshToken } = JSON.parse(loginRes.body);
  const headers = authHeaders(accessToken);

  sleep(0.2);

  // 토큰 갱신
  const refreshRes = http.post(
    `${API_BASE}/auth/refresh`,
    JSON.stringify({ refreshToken }),
    { headers: DEFAULT_HEADERS, tags: { api: 'auth', endpoint: 'refresh' } }
  );

  httpErrorRate.add(refreshRes.status >= 500 ? 1 : 0);
  const newToken = refreshRes.status === 200
    ? JSON.parse(refreshRes.body).accessToken
    : accessToken;
  const updatedHeaders = authHeaders(newToken);

  sleep(0.2);

  // === 문서 API ===
  // 문서 생성
  const createDocRes = http.post(
    `${API_BASE}/documents`,
    JSON.stringify({
      title: `Load Test Doc ${suffix}`,
      content: `# Load Test\n\nContent for load test ${suffix}.`,
      tags: ['load-test'],
    }),
    { headers: updatedHeaders, tags: { api: 'documents', endpoint: 'create' } }
  );

  httpErrorRate.add(createDocRes.status >= 500 ? 1 : 0);
  check(createDocRes, {
    'create doc: 201': (r) => r.status === 201,
  });

  let docId = null;
  if (createDocRes.status === 201) {
    try {
      docId = JSON.parse(createDocRes.body).id;
    } catch (_) {
      // ignore
    }
  }

  sleep(0.2);

  // 문서 목록 조회
  const listDocsRes = http.get(
    `${API_BASE}/documents?page=0&size=10`,
    { headers: updatedHeaders, tags: { api: 'documents', endpoint: 'list' } }
  );

  httpErrorRate.add(listDocsRes.status >= 500 ? 1 : 0);
  check(listDocsRes, {
    'list docs: 200': (r) => r.status === 200,
  });

  sleep(0.2);

  if (docId) {
    // 문서 상세 조회
    const getDocRes = http.get(
      `${API_BASE}/documents/${docId}`,
      { headers: updatedHeaders, tags: { api: 'documents', endpoint: 'get' } }
    );

    httpErrorRate.add(getDocRes.status >= 500 ? 1 : 0);
    check(getDocRes, {
      'get doc: 200': (r) => r.status === 200,
    });

    sleep(0.2);

    // 문서 수정
    const updateDocRes = http.put(
      `${API_BASE}/documents/${docId}`,
      JSON.stringify({ title: `Updated Load Test Doc ${suffix}`, content: '# Updated' }),
      { headers: updatedHeaders, tags: { api: 'documents', endpoint: 'update' } }
    );

    httpErrorRate.add(updateDocRes.status >= 500 ? 1 : 0);

    sleep(0.2);

    // 문서 삭제
    const deleteDocRes = http.del(
      `${API_BASE}/documents/${docId}`,
      null,
      { headers: updatedHeaders, tags: { api: 'documents', endpoint: 'delete' } }
    );

    httpErrorRate.add(deleteDocRes.status >= 500 ? 1 : 0);

    sleep(0.2);
  }

  // === 검색 API ===
  const searchRes = http.get(
    `${API_BASE}/search/integrated?query=${encodeURIComponent('load test')}&page=0&size=10`,
    { headers: updatedHeaders, tags: { api: 'search', endpoint: 'integrated' } }
  );

  httpErrorRate.add(searchRes.status >= 500 ? 1 : 0);
  check(searchRes, {
    'search: 200': (r) => r.status === 200,
  });

  sleep(0.2);

  // === 태그 API ===
  const tagsRes = http.get(
    `${API_BASE}/tags/types`,
    { headers: updatedHeaders, tags: { api: 'tags', endpoint: 'types' } }
  );

  httpErrorRate.add(tagsRes.status >= 500 ? 1 : 0);

  sleep(0.2);

  // 휴지통 조회
  const trashRes = http.get(
    `${API_BASE}/documents/trash`,
    { headers: updatedHeaders, tags: { api: 'documents', endpoint: 'trash' } }
  );

  httpErrorRate.add(trashRes.status >= 500 ? 1 : 0);

  sleep(0.2);

  // 로그아웃
  http.post(
    `${API_BASE}/auth/logout`,
    null,
    { headers: updatedHeaders, tags: { api: 'auth', endpoint: 'logout' } }
  );

  sleep(0.5);
}

export function handleSummary(data) {
  return {
    'tests/k6/results/load-test-summary.json': JSON.stringify(data, null, 2),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}

function textSummary(data, opts) {
  const indent = opts.indent || '';
  const lines = [];

  lines.push(`${indent}=== Load Test Summary ===`);
  lines.push(`${indent}Total requests: ${data.metrics.http_reqs ? data.metrics.http_reqs.values.count : 'N/A'}`);
  lines.push(`${indent}Error rate: ${data.metrics.http_req_failed ? (data.metrics.http_req_failed.values.rate * 100).toFixed(2) : 'N/A'}%`);
  lines.push(`${indent}p95 duration: ${data.metrics.http_req_duration ? data.metrics.http_req_duration.values['p(95)'].toFixed(2) : 'N/A'}ms`);
  lines.push(`${indent}p99 duration: ${data.metrics.http_req_duration ? data.metrics.http_req_duration.values['p(99)'].toFixed(2) : 'N/A'}ms`);

  return lines.join('\n');
}
