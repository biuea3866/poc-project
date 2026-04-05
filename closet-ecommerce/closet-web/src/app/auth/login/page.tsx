'use client';

import { useState, useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Suspense } from 'react';
import LoginForm from '@/components/auth/LoginForm';
import { useAuth } from '@/hooks/useAuth';
import { useAuthStore } from '@/stores/authStore';

function LoginContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { login } = useAuth();
  const { isAuthenticated } = useAuthStore();
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const redirectTo = searchParams.get('redirect') || '/';

  // Redirect if already logged in
  useEffect(() => {
    if (isAuthenticated) {
      router.push(redirectTo);
    }
  }, [isAuthenticated, router, redirectTo]);

  const handleLogin = async (email: string, password: string) => {
    setIsLoading(true);
    setError(null);
    try {
      await login({ email, password });
      router.push(redirectTo);
    } catch {
      setError('이메일 또는 비밀번호가 올바르지 않습니다. 다시 시도해주세요.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-[80vh] flex items-center justify-center px-4">
      <div className="w-full max-w-md">
        <h1 className="text-3xl font-bold text-center mb-8">로그인</h1>
        <LoginForm onSubmit={handleLogin} isLoading={isLoading} error={error} />
      </div>
    </div>
  );
}

export default function AuthLoginPage() {
  return (
    <Suspense fallback={
      <div className="min-h-[80vh] flex items-center justify-center px-4">
        <div className="w-full max-w-md">
          <h1 className="text-3xl font-bold text-center mb-8">로그인</h1>
          <div className="space-y-4 animate-pulse">
            <div className="h-10 bg-gray-200 rounded-lg" />
            <div className="h-10 bg-gray-200 rounded-lg" />
            <div className="h-12 bg-gray-200 rounded-lg" />
          </div>
        </div>
      </div>
    }>
      <LoginContent />
    </Suspense>
  );
}
