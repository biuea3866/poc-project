import { apiFetch } from '@/lib/api';

// auth 모듈 전체를 모킹하여 refreshAccessToken 동작을 제어
jest.mock('@/lib/auth', () => ({
  refreshAccessToken: jest.fn(),
  clearTokens: jest.fn(),
  persistTokens: jest.fn(),
  getAccessToken: jest.fn(),
}));

import { refreshAccessToken, clearTokens } from '@/lib/auth';

const mockFetch = jest.fn();
global.fetch = mockFetch;

// window.location.href 설정을 위한 mock
const mockLocation = { href: '' };
Object.defineProperty(window, 'location', {
  value: mockLocation,
  writable: true,
});

function makeResponse(body: unknown, status = 200) {
  const text = typeof body === 'string' ? body : JSON.stringify(body);
  return {
    ok: status >= 200 && status < 300,
    status,
    text: jest.fn().mockResolvedValue(text),
  } as unknown as Response;
}

describe('apiFetch', () => {
  beforeEach(() => {
    mockFetch.mockClear();
    localStorage.clear();
    (refreshAccessToken as jest.Mock).mockReset();
    (clearTokens as jest.Mock).mockReset();
    mockLocation.href = '';
  });

  it('성공 응답의 JSON을 반환한다', async () => {
    mockFetch.mockResolvedValue(makeResponse({ id: 1, title: 'test' }));

    const result = await apiFetch<{ id: number; title: string }>('/api/v1/documents');

    expect(result).toEqual({ id: 1, title: 'test' });
    expect(mockFetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/v1/documents'),
      expect.objectContaining({ headers: expect.anything() }),
    );
  });

  it('Content-Type: application/json 헤더를 설정한다', async () => {
    mockFetch.mockResolvedValue(makeResponse({}));

    await apiFetch('/api/v1/test');

    const headers = mockFetch.mock.calls[0][1].headers as Headers;
    expect(headers.get('Content-Type')).toBe('application/json');
  });

  it('localStorage에 accessToken이 있으면 Authorization 헤더를 설정한다', async () => {
    localStorage.setItem('accessToken', 'bearer-token-123');
    mockFetch.mockResolvedValue(makeResponse({}));

    await apiFetch('/api/v1/test');

    const headers = mockFetch.mock.calls[0][1].headers as Headers;
    expect(headers.get('Authorization')).toBe('Bearer bearer-token-123');
  });

  it('5xx 응답이면 에러를 던진다', async () => {
    mockFetch.mockResolvedValue(makeResponse({ message: '서버 오류' }, 500));

    await expect(apiFetch('/api/v1/error')).rejects.toThrow('서버 오류');
  });

  it('빈 응답 바디이면 null을 반환한다', async () => {
    mockFetch.mockResolvedValue(makeResponse(''));

    const result = await apiFetch('/api/v1/empty');

    expect(result).toBeNull();
  });

  describe('401 자동 재발급 인터셉터', () => {
    it('401 응답 시 refreshAccessToken을 호출한다', async () => {
      (refreshAccessToken as jest.Mock).mockResolvedValue(false);
      mockFetch.mockResolvedValue(makeResponse({ message: '인증이 필요합니다' }, 401));

      await expect(apiFetch('/api/v1/protected')).rejects.toThrow();
      expect(refreshAccessToken).toHaveBeenCalledTimes(1);
    });

    it('401 응답 + 재발급 성공 시 요청을 재시도한다', async () => {
      (refreshAccessToken as jest.Mock).mockResolvedValue(true);
      // 첫 번째 호출: 401, 두 번째 호출: 200
      mockFetch
        .mockResolvedValueOnce(makeResponse({ message: '인증이 필요합니다' }, 401))
        .mockResolvedValueOnce(makeResponse({ id: 1 }));

      const result = await apiFetch<{ id: number }>('/api/v1/protected');

      expect(result).toEqual({ id: 1 });
      expect(mockFetch).toHaveBeenCalledTimes(2);
    });

    it('401 응답 + 재발급 실패 시 clearTokens를 호출하고 에러를 던진다', async () => {
      (refreshAccessToken as jest.Mock).mockResolvedValue(false);
      mockFetch.mockResolvedValue(makeResponse({ message: '인증 실패' }, 401));

      await expect(apiFetch('/api/v1/protected')).rejects.toThrow();
      expect(clearTokens).toHaveBeenCalledTimes(1);
    });

    it('401 응답 + 재발급 실패 시 /login 으로 이동한다', async () => {
      (refreshAccessToken as jest.Mock).mockResolvedValue(false);
      mockFetch.mockResolvedValue(makeResponse({ message: '인증 실패' }, 401));

      await expect(apiFetch('/api/v1/protected')).rejects.toThrow();
      expect(mockLocation.href).toBe('/login');
    });

    it('isRetry=true 이면 401에서 재발급을 시도하지 않는다 (무한루프 방지)', async () => {
      mockFetch.mockResolvedValue(makeResponse({ message: '인증이 필요합니다' }, 401));

      await expect(apiFetch('/api/v1/protected', {}, true)).rejects.toThrow();
      // isRetry=true 이므로 refreshAccessToken 호출 없음
      expect(refreshAccessToken).not.toHaveBeenCalled();
    });
  });
});
