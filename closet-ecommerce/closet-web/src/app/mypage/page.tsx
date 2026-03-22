'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { getMe, getAddresses } from '@/lib/api/member';
import { getOrders } from '@/lib/api/order';
import { formatPriceWithCurrency, formatDate } from '@/lib/utils/format';
import type { Member, ShippingAddress } from '@/types/member';
import type { Order } from '@/types/order';

const GRADE_LABEL: Record<string, string> = {
  NORMAL: '일반',
  SILVER: '실버',
  GOLD: '골드',
  PLATINUM: '플래티넘',
};

export default function MyPage() {
  const [member, setMember] = useState<Member | null>(null);
  const [recentOrders, setRecentOrders] = useState<Order[]>([]);
  const [addressCount, setAddressCount] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      getMe().catch(() => null),
      getOrders({ page: 0, size: 3 }).catch(() => null),
      getAddresses().catch(() => null),
    ])
      .then(([memberRes, ordersRes, addressesRes]) => {
        if (memberRes?.data?.data) {
          setMember(memberRes.data.data);
        }
        if (ordersRes?.data?.data) {
          setRecentOrders(ordersRes.data.data.content || []);
        }
        if (addressesRes?.data?.data) {
          setAddressCount(addressesRes.data.data.length);
        }
      })
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-8">마이페이지</h1>
        <div className="bg-gray-50 rounded-lg p-6 mb-6 animate-pulse">
          <div className="h-4 bg-gray-200 rounded w-1/3 mb-3" />
          <div className="h-4 bg-gray-200 rounded w-1/2 mb-3" />
          <div className="h-4 bg-gray-200 rounded w-1/4" />
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-8">마이페이지</h1>

      {/* Profile Section */}
      <div className="bg-gray-50 rounded-lg p-6 mb-6">
        <h2 className="text-lg font-bold mb-4">내 정보</h2>
        {member ? (
          <div className="space-y-2">
            <div className="flex justify-between">
              <span className="text-sm text-gray-600">이름</span>
              <span className="text-sm font-medium text-gray-900">{member.name}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-sm text-gray-600">이메일</span>
              <span className="text-sm font-medium text-gray-900">{member.email}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-sm text-gray-600">등급</span>
              <span className="text-sm font-medium text-gray-900">
                {GRADE_LABEL[member.grade] || member.grade}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-sm text-gray-600">포인트</span>
              <span className="text-sm font-bold text-blue-600">
                {formatPriceWithCurrency(member.pointBalance)}
              </span>
            </div>
          </div>
        ) : (
          <p className="text-sm text-gray-500">정보를 불러올 수 없습니다.</p>
        )}
      </div>

      {/* Recent Orders */}
      <div className="mb-6">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-lg font-bold">최근 주문</h2>
          <Link href="/orders" className="text-sm text-blue-600 hover:underline">
            전체보기
          </Link>
        </div>
        {recentOrders.length === 0 ? (
          <div className="border border-gray-200 rounded-lg p-6 text-center text-gray-500">
            <p className="text-sm">최근 주문 내역이 없습니다.</p>
          </div>
        ) : (
          <div className="space-y-3">
            {recentOrders.map((order) => (
              <Link
                key={order.id}
                href={`/orders/${order.id}`}
                className="block border border-gray-200 rounded-lg p-4 hover:border-gray-400 transition-colors"
              >
                <div className="flex justify-between items-center">
                  <div>
                    <p className="text-xs text-gray-500">{formatDate(order.orderedAt)}</p>
                    <p className="text-sm font-medium text-gray-900 mt-0.5">
                      {order.orderNumber}
                    </p>
                  </div>
                  <p className="text-sm font-bold text-gray-900">
                    {formatPriceWithCurrency(order.paymentAmount)}
                  </p>
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>

      {/* Quick Links */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <Link
          href="/orders"
          className="block p-6 border border-gray-200 rounded-lg hover:border-gray-400 transition-colors"
        >
          <h3 className="font-bold mb-1">주문 내역</h3>
          <p className="text-sm text-gray-500">주문하신 내역을 확인하세요</p>
        </Link>
        <Link
          href="/mypage/addresses"
          className="block p-6 border border-gray-200 rounded-lg hover:border-gray-400 transition-colors"
        >
          <h3 className="font-bold mb-1">배송지 관리</h3>
          <p className="text-sm text-gray-500">
            {addressCount > 0 ? `${addressCount}개의 배송지` : '배송지를 관리하세요'}
          </p>
        </Link>
      </div>
    </div>
  );
}
