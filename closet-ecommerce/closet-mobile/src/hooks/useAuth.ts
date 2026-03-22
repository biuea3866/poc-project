import { useCallback } from 'react';
import { useAuthStore } from '../stores/authStore';

export const useAuth = () => {
  const { accessToken, user, isLoading, login, logout, fetchUser } = useAuthStore();

  const isAuthenticated = !!accessToken;

  const handleLogin = useCallback(
    async (email: string, password: string) => {
      await login(email, password);
    },
    [login],
  );

  const handleLogout = useCallback(async () => {
    await logout();
  }, [logout]);

  return {
    user,
    isAuthenticated,
    isLoading,
    login: handleLogin,
    logout: handleLogout,
    fetchUser,
  };
};
