/**
 * Smoke Test - 기본 동작 확인
 * VU: 1~2명, 시간: 1분
 */

import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 2,
  duration: '1m',
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  // 1. Health Check
  const healthRes = http.get(`${BASE_URL}/health`);
  check(healthRes, {
    'health check status is 200': (r) => r.status === 200,
  });

  sleep(1);

  // 2. 메인 페이지 (상품 목록)
  const productsRes = http.get(`${BASE_URL}/api/products?page=0&size=20`);
  check(productsRes, {
    'products status is 200': (r) => r.status === 200,
    'products has content': (r) => JSON.parse(r.body).data !== undefined,
  });

  sleep(1);

  // 3. 카테고리 조회
  const categoriesRes = http.get(`${BASE_URL}/api/categories`);
  check(categoriesRes, {
    'categories status is 200': (r) => r.status === 200,
  });

  sleep(1);
}
