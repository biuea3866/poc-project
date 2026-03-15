import { apiFetch } from '@/lib/api';

const mockFetch = jest.fn();
global.fetch = mockFetch;

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

  it('4xx 응답이면 에러를 던진다', async () => {
    mockFetch.mockResolvedValue(makeResponse({ message: '인증이 필요합니다' }, 401));

    await expect(apiFetch('/api/v1/protected')).rejects.toThrow('인증이 필요합니다');
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
});
