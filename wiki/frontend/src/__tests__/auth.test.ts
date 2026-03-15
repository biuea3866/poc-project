import { persistTokens, clearTokens, getAccessToken } from '@/lib/auth';

describe('auth utilities', () => {
  beforeEach(() => {
    localStorage.clear();
    jest.clearAllMocks();
  });

  describe('persistTokens', () => {
    it('accessToken과 refreshToken을 localStorage에 저장한다', () => {
      persistTokens({ accessToken: 'at-123', refreshToken: 'rt-456' });

      expect(localStorage.getItem('accessToken')).toBe('at-123');
      expect(localStorage.getItem('refreshToken')).toBe('rt-456');
    });

    it('data 중첩 구조에서도 토큰을 저장한다', () => {
      persistTokens({ data: { accessToken: 'at-nested', refreshToken: 'rt-nested' } });

      expect(localStorage.getItem('accessToken')).toBe('at-nested');
      expect(localStorage.getItem('refreshToken')).toBe('rt-nested');
    });

    it('accessToken이 없으면 저장하지 않는다', () => {
      persistTokens({});

      expect(localStorage.getItem('accessToken')).toBeNull();
      expect(localStorage.getItem('refreshToken')).toBeNull();
    });

    it('accessToken만 있는 경우 accessToken만 저장한다', () => {
      persistTokens({ accessToken: 'at-only' });

      expect(localStorage.getItem('accessToken')).toBe('at-only');
      expect(localStorage.getItem('refreshToken')).toBeNull();
    });
  });

  describe('clearTokens', () => {
    it('localStorage에서 토큰을 삭제한다', () => {
      localStorage.setItem('accessToken', 'at-123');
      localStorage.setItem('refreshToken', 'rt-456');

      clearTokens();

      expect(localStorage.getItem('accessToken')).toBeNull();
      expect(localStorage.getItem('refreshToken')).toBeNull();
    });
  });

  describe('getAccessToken', () => {
    it('localStorage에서 accessToken을 반환한다', () => {
      localStorage.setItem('accessToken', 'at-123');

      expect(getAccessToken()).toBe('at-123');
    });

    it('토큰이 없으면 null을 반환한다', () => {
      expect(getAccessToken()).toBeNull();
    });
  });
});
