'use client';

import Link from 'next/link';

export default function CartPage() {
  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-8">장바구니</h1>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Cart Items */}
        <div className="lg:col-span-2">
          <div className="text-center py-16 text-gray-500">
            <p className="text-lg mb-4">장바구니가 비어있습니다</p>
            <Link
              href="/products"
              className="inline-block bg-black text-white px-6 py-2 rounded-lg font-medium hover:bg-gray-800 transition-colors"
            >
              쇼핑 계속하기
            </Link>
          </div>
        </div>

        {/* Order Summary */}
        <div>
          <div className="bg-gray-50 rounded-lg p-6">
            <h3 className="text-lg font-bold mb-4">주문 요약</h3>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-600">상품 금액 (0개)</span>
                <span>₩0</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">배송비</span>
                <span>₩0</span>
              </div>
              <div className="border-t border-gray-200 pt-2 mt-2">
                <div className="flex justify-between font-bold text-base">
                  <span>합계</span>
                  <span>₩0</span>
                </div>
              </div>
            </div>
            <button
              disabled
              className="w-full mt-4 bg-gray-300 text-white py-3 rounded-lg font-medium cursor-not-allowed"
            >
              주문하기
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
