'use client';

import { useAuthStore } from '@/stores/authStore';
import * as memberApi from '@/lib/api/member';
import type { LoginRequest, RegisterRequest } from '@/types/member';

export function useAuth() {
  const { user, isAuthenticated, login: storeLogin, logout: storeLogout, setUser } = useAuthStore();

  const login = async (data: LoginRequest) => {
    const response = await memberApi.login(data);
    const { accessToken, refreshToken, member } = response.data.data;
    storeLogin(accessToken, refreshToken, member);
    return member;
  };

  const register = async (data: RegisterRequest) => {
    const response = await memberApi.register(data);
    return response.data.data;
  };

  const logout = () => {
    storeLogout();
  };

  const fetchMe = async () => {
    const response = await memberApi.getMe();
    setUser(response.data.data);
    return response.data.data;
  };

  return {
    user,
    isAuthenticated,
    login,
    register,
    logout,
    fetchMe,
  };
}
