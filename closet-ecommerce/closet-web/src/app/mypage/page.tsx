'use client';

import { useEffect, useState, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useAuth } from '@/hooks/useAuth';
import { useAuthStore } from '@/stores/authStore';
import { getOrders } from '@/lib/api/order';
import { formatPriceWithCurrency, formatDate } from '@/lib/utils/format';
import OrderStatusBadge from '@/components/order/OrderStatus';
import type { Order } from '@/types/order';

const GRADE_CONFIG: Record<string, { label: string; color: string }> = {
  NORMAL: { label: '일반', color: 'bg-gray-100 text-gray-800' },
  SILVER: { label: '실버', color: 'bg-gray-200 text-gray-800' },
  GOLD: { label: '골드', color: 'bg-yellow-100 text-yellow-800' },
  PLATINUM: { label: '플래티넘', color: 'bg-purple-100 text-purple-800' },
};

function ProfileSkeleton() {
  return (
    <div className="bg-gray-50 rounded-lg p-6 animate-pulse">
      <div className="flex items-center gap-4">
        <div className="w-14 h-14 sm:w-16 sm:h-16 bg-gray-200 rounded-full" />
        <div className="space-y-2 flex-1">
          <div className="h-5 bg-gray-200 rounded w-1/4" />
          <div className="h-4 bg-gray-200 rounded w-1/3" />
        </div>
      </div>
    </div>
  );
}

function OrdersSkeleton() {
  return (
    <div className="space-y-4">
      {[1, 2].map((i) => (
        <div key={i} className="border border-gray-200 rounded-lg p-4 animate-pulse">
          <div className="flex justify-between mb-3">
            <div className="h-4 bg-gray-200 rounded w-32" />
            <div className="h-5 bg-gray-200 rounded w-16" />
          </div>
          <div className="flex gap-3">
            <div className="w-14 h-14 bg-gray-200 rounded" />
            <div className="flex-1 space-y-2">
              <div className="h-3 bg-gray-200 rounded w-1/2" />
              <div className="h-3 bg-gray-200 rounded w-1/4" />
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

export default function MyPage() {
  const router = useRouter();
  const { isAuthenticated, logout } = useAuthStore();
  const { user, fetchMe } = useAuth();
  const [recentOrders, setRecentOrders] = useState<Order[]>([]);
  const [loadingProfile, setLoadingProfile] = useState(true);
  const [loadingOrders, setLoadingOrders] = useState(true);

  const loadProfile = useCallback(() => {
    return fetchMe()
      .catch(() => {})
      .finally(() => setLoadingProfile(false));
  }, [fetchMe]);

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/auth/login');
      return;
    }

    loadProfile();

    getOrders({ page: 0, size: 5 })
      .then((res) => {
        setRecentOrders(res.data.data?.content || []);
      })
      .catch(() => {})
      .finally(() => setLoadingOrders(false));
  }, [isAuthenticated, router, loadProfile]);

  const gradeConfig = GRADE_CONFIG[user?.grade || 'NORMAL'] || GRADE_CONFIG.NORMAL;

  const handleLogout = () => {
    logout();
    router.push('/');
  };

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-8">마이페이지</h1>

      {/* Profile Section */}
      {loadingProfile ? (
        <ProfileSkeleton />
      ) : user ? (
        <section className="bg-gray-50 rounded-lg p-4 sm:p-6 mb-6">
          <div className="flex items-center gap-3 sm:gap-4">
            {/* Avatar */}
            <div className="w-14 h-14 sm:w-16 sm:h-16 bg-black rounded-full flex items-center justify-center flex-shrink-0">
              <span className="text-xl sm:text-2xl font-bold text-white">
                {user.name.charAt(0).toUpperCase()}
              </span>
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 flex-wrap">
                <h2 className="text-base sm:text-lg font-bold text-gray-900">{user.name}</h2>
                <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${gradeConfig.color}`}>
                  {gradeConfig.label}
                </span>
              </div>
              <p className="text-sm text-gray-500 truncate">{user.email}</p>
              {user.phone && (
                <p className="text-sm text-gray-500">{user.phone}</p>
              )}
            </div>
            <div className="text-right flex-shrink-0">
              <p className="text-xs text-gray-500">적립금</p>
              <p className="text-base sm:text-lg font-bold text-gray-900">
                {formatPriceWithCurrency(user.pointBalance)}
              </p>
            </div>
          </div>
        </section>
      ) : (
        <section className="bg-gray-50 rounded-lg p-6 mb-6">
          <p className="text-sm text-gray-500">회원 정보를 불러올 수 없습니다.</p>
        </section>
      )}

      {/* Quick Links */}
      <section className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-8">
        <Link
          href="/orders"
          className="flex flex-col items-center gap-2 p-4 border border-gray-200 rounded-lg hover:border-gray-400 transition-colors"
        >
          <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
          </svg>
          <span className="text-sm font-medium">주문 내역</span>
        </Link>
        <Link
          href="/mypage/addresses"
          className="flex flex-col items-center gap-2 p-4 border border-gray-200 rounded-lg hover:border-gray-400 transition-colors"
        >
          <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
            <path strokeLinecap="round" strokeLinejoin="round" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
          <span className="text-sm font-medium">배송지 관리</span>
        </Link>
        <Link
          href="/cart"
          className="flex flex-col items-center gap-2 p-4 border border-gray-200 rounded-lg hover:border-gray-400 transition-colors"
        >
          <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 100 4 2 2 0 000-4z" />
          </svg>
          <span className="text-sm font-medium">장바구니</span>
        </Link>
        <button
          className="flex flex-col items-center gap-2 p-4 border border-gray-200 rounded-lg hover:border-gray-400 transition-colors cursor-not-allowed opacity-50"
          disabled
        >
          <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M15 5v2m0 4v2m0 4v2M5 5a2 2 0 00-2 2v3a2 2 0 110 4v3a2 2 0 002 2h14a2 2 0 002-2v-3a2 2 0 110-4V7a2 2 0 00-2-2H5z" />
          </svg>
          <span className="text-sm font-medium">쿠폰함</span>
        </button>
      </section>

      {/* Recent Orders */}
      <section>
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-lg font-bold text-gray-900">최근 주문</h2>
          <Link href="/orders" className="text-sm text-gray-500 hover:text-gray-900">
            전체보기
          </Link>
        </div>

        {loadingOrders ? (
          <OrdersSkeleton />
        ) : recentOrders.length === 0 ? (
          <div className="text-center py-12 text-gray-500 border border-gray-200 rounded-lg">
            <svg xmlns="http://www.w3.org/2000/svg" className="h-12 w-12 mx-auto text-gray-300 mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
            </svg>
            <p className="text-sm">최근 주문 내역이 없습니다.</p>
            <Link
              href="/products"
              className="inline-block mt-3 text-sm text-black font-medium hover:underline"
            >
              쇼핑하러 가기
            </Link>
          </div>
        ) : (
          <div className="space-y-3">
            {recentOrders.map((order) => (
              <Link
                key={order.id}
                href={`/orders/${order.id}`}
                className="block border border-gray-200 rounded-lg p-4 hover:border-gray-400 transition-colors"
              >
                <div className="flex justify-between items-center mb-3 gap-2">
                  <div className="flex items-center gap-2 min-w-0">
                    <span className="text-sm font-medium text-gray-900 truncate">
                      {order.orderNumber}
                    </span>
                    <span className="text-xs text-gray-500 flex-shrink-0">
                      {formatDate(order.orderedAt)}
                    </span>
                  </div>
                  <OrderStatusBadge status={order.status} />
                </div>
                <div className="flex items-center gap-3">
                  <div className="w-14 h-14 bg-gray-200 rounded flex items-center justify-center flex-shrink-0">
                    <span className="text-xs text-gray-400">
                      #{order.items[0]?.productId}
                    </span>
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm text-gray-900 truncate">
                      {order.items[0]?.productName}
                      {order.items.length > 1 && ` 외 ${order.items.length - 1}건`}
                    </p>
                    <p className="text-sm font-bold text-gray-900 mt-1">
                      {formatPriceWithCurrency(order.paymentAmount)}
                    </p>
                  </div>
                  {/* Shipping tracking link */}
                  {order.status === 'SHIPPING' && (
                    <span className="text-xs text-blue-600 font-medium flex-shrink-0 hidden sm:block">
                      배송조회
                    </span>
                  )}
                </div>
                {/* Mobile: shipping tracking */}
                {order.status === 'SHIPPING' && (
                  <div className="mt-2 pt-2 border-t border-gray-100 sm:hidden">
                    <span className="text-xs text-blue-600 font-medium">
                      배송 추적하기 &rarr;
                    </span>
                  </div>
                )}
              </Link>
            ))}
          </div>
        )}
      </section>

      {/* Logout button */}
      <div className="mt-12 pt-6 border-t border-gray-200">
        <button
          onClick={handleLogout}
          className="w-full py-3 border border-gray-300 rounded-lg text-sm text-gray-600 hover:bg-gray-50 transition-colors"
        >
          로그아웃
        </button>
      </div>
    </div>
  );
}
