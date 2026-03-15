/**
 * CSS/레이아웃 체크 스펙
 * - 로그인 페이지 렌더링 확인
 * - 대시보드 레이아웃 확인
 * - 스크린샷 저장
 */
import { test, expect } from '@playwright/test';
import * as path from 'path';
import * as fs from 'fs';
import { createTestUser, loginUser } from '../helpers/auth';

const SCREENSHOTS_DIR = path.join(__dirname, '..', 'screenshots');

test.beforeAll(() => {
  if (!fs.existsSync(SCREENSHOTS_DIR)) {
    fs.mkdirSync(SCREENSHOTS_DIR, { recursive: true });
  }
});

test.describe('CSS/Layout Check', () => {
  test.describe('로그인 페이지 렌더링', () => {
    test('로그인 페이지가 올바르게 렌더링된다', async ({ page }) => {
      await page.goto('/login');

      // .max-w-sm 카드 확인 (AuthForm - max-w-sm 클래스)
      const card = page.locator('.max-w-sm');
      await expect(card).toBeVisible();

      // 스크린샷 저장
      await page.screenshot({
        path: path.join(SCREENSHOTS_DIR, '01-login-page.png'),
        fullPage: true,
      });
    });

    test('AI Wiki 텍스트 그라디언트가 존재한다', async ({ page }) => {
      await page.goto('/login');

      // AI Wiki 텍스트 확인
      const aiWikiText = page.locator('h1').filter({ hasText: 'AI Wiki' });
      await expect(aiWikiText).toBeVisible();

      // 그라디언트 span 확인 (bg-gradient-to-r 클래스)
      const gradientSpan = page.locator(
        'span.bg-gradient-to-r, span[class*="bg-gradient"]'
      ).filter({ hasText: 'AI Wiki' });
      await expect(gradientSpan).toBeVisible();
    });

    test('이메일 입력 필드 border 스타일이 적용된다', async ({ page }) => {
      await page.goto('/login');

      // 이메일 입력 필드
      const emailInput = page.locator('input[type="email"]');
      await expect(emailInput).toBeVisible();

      // border 클래스 확인
      const classList = await emailInput.getAttribute('class');
      expect(classList).toContain('border');

      // border 색상 확인 (border-line 클래스)
      expect(classList).toMatch(/border-/);
    });

    test('로그인 버튼 스타일이 적용된다', async ({ page }) => {
      await page.goto('/login');

      // 로그인 버튼
      const submitButton = page.locator('button[type="submit"]');
      await expect(submitButton).toBeVisible();

      // 그라디언트 버튼 클래스 확인
      const classList = await submitButton.getAttribute('class');
      expect(classList).toMatch(/bg-gradient/);
      expect(classList).toContain('rounded-lg');
    });

    test('비밀번호 입력 필드가 존재한다', async ({ page }) => {
      await page.goto('/login');

      const passwordInput = page.locator('input[type="password"]');
      await expect(passwordInput).toBeVisible();

      const classList = await passwordInput.getAttribute('class');
      expect(classList).toContain('border');
    });
  });

  test.describe('대시보드 레이아웃 확인', () => {
    let testEmail: string;
    let testPassword: string;

    test.beforeAll(async ({ browser }) => {
      const page = await browser.newPage();
      const user = await createTestUser(page);
      testEmail = user.email;
      testPassword = user.password;
      await page.close();
    });

    test('로그인 후 사이드바 네비게이션이 존재한다', async ({ page }) => {
      await loginUser(page, testEmail, testPassword);

      // aside 사이드바 확인
      const sidebar = page.locator('aside');
      await expect(sidebar).toBeVisible();

      // 네비게이션 링크들 확인
      await expect(page.getByRole('link', { name: /documents/i })).toBeVisible();
      await expect(page.getByRole('link', { name: /search/i })).toBeVisible();

      await page.screenshot({
        path: path.join(SCREENSHOTS_DIR, '01-dashboard-sidebar.png'),
        fullPage: false,
      });
    });

    test('헤더가 sticky 고정된다', async ({ page }) => {
      await loginUser(page, testEmail, testPassword);

      // sticky header 확인
      const header = page.locator('header');
      await expect(header).toBeVisible();

      // sticky top-0 클래스 확인
      const classList = await header.getAttribute('class');
      expect(classList).toMatch(/sticky|fixed/);
      expect(classList).toContain('top-0');
    });

    test('main content 영역이 존재한다', async ({ page }) => {
      await loginUser(page, testEmail, testPassword);

      // main 태그 확인
      const main = page.locator('main');
      await expect(main).toBeVisible();

      await page.screenshot({
        path: path.join(SCREENSHOTS_DIR, '01-dashboard-main.png'),
        fullPage: true,
      });
    });

    test('AI Wiki 브랜드명이 헤더에 표시된다', async ({ page }) => {
      await loginUser(page, testEmail, testPassword);

      // 헤더의 AI Wiki 링크
      const brandLink = page.locator('header').getByRole('link', { name: /AI Wiki/i });
      await expect(brandLink).toBeVisible();
    });

    test('새 문서 버튼이 헤더에 존재한다', async ({ page }) => {
      await loginUser(page, testEmail, testPassword);

      const newDocButton = page.getByRole('link', { name: /새 문서/i });
      await expect(newDocButton).toBeVisible();
    });
  });
});
