/**
 * k6 전체 사용자 여정 시나리오
 * - 신규 사용자: signup → create 5 documents → search → logout
 * - 기존 사용자: login → list documents → update document → logout
 * 동시 사용자 30명, ramp-up 10s, sustain 60s, ramp-down 10s
 */
import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import { API_BASE, DEFAULT_HEADERS, authHeaders, signupAndLogin, randomSuffix } from '../config.js';

const newUserJourneySuccess = new Rate('new_user_journey_success');
const existingUserJourneySuccess = new Rate('existing_user_journey_success');
const journeyErrors = new Counter('journey_errors');

// 기존 사용자용 미리 생성된 계정 (setup에서 생성)
let existingUserCredentials = null;

export const options = {
  scenarios: {
    new_users: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 15 },
        { duration: '60s', target: 15 },
        { duration: '10s', target: 0 },
      ],
      tags: { user_type: 'new' },
      exec: 'newUserJourney',
    },
    existing_users: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 15 },
        { duration: '60s', target: 15 },
        { duration: '10s', target: 0 },
      ],
      tags: { user_type: 'existing' },
      exec: 'existingUserJourney',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<2000'],
    new_user_journey_success: ['rate>=0.90'],
    existing_user_journey_success: ['rate>=0.90'],
  },
};

/**
 * 신규 사용자 여정
 */
export function newUserJourney() {
  const suffix = randomSuffix();
  let success = true;

  group('New User Journey', () => {
    // 1. 회원가입
    const signupRes = http.post(
      `${API_BASE}/auth/signup`,
      JSON.stringify({
        email: `new-user-${suffix}@test.com`,
        name: `New User ${suffix}`,
        password: 'Test1234!',
      }),
      { headers: DEFAULT_HEADERS, tags: { step: 'signup' } }
    );

    const signupOk = check(signupRes, {
      'new user signup: 201': (r) => r.status === 201,
    });

    if (!signupOk) {
      journeyErrors.add(1);
      success = false;
      newUserJourneySuccess.add(0);
      return;
    }

    sleep(0.5);

    // 2. 로그인
    const loginRes = http.post(
      `${API_BASE}/auth/login`,
      JSON.stringify({ email: `new-user-${suffix}@test.com`, password: 'Test1234!' }),
      { headers: DEFAULT_HEADERS, tags: { step: 'login' } }
    );

    const loginOk = check(loginRes, {
      'new user login: 200': (r) => r.status === 200,
    });

    if (!loginOk) {
      journeyErrors.add(1);
      success = false;
      newUserJourneySuccess.add(0);
      return;
    }

    const { accessToken, refreshToken } = JSON.parse(loginRes.body);
    const headers = authHeaders(accessToken);

    sleep(0.5);

    // 3. 문서 5개 생성
    const createdDocIds = [];
    for (let i = 1; i <= 5; i++) {
      const createRes = http.post(
        `${API_BASE}/documents`,
        JSON.stringify({
          title: `My Document ${i} - ${suffix}`,
          content: `# My Document ${i}\n\nContent created by new user ${suffix}.`,
          tags: ['user-journey', `doc-${i}`],
        }),
        { headers, tags: { step: 'create-document' } }
      );

      check(createRes, {
        [`create doc ${i}: 201`]: (r) => r.status === 201,
      });

      try {
        createdDocIds.push(JSON.parse(createRes.body).id);
      } catch (_) {
        // ignore
      }

      sleep(0.3);
    }

    // 4. 검색 실행
    const searchRes = http.get(
      `${API_BASE}/search/integrated?query=${encodeURIComponent(suffix)}&page=0&size=20`,
      { headers, tags: { step: 'search' } }
    );

    check(searchRes, {
      'new user search: 200': (r) => r.status === 200,
    });

    sleep(0.5);

    // 5. 로그아웃
    const logoutRes = http.post(
      `${API_BASE}/auth/logout`,
      null,
      { headers, tags: { step: 'logout' } }
    );

    check(logoutRes, {
      'new user logout: 200 or 204': (r) => r.status === 200 || r.status === 204,
    });
  });

  newUserJourneySuccess.add(success ? 1 : 0);
  sleep(1);
}

/**
 * 기존 사용자 여정 (매 VU마다 계정 생성 후 활동)
 */
export function existingUserJourney() {
  const suffix = randomSuffix();
  let success = true;

  group('Existing User Journey', () => {
    // 기존 사용자 시뮬레이션: 로그인부터 시작
    const auth = signupAndLogin(http, check, `exist-${suffix}`);
    if (!auth) {
      journeyErrors.add(1);
      success = false;
      existingUserJourneySuccess.add(0);
      return;
    }

    const headers = authHeaders(auth.accessToken);

    sleep(0.5);

    // 1. 문서 목록 조회
    const listRes = http.get(
      `${API_BASE}/documents?page=0&size=20`,
      { headers, tags: { step: 'list-documents' } }
    );

    const listOk = check(listRes, {
      'existing user list: 200': (r) => r.status === 200,
      'existing user list: has documents': (r) => {
        try {
          return Array.isArray(JSON.parse(r.body).documents);
        } catch {
          return false;
        }
      },
    });

    if (!listOk) {
      success = false;
    }

    sleep(0.5);

    // 2. 문서 생성 후 수정
    const createRes = http.post(
      `${API_BASE}/documents`,
      JSON.stringify({
        title: `Existing User Doc ${suffix}`,
        content: '# Existing User Document\n\nInitial content.',
      }),
      { headers, tags: { step: 'create-for-update' } }
    );

    let docId = null;
    if (createRes.status === 201) {
      try {
        docId = JSON.parse(createRes.body).id;
      } catch (_) {
        // ignore
      }
    }

    sleep(0.5);

    // 3. 문서 수정
    if (docId) {
      const updateRes = http.put(
        `${API_BASE}/documents/${docId}`,
        JSON.stringify({
          title: `Updated Doc ${suffix}`,
          content: '# Updated Document\n\nModified content by existing user.',
        }),
        { headers, tags: { step: 'update-document' } }
      );

      check(updateRes, {
        'existing user update: 200': (r) => r.status === 200,
      });

      sleep(0.3);

      // 정리
      http.del(
        `${API_BASE}/documents/${docId}`,
        null,
        { headers, tags: { step: 'cleanup' } }
      );
    }

    sleep(0.5);

    // 4. 로그아웃
    const logoutRes = http.post(
      `${API_BASE}/auth/logout`,
      null,
      { headers, tags: { step: 'logout' } }
    );

    check(logoutRes, {
      'existing user logout: 200 or 204': (r) => r.status === 200 || r.status === 204,
    });
  });

  existingUserJourneySuccess.add(success ? 1 : 0);
  sleep(1);
}
