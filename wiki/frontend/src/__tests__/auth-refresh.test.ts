/**
 * refreshAccessToken() 단위 테스트
 *
 * AC 매핑:
 * - TC1: refresh 성공 시 새 access + refresh 토큰이 localStorage에 저장된다
 * - TC2: refresh 실패(서버 오류) 시 토큰이 클리어된다
 * - TC3: refresh 실패(네트워크 오류) 시 토큰이 클리어된다
 * - TC4: refreshToken이 localStorage에 없으면 false를 반환한다
 */

import { refreshAccessToken, persistTokens, clearTokens } from '@/lib/auth';

const mockFetch = jest.fn();
global.fetch = mockFetch;

function makeResponse(body: unknown, status = 200) {
  const text = typeof body === 'string' ? body : JSON.stringify(body);
  return {
    ok: status >= 200 && status < 300,
    status,
    json: jest.fn().mockResolvedValue(typeof body === 'object' ? body : {}),
    text: jest.fn().mockResolvedValue(text),
  } as unknown as Response;
}

describe('refreshAccessToken', () => {
  beforeEach(() => {
    mockFetch.mockClear();
    localStorage.clear();
  });

  // TC1: 재발급 성공 시 새 토큰 저장
  it('TC1 - 재발급 성공 시 새 access + refresh 토큰이 localStorage에 저장되고 true를 반환한다', async () => {
    localStorage.setItem('refreshToken', 'old-refresh-token');
    mockFetch.mockResolvedValue(
      makeResponse({ accessToken: 'new-access-token', refreshToken: 'new-refresh-token' }, 200)
    );

    const result = await refreshAccessToken();

    expect(result).toBe(true);
    expect(localStorage.getItem('accessToken')).toBe('new-access-token');
    expect(localStorage.getItem('refreshToken')).toBe('new-refresh-token');
  });

  // TC2: 재발급 실패(서버 오류) 시 토큰 클리어
  it('TC2 - 서버가 4xx/5xx를 반환하면 토큰이 클리어되고 false를 반환한다', async () => {
    localStorage.setItem('accessToken', 'old-access-token');
    localStorage.setItem('refreshToken', 'old-refresh-token');
    mockFetch.mockResolvedValue(makeResponse({ message: '탈취 감지' }, 401));

    const result = await refreshAccessToken();

    expect(result).toBe(false);
    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(localStorage.getItem('refreshToken')).toBeNull();
  });

  // TC3: 재발급 실패(네트워크 오류) 시 토큰 클리어
  it('TC3 - 네트워크 오류 시 토큰이 클리어되고 false를 반환한다', async () => {
    localStorage.setItem('accessToken', 'old-access-token');
    localStorage.setItem('refreshToken', 'old-refresh-token');
    mockFetch.mockRejectedValue(new Error('Network Error'));

    const result = await refreshAccessToken();

    expect(result).toBe(false);
    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(localStorage.getItem('refreshToken')).toBeNull();
  });

  // TC4: refreshToken 없으면 false 반환
  it('TC4 - localStorage에 refreshToken이 없으면 false를 반환하고 fetch를 호출하지 않는다', async () => {
    const result = await refreshAccessToken();

    expect(result).toBe(false);
    expect(mockFetch).not.toHaveBeenCalled();
  });

  // TC5: 재발급 요청 시 /api/v1/auth/refresh 엔드포인트를 호출한다
  it('TC5 - 재발급 요청 시 POST /api/v1/auth/refresh 를 호출하고 refreshToken을 body에 포함한다', async () => {
    localStorage.setItem('refreshToken', 'my-refresh-token');
    mockFetch.mockResolvedValue(
      makeResponse({ accessToken: 'new-at', refreshToken: 'new-rt' }, 200)
    );

    await refreshAccessToken();

    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/auth/refresh'),
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ refreshToken: 'my-refresh-token' }),
        headers: expect.objectContaining({ 'Content-Type': 'application/json' }),
      })
    );
  });

  // TC6: 재발급 성공 시 이전 토큰을 덮어쓴다 (rotation)
  it('TC6 - 재발급 성공 시 새 refresh 토큰이 기존 refresh 토큰을 덮어쓴다 (rotation)', async () => {
    localStorage.setItem('accessToken', 'old-access-token');
    localStorage.setItem('refreshToken', 'old-refresh-token');
    mockFetch.mockResolvedValue(
      makeResponse({ accessToken: 'rotated-access', refreshToken: 'rotated-refresh' }, 200)
    );

    await refreshAccessToken();

    expect(localStorage.getItem('accessToken')).toBe('rotated-access');
    expect(localStorage.getItem('refreshToken')).toBe('rotated-refresh');
    // 이전 토큰이 아닌 새 토큰으로 교체되었는지 확인
    expect(localStorage.getItem('refreshToken')).not.toBe('old-refresh-token');
  });
});
