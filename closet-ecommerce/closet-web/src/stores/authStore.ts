import { create } from 'zustand';
import { tokenManager } from '@/lib/auth/token';
import type { Member } from '@/types/member';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  memberId: number | null;
  user: Member | null;
  isAuthenticated: boolean;
  login: (accessToken: string, refreshToken: string, memberId: number) => void;
  logout: () => void;
  setUser: (user: Member) => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: tokenManager.getAccessToken(),
  refreshToken: tokenManager.getRefreshToken(),
  memberId: tokenManager.getMemberId(),
  user: null,
  isAuthenticated: tokenManager.isAuthenticated(),

  login: (accessToken, refreshToken, memberId) => {
    tokenManager.setTokens(accessToken, refreshToken, memberId);
    set({ accessToken, refreshToken, memberId, isAuthenticated: true });
  },

  logout: () => {
    tokenManager.clearTokens();
    set({
      accessToken: null,
      refreshToken: null,
      memberId: null,
      user: null,
      isAuthenticated: false,
    });
  },

  setUser: (user) => set({ user }),
}));
