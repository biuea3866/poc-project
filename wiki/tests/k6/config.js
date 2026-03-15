// k6 공통 설정
export const BASE_URL = __ENV.API_BASE || 'http://localhost:8081';
export const API_BASE = `${BASE_URL}/api/v1`;

export const TEST_USER_EMAIL = __ENV.TEST_USER_EMAIL || 'qa-load@test.com';
export const TEST_USER_PASSWORD = __ENV.TEST_USER_PASSWORD || 'Test1234!';

export const DEFAULT_THRESHOLDS = {
  http_req_failed: ['rate<0.05'],         // HTTP 에러율 5% 미만
  http_req_duration: ['p(95)<2000'],      // p95 응답시간 2000ms 미만
};

export const DEFAULT_HEADERS = {
  'Content-Type': 'application/json',
};

/**
 * 랜덤 suffix 생성 (테스트 유저 격리용)
 */
export function randomSuffix() {
  return Math.random().toString(36).substring(2, 10);
}

/**
 * 인증 헤더 생성
 */
export function authHeaders(accessToken) {
  return {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${accessToken}`,
  };
}

/**
 * 회원가입 + 로그인 후 토큰 반환
 * @returns {{ accessToken: string, refreshToken: string, userId: string }}
 */
export function signupAndLogin(http, check, suffix) {
  const email = `qa-${suffix}@test.com`;
  const password = 'Test1234!';
  const name = `QA User ${suffix}`;

  // 회원가입
  const signupRes = http.post(
    `${API_BASE}/auth/signup`,
    JSON.stringify({ email, name, password }),
    { headers: DEFAULT_HEADERS }
  );

  // 이미 존재하는 경우 무시 (409)
  const signupOk = signupRes.status === 201 || signupRes.status === 409;
  check(signupRes, {
    'signup ok': () => signupOk,
  });

  // 로그인
  const loginRes = http.post(
    `${API_BASE}/auth/login`,
    JSON.stringify({ email, password }),
    { headers: DEFAULT_HEADERS }
  );

  check(loginRes, {
    'login status 200': (r) => r.status === 200,
    'login has accessToken': (r) => {
      try {
        return JSON.parse(r.body).accessToken !== undefined;
      } catch {
        return false;
      }
    },
  });

  if (loginRes.status !== 200) {
    return null;
  }

  const body = JSON.parse(loginRes.body);
  return {
    accessToken: body.accessToken,
    refreshToken: body.refreshToken,
    email,
    password,
  };
}
