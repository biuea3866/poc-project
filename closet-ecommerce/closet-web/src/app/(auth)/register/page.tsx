'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import RegisterForm from '@/components/auth/RegisterForm';
import { useAuth } from '@/hooks/useAuth';

export default function RegisterPage() {
  const router = useRouter();
  const { register } = useAuth();
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleRegister = async (data: { email: string; password: string; name: string; phone?: string }) => {
    setIsLoading(true);
    setError(null);
    try {
      await register(data);
      router.push('/login');
    } catch {
      setError('회원가입에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-[80vh] flex items-center justify-center px-4">
      <div className="w-full max-w-md">
        <h1 className="text-3xl font-bold text-center mb-8">회원가입</h1>
        <RegisterForm onSubmit={handleRegister} isLoading={isLoading} error={error} />
      </div>
    </div>
  );
}
