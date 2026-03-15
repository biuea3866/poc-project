/**
 * k6 문서 CRUD 시나리오
 * login → create document → get document → update document → list documents → delete document
 * 동시 사용자 20명, duration 60s
 * p95 응답시간 < 2000ms
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { API_BASE, DEFAULT_HEADERS, authHeaders, signupAndLogin, randomSuffix } from '../config.js';

const docCrudErrors = new Counter('doc_crud_errors');
const docCreateDuration = new Trend('doc_create_duration');
const docGetDuration = new Trend('doc_get_duration');

export const options = {
  vus: 20,
  duration: '60s',
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<2000'],
    doc_crud_errors: ['count<50'],
  },
};

// 각 VU 시작 시 계정 생성
export function setup() {
  return {};
}

export default function () {
  const suffix = randomSuffix();

  // 1. 로그인 (계정 생성 포함)
  const auth = signupAndLogin(http, check, suffix);
  if (!auth) {
    docCrudErrors.add(1);
    return;
  }

  const headers = authHeaders(auth.accessToken);

  sleep(0.3);

  // 2. 문서 생성
  const createStart = Date.now();
  const createRes = http.post(
    `${API_BASE}/documents`,
    JSON.stringify({
      title: `Test Document ${suffix}`,
      content: `# Test Document\n\nContent for document ${suffix}.\n\nThis is a load test document.`,
      tags: ['test', 'k6', 'load-test'],
    }),
    { headers, tags: { scenario: 'documents', step: 'create' } }
  );
  docCreateDuration.add(Date.now() - createStart);

  const createOk = check(createRes, {
    'create doc: status 201': (r) => r.status === 201,
    'create doc: has id': (r) => {
      try {
        return JSON.parse(r.body).id !== undefined;
      } catch {
        return false;
      }
    },
  });

  if (!createOk) {
    docCrudErrors.add(1);
    return;
  }

  const docId = JSON.parse(createRes.body).id;

  sleep(0.3);

  // 3. 문서 조회
  const getStart = Date.now();
  const getRes = http.get(
    `${API_BASE}/documents/${docId}`,
    { headers, tags: { scenario: 'documents', step: 'get' } }
  );
  docGetDuration.add(Date.now() - getStart);

  check(getRes, {
    'get doc: status 200': (r) => r.status === 200,
    'get doc: correct id': (r) => {
      try {
        return JSON.parse(r.body).id === docId;
      } catch {
        return false;
      }
    },
  });

  sleep(0.3);

  // 4. 문서 수정
  const updateRes = http.put(
    `${API_BASE}/documents/${docId}`,
    JSON.stringify({
      title: `Updated Document ${suffix}`,
      content: `# Updated Document\n\nUpdated content for ${suffix}.`,
    }),
    { headers, tags: { scenario: 'documents', step: 'update' } }
  );

  check(updateRes, {
    'update doc: status 200': (r) => r.status === 200,
  });

  sleep(0.3);

  // 5. 문서 목록 조회
  const listRes = http.get(
    `${API_BASE}/documents?page=0&size=20`,
    { headers, tags: { scenario: 'documents', step: 'list' } }
  );

  check(listRes, {
    'list docs: status 200': (r) => r.status === 200,
    'list docs: has documents array': (r) => {
      try {
        return Array.isArray(JSON.parse(r.body).documents);
      } catch {
        return false;
      }
    },
  });

  sleep(0.3);

  // 6. 문서 삭제 (휴지통)
  const deleteRes = http.del(
    `${API_BASE}/documents/${docId}`,
    null,
    { headers, tags: { scenario: 'documents', step: 'delete' } }
  );

  check(deleteRes, {
    'delete doc: status 200 or 204': (r) => r.status === 200 || r.status === 204,
  });

  sleep(1);
}
