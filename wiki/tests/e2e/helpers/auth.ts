import { Page, expect } from '@playwright/test';

const API_BASE = process.env.API_BASE || 'http://localhost:8081/api/v1';

/**
 * 랜덤 suffix 생성
 */
export function randomSuffix(): string {
  return Math.random().toString(36).substring(2, 10);
}

/**
 * 테스트 유저 생성 (API 직접 호출)
 * @returns { email, password, name }
 */
export async function createTestUser(
  page: Page,
  suffix?: string
): Promise<{ email: string; password: string; name: string }> {
  const s = suffix || randomSuffix();
  const email = `e2e-${s}@test.com`;
  const password = 'Test1234!';
  const name = `E2E User ${s}`;

  const response = await page.request.post(`${API_BASE}/auth/signup`, {
    data: { email, name, password },
  });

  // 201 또는 409(이미 존재) 허용
  if (response.status() !== 201 && response.status() !== 409) {
    throw new Error(
      `Failed to create test user: ${response.status()} ${await response.text()}`
    );
  }

  return { email, password, name };
}

/**
 * UI를 통한 로그인
 */
export async function loginUser(
  page: Page,
  email: string,
  password: string
): Promise<void> {
  await page.goto('/login');

  // 이메일 입력
  await page.getByLabel(/email/i).fill(email);

  // 비밀번호 입력
  await page.getByLabel(/password/i).fill(password);

  // 로그인 버튼 클릭
  await page.getByRole('button', { name: /log in|sign in|로그인/i }).click();

  // 대시보드로 리다이렉트 대기
  await expect(page).toHaveURL(/\/dashboard/, { timeout: 15_000 });
}

/**
 * UI를 통한 회원가입 + 로그인
 */
export async function signupAndLogin(
  page: Page,
  suffix?: string
): Promise<{ email: string; password: string; name: string }> {
  const user = await createTestUser(page, suffix);
  await loginUser(page, user.email, user.password);
  return user;
}

/**
 * API를 통한 로그인 (토큰 반환)
 */
export async function loginViaAPI(
  page: Page,
  email: string,
  password: string
): Promise<{ accessToken: string; refreshToken: string }> {
  const response = await page.request.post(`${API_BASE}/auth/login`, {
    data: { email, password },
  });

  if (response.status() !== 200) {
    throw new Error(
      `API login failed: ${response.status()} ${await response.text()}`
    );
  }

  return response.json();
}

/**
 * 로그아웃 (UI)
 */
export async function logoutUser(page: Page): Promise<void> {
  // 로그아웃 버튼 찾기 (사이드바 또는 헤더)
  const logoutButton = page.getByRole('button', { name: /logout|log out|로그아웃/i });
  await logoutButton.click();
  await expect(page).toHaveURL(/\/login/, { timeout: 10_000 });
}
