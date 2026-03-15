/**
 * k6 인증 시나리오
 * signup → login → refresh → logout 전체 플로우
 * 동시 사용자 10명, duration 30s
 * 성공률 95% 이상
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import { API_BASE, DEFAULT_HEADERS, authHeaders, randomSuffix } from '../config.js';

const authSuccessRate = new Rate('auth_success_rate');
const authErrors = new Counter('auth_errors');

export const options = {
  vus: 10,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<2000'],
    auth_success_rate: ['rate>=0.95'],
  },
};

export default function () {
  const suffix = randomSuffix();
  const email = `qa-auth-${suffix}@test.com`;
  const password = 'Test1234!';
  const name = `Auth User ${suffix}`;

  // 1. 회원가입
  const signupRes = http.post(
    `${API_BASE}/auth/signup`,
    JSON.stringify({ email, name, password }),
    { headers: DEFAULT_HEADERS, tags: { scenario: 'auth', step: 'signup' } }
  );

  const signupOk = check(signupRes, {
    'signup: status 201': (r) => r.status === 201,
  });

  if (!signupOk) {
    authErrors.add(1);
    authSuccessRate.add(0);
    return;
  }

  sleep(0.5);

  // 2. 로그인
  const loginRes = http.post(
    `${API_BASE}/auth/login`,
    JSON.stringify({ email, password }),
    { headers: DEFAULT_HEADERS, tags: { scenario: 'auth', step: 'login' } }
  );

  const loginOk = check(loginRes, {
    'login: status 200': (r) => r.status === 200,
    'login: has accessToken': (r) => {
      try {
        return JSON.parse(r.body).accessToken !== undefined;
      } catch {
        return false;
      }
    },
    'login: has refreshToken': (r) => {
      try {
        return JSON.parse(r.body).refreshToken !== undefined;
      } catch {
        return false;
      }
    },
  });

  if (!loginOk) {
    authErrors.add(1);
    authSuccessRate.add(0);
    return;
  }

  const { accessToken, refreshToken } = JSON.parse(loginRes.body);

  sleep(0.5);

  // 3. 토큰 갱신 (refresh)
  const refreshRes = http.post(
    `${API_BASE}/auth/refresh`,
    JSON.stringify({ refreshToken }),
    { headers: DEFAULT_HEADERS, tags: { scenario: 'auth', step: 'refresh' } }
  );

  const refreshOk = check(refreshRes, {
    'refresh: status 200': (r) => r.status === 200,
    'refresh: has new accessToken': (r) => {
      try {
        return JSON.parse(r.body).accessToken !== undefined;
      } catch {
        return false;
      }
    },
  });

  if (!refreshOk) {
    authErrors.add(1);
    authSuccessRate.add(0);
    return;
  }

  const newAccessToken = JSON.parse(refreshRes.body).accessToken;

  sleep(0.5);

  // 4. 로그아웃
  const logoutRes = http.post(
    `${API_BASE}/auth/logout`,
    null,
    { headers: authHeaders(newAccessToken), tags: { scenario: 'auth', step: 'logout' } }
  );

  const logoutOk = check(logoutRes, {
    'logout: status 200 or 204': (r) => r.status === 200 || r.status === 204,
  });

  if (!logoutOk) {
    authErrors.add(1);
    authSuccessRate.add(0);
    return;
  }

  authSuccessRate.add(1);
  sleep(1);
}
