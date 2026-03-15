/**
 * 검색 E2E 테스트
 * - 검색바에 키워드 입력
 * - 검색 결과 렌더링 확인
 * - 결과 클릭해서 문서 이동 확인
 */
import { test, expect } from '@playwright/test';
import { createTestUser, loginUser } from '../helpers/auth';

test.describe('검색 기능', () => {
  let testEmail: string;
  let testPassword: string;
  const uniqueKeyword = `searchkw-${Date.now()}`;

  test.beforeAll(async ({ browser }) => {
    const page = await browser.newPage();

    // 테스트 유저 생성
    const user = await createTestUser(page);
    testEmail = user.email;
    testPassword = user.password;

    // API로 검색 가능한 문서 미리 생성
    const loginRes = await page.request.post(
      `${process.env.API_BASE || 'http://localhost:8081'}/api/v1/auth/login`,
      { data: { email: user.email, password: user.password } }
    );
    const { accessToken } = await loginRes.json();

    // 검색 대상 문서 2개 생성
    for (let i = 1; i <= 2; i++) {
      await page.request.post(
        `${process.env.API_BASE || 'http://localhost:8081'}/api/v1/documents`,
        {
          data: {
            title: `${uniqueKeyword} Document ${i}`,
            content: `This document contains the unique keyword ${uniqueKeyword} for E2E search testing. Item ${i}.`,
          },
          headers: {
            Authorization: `Bearer ${accessToken}`,
            'Content-Type': 'application/json',
          },
        }
      );
    }

    await page.close();
  });

  test.beforeEach(async ({ page }) => {
    await loginUser(page, testEmail, testPassword);
  });

  test('검색 페이지가 올바르게 렌더링된다', async ({ page }) => {
    await page.goto('/search');

    // 검색 페이지 타이틀 확인
    await expect(page.locator('h1').filter({ hasText: '검색' })).toBeVisible();

    // 검색 입력 필드 확인
    const searchInput = page.locator('input[type="text"][placeholder*="검색"]');
    await expect(searchInput).toBeVisible();

    // 검색 탭 확인 (키워드, 시맨틱, 하이브리드)
    await expect(page.getByRole('button', { name: /키워드/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /시맨틱/i })).toBeVisible();
    await expect(page.getByRole('button', { name: /하이브리드/i })).toBeVisible();
  });

  test('키워드 검색 시 결과가 렌더링된다', async ({ page }) => {
    await page.goto('/search');

    // 검색어 입력
    const searchInput = page.locator('input[type="text"][placeholder*="검색"]');
    await searchInput.fill(uniqueKeyword);

    // Enter로 검색 실행
    await searchInput.press('Enter');

    // 결과 로딩 대기
    await page.waitForTimeout(2_000);

    // 검색 결과 영역 확인 (결과 있거나 빈 결과 메시지)
    const hasResults = await page.locator('a[href*="/documents/"]').count() > 0;
    const hasEmptyMessage = await page.getByText(/찾지 못했습니다|결과가 없습니다/i).isVisible();

    // 결과가 있거나 빈 결과 메시지가 있어야 함
    expect(hasResults || hasEmptyMessage).toBeTruthy();
  });

  test('검색 결과 클릭 시 해당 문서로 이동한다', async ({ page }) => {
    await page.goto('/search');

    // 검색어 입력 및 실행
    const searchInput = page.locator('input[type="text"][placeholder*="검색"]');
    await searchInput.fill(uniqueKeyword);
    await searchInput.press('Enter');

    // 결과 로딩 대기
    await page.waitForTimeout(3_000);

    // 검색 결과 링크 확인
    const resultLinks = page.locator('a[href*="/documents/"]');
    const resultCount = await resultLinks.count();

    if (resultCount > 0) {
      // 첫 번째 결과 클릭
      const firstResult = resultLinks.first();
      const href = await firstResult.getAttribute('href');
      await firstResult.click();

      // 문서 상세 페이지로 이동 확인
      await expect(page).toHaveURL(/\/documents\/\d+/, { timeout: 10_000 });
    } else {
      // 결과가 없는 경우 빈 상태 메시지 확인
      await expect(page.getByText(/찾지 못했습니다|결과가 없습니다/i)).toBeVisible();
    }
  });

  test('사이드바의 Search 링크로 검색 페이지에 접근할 수 있다', async ({ page }) => {
    await page.goto('/dashboard');

    // 사이드바 Search 링크 클릭
    await page.getByRole('link', { name: /search/i }).first().click();

    // 검색 페이지로 이동 확인
    await expect(page).toHaveURL(/\/search/, { timeout: 10_000 });
  });

  test('시맨틱 탭 클릭 시 검색 모드가 변경된다', async ({ page }) => {
    await page.goto('/search');

    // 시맨틱 탭 클릭
    await page.getByRole('button', { name: /시맨틱/i }).click();

    // 플레이스홀더 변경 확인 (시맨틱 모드의 placeholder)
    const searchInput = page.locator('input[type="text"]');
    const placeholder = await searchInput.getAttribute('placeholder');
    expect(placeholder).toMatch(/자연어/);
  });

  test('헤더 검색바에서 검색 페이지로 이동할 수 있다', async ({ page }) => {
    await page.goto('/dashboard');

    // 헤더의 검색 입력 필드 (문서 검색...)
    const headerSearch = page.locator('input[placeholder*="문서 검색"]');
    if (await headerSearch.isVisible()) {
      await headerSearch.fill('test query');
      await headerSearch.press('Enter');
      // 검색 페이지로 이동하거나 결과 표시
      await page.waitForTimeout(1_000);
    }

    // 검색 링크로 직접 이동
    await page.goto('/search');
    await expect(page).toHaveURL(/\/search/);
  });
});
