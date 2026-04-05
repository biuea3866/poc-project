'use client';

import { useState } from 'react';
import { createRestockNotification } from '@/lib/api/claim';
import { useAuthStore } from '@/stores/authStore';
import type { ProductOption } from '@/types/product';

interface RestockNotificationProps {
  productId: number;
  option: ProductOption;
}

export default function RestockNotification({
  productId,
  option,
}: RestockNotificationProps) {
  const { isAuthenticated } = useAuthStore();
  const [registered, setRegistered] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleRegister = async () => {
    if (!isAuthenticated) {
      alert('로그인이 필요합니다.');
      return;
    }
    setLoading(true);
    try {
      await createRestockNotification({
        productId,
        productOptionId: option.id,
      });
      setRegistered(true);
    } catch {
      alert('재입고 알림 등록에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  if (registered) {
    return (
      <div className="flex items-center gap-2 text-sm text-green-600">
        <svg
          className="h-4 w-4"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          strokeWidth={2}
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            d="M5 13l4 4L19 7"
          />
        </svg>
        재입고 알림이 등록되었습니다
      </div>
    );
  }

  return (
    <button
      onClick={handleRegister}
      disabled={loading}
      className="inline-flex items-center gap-1.5 px-4 py-2 text-sm border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 disabled:opacity-50 transition-colors"
    >
      <svg
        className="h-4 w-4"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
        strokeWidth={2}
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
        />
      </svg>
      {loading ? '등록 중...' : `${option.colorName} / ${option.size} 재입고 알림`}
    </button>
  );
}
