'use client';

import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { getOrder, cancelOrder } from '@/lib/api/order';
import { useAuthStore } from '@/stores/authStore';
import { formatPriceWithCurrency, formatDateTime } from '@/lib/utils/format';
import OrderStatusBadge from '@/components/order/OrderStatus';
import type { Order } from '@/types/order';

function OrderDetailSkeleton() {
  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-8">주문 상세</h1>
      <div className="space-y-6">
        <div className="bg-gray-50 rounded-lg p-6">
          <div className="flex justify-between items-center mb-4">
            <div className="h-5 bg-gray-200 rounded w-1/3 animate-pulse" />
            <div className="h-6 bg-gray-200 rounded w-20 animate-pulse" />
          </div>
          <div className="h-4 bg-gray-200 rounded w-1/4 animate-pulse" />
        </div>
        <div>
          <h2 className="text-lg font-bold mb-4">주문 상품</h2>
          <div className="space-y-4">
            {[1, 2].map((i) => (
              <div key={i} className="flex gap-4 py-4 border-b border-gray-200 animate-pulse">
                <div className="w-20 h-20 bg-gray-200 rounded" />
                <div className="flex-1 space-y-2">
                  <div className="h-4 bg-gray-200 rounded w-1/2" />
                  <div className="h-3 bg-gray-200 rounded w-1/4" />
                </div>
                <div className="h-4 bg-gray-200 rounded w-20" />
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

export default function OrderDetailPage() {
  const params = useParams();
  const router = useRouter();
  const orderId = Number(params.id);
  const { isAuthenticated } = useAuthStore();

  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [cancelling, setCancelling] = useState(false);

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/auth/login');
      return;
    }
    if (!orderId) return;

    setLoading(true);
    getOrder(orderId)
      .then((res) => {
        setOrder(res.data.data);
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  }, [orderId, isAuthenticated, router]);

  const handleCancel = async () => {
    if (!order) return;
    if (!confirm('주문을 취소하시겠습니까?')) return;

    setCancelling(true);
    try {
      const res = await cancelOrder(order.id);
      if (res.data.data) {
        setOrder(res.data.data);
      }
    } catch {
      alert('주문 취소에 실패했습니다.');
    } finally {
      setCancelling(false);
    }
  };

  if (loading) return <OrderDetailSkeleton />;

  if (error || !order) {
    return (
      <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-16 text-center">
        <p className="text-lg text-gray-500">주문을 찾을 수 없습니다.</p>
        <button
          onClick={() => router.push('/orders')}
          className="mt-4 text-sm text-black underline"
        >
          주문 내역으로 돌아가기
        </button>
      </div>
    );
  }

  const canCancel = ['PENDING', 'PAID'].includes(order.status);

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center gap-4 mb-8">
        <button
          onClick={() => router.push('/orders')}
          className="text-gray-500 hover:text-gray-900"
        >
          <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        <h1 className="text-2xl font-bold text-gray-900">주문 상세</h1>
      </div>

      <div className="space-y-6">
        {/* Order Summary */}
        <section className="bg-gray-50 rounded-lg p-6">
          <div className="flex justify-between items-start">
            <div>
              <p className="text-sm text-gray-500">주문번호</p>
              <p className="text-lg font-bold text-gray-900">{order.orderNumber}</p>
              <p className="text-sm text-gray-500 mt-1">
                {formatDateTime(order.orderedAt)}
              </p>
            </div>
            <OrderStatusBadge status={order.status} />
          </div>
        </section>

        {/* Order Items */}
        <section>
          <h2 className="text-lg font-bold mb-4">주문 상품</h2>
          <div className="border border-gray-200 rounded-lg divide-y divide-gray-200">
            {order.items.map((item) => (
              <div key={item.id} className="flex gap-4 p-4">
                <Link href={`/products/${item.productId}`} className="flex-shrink-0">
                  <div className="w-20 h-20 bg-gray-200 rounded flex items-center justify-center">
                    <span className="text-xs text-gray-400">#{item.productId}</span>
                  </div>
                </Link>
                <div className="flex-1 min-w-0">
                  <Link href={`/products/${item.productId}`} className="hover:underline">
                    <p className="text-sm font-medium text-gray-900">{item.productName}</p>
                  </Link>
                  <p className="text-xs text-gray-500 mt-1">{item.optionName}</p>
                  <p className="text-xs text-gray-500">수량: {item.quantity}</p>
                  <div className="mt-1">
                    <OrderStatusBadge status={item.status} />
                  </div>
                </div>
                <div className="text-right flex-shrink-0">
                  <p className="text-sm font-bold text-gray-900">
                    {formatPriceWithCurrency(item.totalPrice)}
                  </p>
                  <p className="text-xs text-gray-500">
                    {formatPriceWithCurrency(item.unitPrice)} x {item.quantity}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* Shipping Info */}
        <section>
          <h2 className="text-lg font-bold mb-4">배송 정보</h2>
          <div className="bg-gray-50 rounded-lg p-4 space-y-2">
            <div className="flex gap-4">
              <span className="text-sm text-gray-500 w-20">수령인</span>
              <span className="text-sm text-gray-900">{order.receiverName}</span>
            </div>
            <div className="flex gap-4">
              <span className="text-sm text-gray-500 w-20">연락처</span>
              <span className="text-sm text-gray-900">{order.receiverPhone}</span>
            </div>
            {order.status === 'SHIPPING' && (
              <div className="mt-3 pt-3 border-t border-gray-200">
                <button className="text-sm text-blue-600 hover:text-blue-800 font-medium">
                  배송 추적하기
                </button>
              </div>
            )}
          </div>
        </section>

        {/* Payment Summary */}
        <section>
          <h2 className="text-lg font-bold mb-4">결제 정보</h2>
          <div className="bg-gray-50 rounded-lg p-4 space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-gray-600">상품 금액</span>
              <span>{formatPriceWithCurrency(order.totalAmount)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">배송비</span>
              <span>
                {order.shippingFee === 0 ? (
                  <span className="text-green-600">무료</span>
                ) : (
                  formatPriceWithCurrency(order.shippingFee)
                )}
              </span>
            </div>
            {order.discountAmount > 0 && (
              <div className="flex justify-between text-red-600">
                <span>할인</span>
                <span>-{formatPriceWithCurrency(order.discountAmount)}</span>
              </div>
            )}
            <div className="border-t border-gray-200 pt-2 mt-2">
              <div className="flex justify-between font-bold text-base">
                <span>총 결제 금액</span>
                <span>{formatPriceWithCurrency(order.paymentAmount)}</span>
              </div>
            </div>
          </div>
        </section>

        {/* Action Buttons */}
        <div className="flex gap-3">
          {canCancel && (
            <button
              onClick={handleCancel}
              disabled={cancelling}
              className="flex-1 py-3 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50 transition-colors"
            >
              {cancelling ? '취소 처리 중...' : '주문 취소'}
            </button>
          )}
          {order.status === 'DELIVERED' && (
            <button className="flex-1 py-3 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 transition-colors">
              교환/반품 신청
            </button>
          )}
          <Link
            href="/orders"
            className="flex-1 py-3 bg-black text-white rounded-lg text-sm font-medium text-center hover:bg-gray-800 transition-colors"
          >
            주문 목록
          </Link>
        </div>
      </div>
    </div>
  );
}
