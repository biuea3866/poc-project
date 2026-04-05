'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';

/**
 * Legacy route: /login -> redirects to /auth/login
 */
export default function LoginPage() {
  const router = useRouter();

  useEffect(() => {
    router.replace('/auth/login');
  }, [router]);

  return (
    <div className="min-h-[80vh] flex items-center justify-center">
      <p className="text-gray-500 text-sm">리다이렉트 중...</p>
    </div>
  );
}
