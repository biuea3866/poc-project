'use client';

import { useAuthStore } from '@/stores/authStore';
import * as memberApi from '@/lib/api/member';
import type { LoginRequest, RegisterRequest } from '@/types/member';

export function useAuth() {
  const { user, isAuthenticated, login: storeLogin, logout: storeLogout, setUser } = useAuthStore();

  const login = async (data: LoginRequest) => {
    const response = await memberApi.login(data);
    const loginData = response.data.data;
    if (!loginData) throw new Error('Login failed');
    const { accessToken, refreshToken, memberId } = loginData;
    storeLogin(accessToken, refreshToken, memberId);
    return memberId;
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
    const member = response.data.data;
    if (member) {
      setUser(member);
    }
    return member;
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
