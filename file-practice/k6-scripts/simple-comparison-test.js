import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

// 커스텀 메트릭
const syncDuration = new Trend('sync_duration');
const asyncDuration = new Trend('async_duration');

export const options = {
  stages: [
    { duration: '30s', target: 10 },  // 10 VUs로 워밍업
    { duration: '1m', target: 30 },   // 30 VUs로 증가
    { duration: '2m', target: 30 },   // 30 VUs 유지
    { duration: '1m', target: 50 },   // 50 VUs로 증가
    { duration: '2m', target: 50 },   // 50 VUs 유지
    { duration: '30s', target: 0 },   // 종료
  ],
  thresholds: {
    'http_req_duration{api:sync}': ['p(95)<30000'],
    'http_req_duration{api:async}': ['p(95)<20000'],
    'http_req_failed': ['rate<0.1'],
  },
};

const BASE_URL = 'http://localhost:8080';

export default function () {
  // 동기 API 테스트
  const syncResponse = http.get(
    `${BASE_URL}/api/poc/excel/download/sync?dataSize=10000&workbookType=FULL`,
    {
      tags: { api: 'sync' },
      timeout: '60s',
    }
  );

  check(syncResponse, {
    'sync: status 200': (r) => r.status === 200,
    'sync: has content': (r) => r.body && r.body.length > 0,
  });

  syncDuration.add(syncResponse.timings.duration);

  sleep(2);

  // 비동기 API 테스트
  const asyncResponse = http.get(
    `${BASE_URL}/api/poc/excel/download/async?dataSize=10000&workbookType=FULL`,
    {
      tags: { api: 'async' },
      timeout: '60s',
    }
  );

  check(asyncResponse, {
    'async: status 200': (r) => r.status === 200,
    'async: has content': (r) => r.body && r.body.length > 0,
  });

  asyncDuration.add(asyncResponse.timings.duration);

  sleep(2);
}
