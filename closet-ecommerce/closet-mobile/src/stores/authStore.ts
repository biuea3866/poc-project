import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Member } from '../types/member';
import { memberApi } from '../api/member';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: Member | null;
  isLoading: boolean;

  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  fetchUser: () => Promise<void>;
  setTokens: (accessToken: string, refreshToken: string) => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      isLoading: false,

      login: async (email: string, password: string) => {
        set({ isLoading: true });
        try {
          const response = await memberApi.login({ email, password });
          const { accessToken, refreshToken, member } = response.data.data;

          await AsyncStorage.setItem('accessToken', accessToken);
          await AsyncStorage.setItem('refreshToken', refreshToken);

          set({
            accessToken,
            refreshToken,
            user: member,
            isLoading: false,
          });
        } catch (error) {
          set({ isLoading: false });
          throw error;
        }
      },

      logout: async () => {
        await AsyncStorage.multiRemove(['accessToken', 'refreshToken']);
        set({
          accessToken: null,
          refreshToken: null,
          user: null,
        });
      },

      fetchUser: async () => {
        try {
          const response = await memberApi.getMyInfo();
          set({ user: response.data.data });
        } catch {
          await get().logout();
        }
      },

      setTokens: (accessToken: string, refreshToken: string) => {
        set({ accessToken, refreshToken });
      },
    }),
    {
      name: 'auth-storage',
      storage: createJSONStorage(() => AsyncStorage),
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        user: state.user,
      }),
    },
  ),
);
