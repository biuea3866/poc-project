'use client';

import Link from 'next/link';

export default function MyPage() {
  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-8">마이페이지</h1>

      {/* Profile Section */}
      <div className="bg-gray-50 rounded-lg p-6 mb-6">
        <h2 className="text-lg font-bold mb-4">내 정보</h2>
        <div className="space-y-2 animate-pulse">
          <div className="h-4 bg-gray-200 rounded w-1/3" />
          <div className="h-4 bg-gray-200 rounded w-1/2" />
          <div className="h-4 bg-gray-200 rounded w-1/4" />
        </div>
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
          <p className="text-sm text-gray-500">배송지를 관리하세요</p>
        </Link>
      </div>
    </div>
  );
}
