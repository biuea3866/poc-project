import { create } from 'zustand';
import type { Member } from '@/types/member';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: Member | null;
  isAuthenticated: boolean;
  login: (accessToken: string, refreshToken: string, user: Member) => void;
  logout: () => void;
  setUser: (user: Member) => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null,
  refreshToken: typeof window !== 'undefined' ? localStorage.getItem('refreshToken') : null,
  user: null,
  isAuthenticated: typeof window !== 'undefined' ? !!localStorage.getItem('accessToken') : false,

  login: (accessToken, refreshToken, user) => {
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    set({ accessToken, refreshToken, user, isAuthenticated: true });
  },

  logout: () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    set({ accessToken: null, refreshToken: null, user: null, isAuthenticated: false });
  },

  setUser: (user) => set({ user }),
}));
