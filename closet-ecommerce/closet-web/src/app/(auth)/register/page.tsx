'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';

/**
 * Legacy route: /register -> redirects to /auth/register
 */
export default function RegisterPage() {
  const router = useRouter();

  useEffect(() => {
    router.replace('/auth/register');
  }, [router]);

  return (
    <div className="min-h-[80vh] flex items-center justify-center">
      <p className="text-gray-500 text-sm">리다이렉트 중...</p>
    </div>
  );
}
