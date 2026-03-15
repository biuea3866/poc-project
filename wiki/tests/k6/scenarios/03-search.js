/**
 * k6 검색 시나리오
 * login → create documents(3개) → search/integrated 검색
 * 동시 사용자 10명, duration 30s
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { API_BASE, DEFAULT_HEADERS, authHeaders, signupAndLogin, randomSuffix } from '../config.js';

const searchErrors = new Counter('search_errors');

export const options = {
  vus: 10,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<3000'],
    search_errors: ['count<20'],
  },
};

export default function () {
  const suffix = randomSuffix();
  const keyword = `searchable-${suffix}`;

  // 1. 로그인
  const auth = signupAndLogin(http, check, suffix);
  if (!auth) {
    searchErrors.add(1);
    return;
  }

  const headers = authHeaders(auth.accessToken);

  sleep(0.3);

  // 2. 검색 가능한 문서 3개 생성
  const docIds = [];
  for (let i = 1; i <= 3; i++) {
    const createRes = http.post(
      `${API_BASE}/documents`,
      JSON.stringify({
        title: `${keyword} Document ${i}`,
        content: `This document contains the keyword ${keyword} for search testing. Index ${i}.`,
        tags: ['search-test', keyword],
      }),
      { headers, tags: { scenario: 'search', step: 'create-doc' } }
    );

    const createOk = check(createRes, {
      [`create doc ${i}: status 201`]: (r) => r.status === 201,
    });

    if (createOk) {
      try {
        docIds.push(JSON.parse(createRes.body).id);
      } catch (_) {
        // ignore
      }
    }

    sleep(0.2);
  }

  // 3. 통합 검색 실행
  const searchRes = http.get(
    `${API_BASE}/search/integrated?query=${encodeURIComponent(keyword)}&page=0&size=20`,
    { headers, tags: { scenario: 'search', step: 'search' } }
  );

  const searchOk = check(searchRes, {
    'search: status 200': (r) => r.status === 200,
    'search: response is object': (r) => {
      try {
        const body = JSON.parse(r.body);
        return typeof body === 'object' && body !== null;
      } catch {
        return false;
      }
    },
  });

  if (!searchOk) {
    searchErrors.add(1);
  }

  sleep(0.5);

  // 4. 빈 검색어 처리 확인
  const emptySearchRes = http.get(
    `${API_BASE}/search/integrated?query=&page=0&size=20`,
    { headers, tags: { scenario: 'search', step: 'empty-search' } }
  );

  check(emptySearchRes, {
    'empty search: handled gracefully': (r) => r.status === 200 || r.status === 400,
  });

  sleep(0.5);

  // 5. 생성한 문서 정리
  for (const docId of docIds) {
    http.del(
      `${API_BASE}/documents/${docId}`,
      null,
      { headers, tags: { scenario: 'search', step: 'cleanup' } }
    );
  }

  sleep(1);
}
