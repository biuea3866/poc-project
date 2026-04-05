'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { getOrders } from '@/lib/api/order';
import { useAuthStore } from '@/stores/authStore';
import { formatPriceWithCurrency, formatDate } from '@/lib/utils/format';
import OrderStatusBadge from '@/components/order/OrderStatus';
import type { Order } from '@/types/order';
import type { PageResponse } from '@/types/common';

const EMPTY_PAGE: PageResponse<Order> = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  pageable: { pageNumber: 0, pageSize: 10 },
};

function OrderSkeleton() {
  return (
    <div className="border border-gray-200 rounded-lg p-6 animate-pulse">
      <div className="flex justify-between items-center mb-4">
        <div className="h-5 bg-gray-200 rounded w-40" />
        <div className="h-6 bg-gray-200 rounded w-20" />
      </div>
      <div className="flex gap-4">
        <div className="w-20 h-20 bg-gray-200 rounded" />
        <div className="flex-1 space-y-2">
          <div className="h-4 bg-gray-200 rounded w-2/3" />
          <div className="h-3 bg-gray-200 rounded w-1/3" />
        </div>
        <div className="h-5 bg-gray-200 rounded w-24" />
      </div>
    </div>
  );
}

export default function OrdersPage() {
  const router = useRouter();
  const { isAuthenticated } = useAuthStore();
  const [orders, setOrders] = useState<PageResponse<Order>>(EMPTY_PAGE);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/auth/login');
      return;
    }

    setLoading(true);
    getOrders({ page, size: 10 })
      .then((res) => {
        setOrders(res.data.data || EMPTY_PAGE);
      })
      .catch(() => {
        setOrders(EMPTY_PAGE);
      })
      .finally(() => setLoading(false));
  }, [isAuthenticated, page, router]);

  const handlePageChange = (newPage: number) => {
    setPage(newPage);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-8">주문 내역</h1>

      {loading ? (
        <div className="space-y-4">
          {[1, 2, 3].map((i) => (
            <OrderSkeleton key={i} />
          ))}
        </div>
      ) : orders.content.length === 0 ? (
        <div className="text-center py-16 text-gray-500">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            className="h-16 w-16 mx-auto text-gray-300 mb-4"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth={1}
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"
            />
          </svg>
          <p className="text-lg">주문 내역이 없습니다.</p>
          <p className="text-sm mt-2">주문하신 내역이 여기에 표시됩니다.</p>
          <Link
            href="/products"
            className="inline-block mt-4 bg-black text-white px-6 py-2 rounded-lg font-medium hover:bg-gray-800 transition-colors"
          >
            쇼핑하기
          </Link>
        </div>
      ) : (
        <div className="space-y-4">
          {orders.content.map((order) => (
            <Link
              key={order.id}
              href={`/orders/${order.id}`}
              className="block border border-gray-200 rounded-lg p-6 hover:border-gray-400 transition-colors"
            >
              {/* Order Header */}
              <div className="flex justify-between items-center mb-4">
                <div>
                  <span className="text-sm font-medium text-gray-900">
                    {order.orderNumber}
                  </span>
                  <span className="text-sm text-gray-500 ml-3">
                    {formatDate(order.orderedAt)}
                  </span>
                </div>
                <OrderStatusBadge status={order.status} />
              </div>

              {/* Order Items Preview */}
              <div className="space-y-3">
                {order.items.slice(0, 2).map((item) => (
                  <div key={item.id} className="flex items-center gap-4">
                    <div className="w-16 h-16 bg-gray-200 rounded flex items-center justify-center flex-shrink-0">
                      <span className="text-xs text-gray-400">#{item.productId}</span>
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-gray-900 truncate">
                        {item.productName}
                      </p>
                      <p className="text-xs text-gray-500">
                        {item.optionName} / {item.quantity}개
                      </p>
                    </div>
                    <span className="text-sm font-bold text-gray-900 flex-shrink-0">
                      {formatPriceWithCurrency(item.totalPrice)}
                    </span>
                  </div>
                ))}
                {order.items.length > 2 && (
                  <p className="text-xs text-gray-500">
                    외 {order.items.length - 2}건
                  </p>
                )}
              </div>

              {/* Order Total */}
              <div className="flex justify-between items-center mt-4 pt-4 border-t border-gray-100">
                <span className="text-sm text-gray-600">결제 금액</span>
                <span className="text-base font-bold text-gray-900">
                  {formatPriceWithCurrency(order.paymentAmount)}
                </span>
              </div>
            </Link>
          ))}
        </div>
      )}

      {/* Pagination */}
      {orders.totalPages > 1 && (
        <div className="flex justify-center items-center gap-2 mt-8">
          <button
            disabled={page === 0}
            onClick={() => handlePageChange(page - 1)}
            className="px-3 py-2 text-sm border border-gray-300 rounded-lg disabled:opacity-30 disabled:cursor-not-allowed hover:bg-gray-50"
          >
            이전
          </button>
          {Array.from({ length: orders.totalPages }).map((_, i) => (
            <button
              key={i}
              onClick={() => handlePageChange(i)}
              className={`w-10 h-10 text-sm rounded-lg ${
                page === i
                  ? 'bg-black text-white'
                  : 'border border-gray-300 hover:bg-gray-50'
              }`}
            >
              {i + 1}
            </button>
          ))}
          <button
            disabled={page >= orders.totalPages - 1}
            onClick={() => handlePageChange(page + 1)}
            className="px-3 py-2 text-sm border border-gray-300 rounded-lg disabled:opacity-30 disabled:cursor-not-allowed hover:bg-gray-50"
          >
            다음
          </button>
        </div>
      )}
    </div>
  );
}
