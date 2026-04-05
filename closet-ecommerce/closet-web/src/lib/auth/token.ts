const ACCESS_TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';
const MEMBER_ID_KEY = 'memberId';

/**
 * JWT Token management utilities.
 * Provides a centralized interface for reading/writing auth tokens
 * from localStorage.
 */
export const tokenManager = {
  getAccessToken(): string | null {
    if (typeof window === 'undefined') return null;
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  },

  getRefreshToken(): string | null {
    if (typeof window === 'undefined') return null;
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  },

  getMemberId(): number | null {
    if (typeof window === 'undefined') return null;
    const val = localStorage.getItem(MEMBER_ID_KEY);
    return val ? Number(val) : null;
  },

  setTokens(accessToken: string, refreshToken: string, memberId: number): void {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    localStorage.setItem(MEMBER_ID_KEY, String(memberId));
  },

  clearTokens(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(MEMBER_ID_KEY);
  },

  isAuthenticated(): boolean {
    return !!this.getAccessToken();
  },

  /**
   * Decode JWT payload without verification (client-side only).
   * Returns null if the token is invalid or missing.
   */
  decodePayload(token: string): Record<string, unknown> | null {
    try {
      const base64Url = token.split('.')[1];
      if (!base64Url) return null;
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join(''),
      );
      return JSON.parse(jsonPayload);
    } catch {
      return null;
    }
  },

  /**
   * Check if the current access token is expired.
   */
  isTokenExpired(): boolean {
    const token = this.getAccessToken();
    if (!token) return true;
    const payload = this.decodePayload(token);
    if (!payload || typeof payload.exp !== 'number') return true;
    // Add 10 seconds buffer
    return Date.now() >= (payload.exp - 10) * 1000;
  },
};
