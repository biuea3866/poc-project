import { create } from 'zustand';
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
  accessToken: typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null,
  refreshToken: typeof window !== 'undefined' ? localStorage.getItem('refreshToken') : null,
  memberId: typeof window !== 'undefined' ? (localStorage.getItem('memberId') ? Number(localStorage.getItem('memberId')) : null) : null,
  user: null,
  isAuthenticated: typeof window !== 'undefined' ? !!localStorage.getItem('accessToken') : false,

  login: (accessToken, refreshToken, memberId) => {
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    localStorage.setItem('memberId', String(memberId));
    set({ accessToken, refreshToken, memberId, isAuthenticated: true });
  },

  logout: () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('memberId');
    set({ accessToken: null, refreshToken: null, memberId: null, user: null, isAuthenticated: false });
  },

  setUser: (user) => set({ user }),
}));
