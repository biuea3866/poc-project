import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright E2E 테스트 설정
 * - baseURL: http://localhost:3000
 * - 브라우저: chromium
 * - 스크린샷: on failure
 * - 비디오: on failure
 * - 리포트: HTML + JSON
 */
export default defineConfig({
  testDir: './specs',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 1 : 2,

  reporter: [
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['json', { outputFile: 'playwright-report/results.json' }],
    ['list'],
  ],

  use: {
    baseURL: process.env.BASE_URL || 'http://localhost:3000',

    // 실패 시 스크린샷
    screenshot: 'only-on-failure',
    screenshotsPath: './screenshots',

    // 실패 시 비디오
    video: 'on-first-retry',

    // 네비게이션 타임아웃
    navigationTimeout: 30_000,
    actionTimeout: 15_000,

    // CI 환경에서 headless
    headless: !!process.env.CI,
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  // 로컬 개발 서버 자동 시작 (로컬 개발용, CI에서는 docker-compose로 기동)
  // webServer: {
  //   command: 'cd ../../frontend && npm run dev',
  //   url: 'http://localhost:3000',
  //   reuseExistingServer: !process.env.CI,
  // },

  // 전역 타임아웃
  timeout: 60_000,
  expect: {
    timeout: 10_000,
  },

  // 출력 디렉토리
  outputDir: './test-results',
});
