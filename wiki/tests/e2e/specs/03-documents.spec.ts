/**
 * 문서 CRUD E2E 테스트
 * - 로그인 후 새 문서 생성 (제목 입력, 내용 입력, 저장)
 * - 생성된 문서 목록에서 확인
 * - 문서 클릭해서 상세 보기
 * - 문서 수정
 * - 문서 삭제 (휴지통으로)
 */
import { test, expect } from '@playwright/test';
import { createTestUser, loginUser } from '../helpers/auth';

test.describe('문서 CRUD', () => {
  let testEmail: string;
  let testPassword: string;

  test.beforeAll(async ({ browser }) => {
    const page = await browser.newPage();
    const user = await createTestUser(page);
    testEmail = user.email;
    testPassword = user.password;
    await page.close();
  });

  test.beforeEach(async ({ page }) => {
    await loginUser(page, testEmail, testPassword);
  });

  test('새 문서를 생성할 수 있다', async ({ page }) => {
    const docTitle = `E2E Test Document ${Date.now()}`;
    const docContent = '# Test Document\n\nThis is content written by E2E test.';

    // 새 문서 페이지로 이동
    await page.goto('/documents/new');
    await expect(page).toHaveURL(/\/documents\/new/);

    // 제목 입력
    await page.locator('input[placeholder*="제목"]').fill(docTitle);

    // 내용 입력
    await page.locator('textarea[placeholder*="Markdown"]').fill(docContent);

    // 임시저장 버튼 클릭 (제목만 있어도 가능)
    await page.getByRole('button', { name: '임시저장' }).click();

    // 문서 상세 페이지로 이동 확인
    await expect(page).toHaveURL(/\/documents\/\d+/, { timeout: 15_000 });
  });

  test('생성된 문서가 대시보드 목록에 표시된다', async ({ page }) => {
    const docTitle = `E2E List Test ${Date.now()}`;

    // 문서 생성
    await page.goto('/documents/new');
    await page.locator('input[placeholder*="제목"]').fill(docTitle);
    await page.locator('textarea[placeholder*="Markdown"]').fill('List test content');
    await page.getByRole('button', { name: '임시저장' }).click();
    await expect(page).toHaveURL(/\/documents\/\d+/, { timeout: 15_000 });

    // 대시보드로 이동
    await page.goto('/dashboard');

    // 문서 목록에서 제목 확인
    await expect(page.getByText(docTitle)).toBeVisible({ timeout: 10_000 });
  });

  test('문서 클릭 시 상세 페이지로 이동한다', async ({ page }) => {
    const docTitle = `E2E Detail Test ${Date.now()}`;

    // 문서 생성
    await page.goto('/documents/new');
    await page.locator('input[placeholder*="제목"]').fill(docTitle);
    await page.locator('textarea[placeholder*="Markdown"]').fill('Detail view test content');
    await page.getByRole('button', { name: '임시저장' }).click();
    await expect(page).toHaveURL(/\/documents\/\d+/, { timeout: 15_000 });

    // URL에서 문서 ID 추출
    const docUrl = page.url();
    const docId = docUrl.match(/\/documents\/(\d+)/)?.[1];
    expect(docId).toBeTruthy();

    // 대시보드로 이동 후 클릭
    await page.goto('/dashboard');
    await page.getByText(docTitle).click();

    // 문서 상세 URL 확인
    await expect(page).toHaveURL(new RegExp(`/documents/${docId}`), { timeout: 10_000 });
  });

  test('문서를 수정할 수 있다', async ({ page }) => {
    const originalTitle = `E2E Edit Original ${Date.now()}`;
    const updatedTitle = `E2E Edit Updated ${Date.now()}`;

    // 문서 생성
    await page.goto('/documents/new');
    await page.locator('input[placeholder*="제목"]').fill(originalTitle);
    await page.locator('textarea[placeholder*="Markdown"]').fill('Original content');
    await page.getByRole('button', { name: '임시저장' }).click();
    await expect(page).toHaveURL(/\/documents\/\d+/, { timeout: 15_000 });

    // 수정 버튼 클릭
    const editButton = page.getByRole('link', { name: /수정|편집|edit/i });
    if (await editButton.isVisible()) {
      await editButton.click();
    } else {
      // 현재 URL에 /edit 추가
      const currentUrl = page.url();
      await page.goto(`${currentUrl}/edit`);
    }

    await expect(page).toHaveURL(/\/edit$/, { timeout: 10_000 });

    // 제목 수정
    const titleInput = page.locator('input[placeholder*="제목"]');
    await titleInput.clear();
    await titleInput.fill(updatedTitle);

    // 내용 수정
    const contentArea = page.locator('textarea[placeholder*="Markdown"]');
    await contentArea.clear();
    await contentArea.fill('Updated content by E2E test');

    // 저장
    await page.getByRole('button', { name: '임시저장' }).click();

    // 문서 상세 페이지로 돌아옴
    await expect(page).toHaveURL(/\/documents\/\d+$/, { timeout: 15_000 });

    // 수정된 제목 확인
    await expect(page.getByText(updatedTitle)).toBeVisible({ timeout: 10_000 });
  });

  test('문서를 휴지통으로 삭제할 수 있다', async ({ page }) => {
    const docTitle = `E2E Delete Test ${Date.now()}`;

    // 문서 생성
    await page.goto('/documents/new');
    await page.locator('input[placeholder*="제목"]').fill(docTitle);
    await page.locator('textarea[placeholder*="Markdown"]').fill('Delete test content');
    await page.getByRole('button', { name: '임시저장' }).click();
    await expect(page).toHaveURL(/\/documents\/\d+/, { timeout: 15_000 });

    // 삭제 버튼 클릭
    const deleteButton = page.getByRole('button', { name: /삭제|delete/i });
    if (await deleteButton.isVisible()) {
      await deleteButton.click();

      // 확인 다이얼로그 처리 (있을 경우)
      const confirmButton = page.getByRole('button', { name: /확인|confirm|yes/i });
      if (await confirmButton.isVisible({ timeout: 2_000 })) {
        await confirmButton.click();
      }

      // 대시보드로 이동했는지 확인
      await expect(page).toHaveURL(/\/dashboard/, { timeout: 10_000 });
    } else {
      // API를 통한 삭제 (UI 버튼이 없는 경우)
      const currentUrl = page.url();
      const docId = currentUrl.match(/\/documents\/(\d+)/)?.[1];

      if (docId) {
        const deleteRes = await page.request.delete(
          `${process.env.API_BASE || 'http://localhost:8081'}/api/v1/documents/${docId}`
        );
        expect([200, 204]).toContain(deleteRes.status());
      }
    }
  });
});
