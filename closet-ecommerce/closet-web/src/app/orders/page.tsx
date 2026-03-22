'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { getOrders } from '@/lib/api/order';
import { formatPriceWithCurrency, formatDateTime } from '@/lib/utils/format';
import type { Order } from '@/types/order';
import type { PageResponse } from '@/types/common';

const ORDER_STATUS_LABEL: Record<string, string> = {
  CREATED: '주문 생성',
  PAID: '결제 완료',
  PREPARING: '상품 준비중',
  SHIPPED: '배송중',
  DELIVERED: '배송 완료',
  CANCELLED: '주문 취소',
  REFUNDED: '환불 완료',
};

const EMPTY_PAGE: PageResponse<Order> = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  pageable: { pageNumber: 0, pageSize: 10 },
};

export default function OrdersPage() {
  const [orders, setOrders] = useState<PageResponse<Order>>(EMPTY_PAGE);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);

  useEffect(() => {
    setLoading(true);
    getOrders({ page, size: 10 })
      .then((res) => {
        setOrders(res.data.data || EMPTY_PAGE);
      })
      .catch(() => {
        setOrders(EMPTY_PAGE);
      })
      .finally(() => {
        setLoading(false);
      });
  }, [page]);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-8">주문 내역</h1>

      {loading ? (
        <div className="space-y-4">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="border border-gray-200 rounded-lg p-6 animate-pulse">
              <div className="h-4 bg-gray-200 rounded w-1/3 mb-3" />
              <div className="h-3 bg-gray-200 rounded w-1/2 mb-2" />
              <div className="h-3 bg-gray-200 rounded w-1/4" />
            </div>
          ))}
        </div>
      ) : orders.content.length === 0 ? (
        <div className="text-center py-16 text-gray-500">
          <p className="text-lg">주문 내역이 없습니다.</p>
          <p className="text-sm mt-2">주문하신 내역이 여기에 표시됩니다.</p>
          <Link
            href="/products"
            className="inline-block mt-4 bg-black text-white px-6 py-2 rounded-lg font-medium hover:bg-gray-800 transition-colors"
          >
            쇼핑하러 가기
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
              <div className="flex justify-between items-start mb-3">
                <div>
                  <p className="text-sm text-gray-500">{formatDateTime(order.orderedAt)}</p>
                  <p className="font-medium text-gray-900 mt-1">주문번호: {order.orderNumber}</p>
                </div>
                <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
                  {ORDER_STATUS_LABEL[order.status] || order.status}
                </span>
              </div>
              <div className="flex justify-between items-end">
                <p className="text-sm text-gray-600">
                  {order.items?.length > 0
                    ? `${order.items[0].productName}${order.items.length > 1 ? ` 외 ${order.items.length - 1}건` : ''}`
                    : '상품 정보'}
                </p>
                <p className="text-lg font-bold text-gray-900">
                  {formatPriceWithCurrency(order.paymentAmount)}
                </p>
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
            onClick={() => setPage((p) => p - 1)}
            className="px-3 py-2 text-sm border border-gray-300 rounded-lg disabled:opacity-30 disabled:cursor-not-allowed hover:bg-gray-50"
          >
            이전
          </button>
          {Array.from({ length: orders.totalPages }).map((_, i) => (
            <button
              key={i}
              onClick={() => setPage(i)}
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
            onClick={() => setPage((p) => p + 1)}
            className="px-3 py-2 text-sm border border-gray-300 rounded-lg disabled:opacity-30 disabled:cursor-not-allowed hover:bg-gray-50"
          >
            다음
          </button>
        </div>
      )}
    </div>
  );
}
