import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// 커스텀 메트릭
const successRate = new Rate('success_rate');
const requestDuration = new Trend('request_duration');
const requestCount = new Counter('request_count');

// 테스트 시나리오 설정
export const options = {
  scenarios: {
    // 시나리오 1: 동기 API - 워밍업 (10 VUs)
    sync_warmup: {
      executor: 'constant-vus',
      exec: 'testSyncAPI',
      vus: 10,
      duration: '30s',
      startTime: '0s',
      tags: { test_type: 'sync', phase: 'warmup' },
    },
    // 시나리오 2: 동기 API - 부하 테스트 (50 VUs)
    sync_load: {
      executor: 'ramping-vus',
      exec: 'testSyncAPI',
      startVUs: 10,
      stages: [
        { duration: '30s', target: 50 },  // 50명까지 증가
        { duration: '1m', target: 50 },   // 50명 유지
        { duration: '30s', target: 100 }, // 100명까지 증가
        { duration: '1m', target: 100 },  // 100명 유지
        { duration: '30s', target: 0 },   // 0명으로 감소
      ],
      startTime: '40s',
      tags: { test_type: 'sync', phase: 'load' },
    },
    // 시나리오 3: 비동기 API - 워밍업 (10 VUs)
    async_warmup: {
      executor: 'constant-vus',
      exec: 'testAsyncAPI',
      vus: 10,
      duration: '30s',
      startTime: '5m',
      tags: { test_type: 'async', phase: 'warmup' },
    },
    // 시나리오 4: 비동기 API - 부하 테스트 (50 VUs)
    async_load: {
      executor: 'ramping-vus',
      exec: 'testAsyncAPI',
      startVUs: 10,
      stages: [
        { duration: '30s', target: 50 },
        { duration: '1m', target: 50 },
        { duration: '30s', target: 100 },
        { duration: '1m', target: 100 },
        { duration: '30s', target: 0 },
      ],
      startTime: '5m40s',
      tags: { test_type: 'async', phase: 'load' },
    },
    // 시나리오 5: 스파이크 테스트 (동기)
    sync_spike: {
      executor: 'ramping-vus',
      exec: 'testSyncAPI',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 200 }, // 급격한 증가
        { duration: '30s', target: 200 }, // 유지
        { duration: '10s', target: 0 },   // 급격한 감소
      ],
      startTime: '11m',
      tags: { test_type: 'sync', phase: 'spike' },
    },
    // 시나리오 6: 스파이크 테스트 (비동기)
    async_spike: {
      executor: 'ramping-vus',
      exec: 'testAsyncAPI',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 200 },
        { duration: '30s', target: 200 },
        { duration: '10s', target: 0 },
      ],
      startTime: '12m',
      tags: { test_type: 'async', phase: 'spike' },
    },
  },
  thresholds: {
    'http_req_duration{test_type:sync}': ['p(95)<5000'], // 동기: 95%가 5초 이내
    'http_req_duration{test_type:async}': ['p(95)<3000'], // 비동기: 95%가 3초 이내
    'http_req_failed{test_type:sync}': ['rate<0.1'], // 동기: 실패율 10% 미만
    'http_req_failed{test_type:async}': ['rate<0.05'], // 비동기: 실패율 5% 미만
  },
};

const BASE_URL = 'http://localhost:8080';

// 동기 API 테스트
export function testSyncAPI() {
  const url = `${BASE_URL}/api/poc/excel/download/sync?dataSize=100&workbookType=FULL`;

  const params = {
    headers: {
      'Accept': 'application/octet-stream',
    },
    tags: { name: 'sync_download' },
    timeout: '10s',
  };

  const response = http.get(url, params);

  const success = check(response, {
    'status is 200': (r) => r.status === 200,
    'has content': (r) => r.body && r.body.length > 0,
    'response time < 5s': (r) => r.timings.duration < 5000,
  });

  successRate.add(success);
  requestDuration.add(response.timings.duration);
  requestCount.add(1);

  sleep(1); // 1초 대기
}

// 비동기 API 테스트
export function testAsyncAPI() {
  const url = `${BASE_URL}/api/poc/excel/download/async?dataSize=100&workbookType=FULL`;

  const params = {
    headers: {
      'Accept': 'application/octet-stream',
    },
    tags: { name: 'async_download' },
    timeout: '10s',
  };

  const response = http.get(url, params);

  const success = check(response, {
    'status is 200': (r) => r.status === 200,
    'has content': (r) => r.body && r.body.length > 0,
    'response time < 3s': (r) => r.timings.duration < 3000,
  });

  successRate.add(success);
  requestDuration.add(response.timings.duration);
  requestCount.add(1);

  sleep(1);
}

// 테스트 완료 시 요약 출력
export function handleSummary(data) {
  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
    'k6-results/poc-excel-performance-report.json': JSON.stringify(data),
  };
}

function textSummary(data, opts) {
  const indent = opts.indent || '';
  const colors = opts.enableColors || false;

  let summary = '\n';
  summary += '================================================================================\n';
  summary += '                    POC Excel Download Performance Report                     \n';
  summary += '================================================================================\n\n';

  // 동기 API 결과
  const syncMetrics = getMetricsByTag(data, 'sync');
  summary += '--- 동기 API (Sync) ---\n';
  summary += formatMetrics(syncMetrics, indent);
  summary += '\n';

  // 비동기 API 결과
  const asyncMetrics = getMetricsByTag(data, 'async');
  summary += '--- 비동기 API (Async) ---\n';
  summary += formatMetrics(asyncMetrics, indent);
  summary += '\n';

  return summary;
}

function getMetricsByTag(data, tag) {
  // k6 메트릭에서 태그별로 필터링하는 로직
  return data.metrics;
}

function formatMetrics(metrics, indent) {
  let output = '';
  // 메트릭 포맷팅 로직
  return output;
}
