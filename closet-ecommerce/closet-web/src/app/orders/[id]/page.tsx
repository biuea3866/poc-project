'use client';

import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { getOrder, cancelOrder } from '@/lib/api/order';
import { formatPriceWithCurrency, formatDateTime } from '@/lib/utils/format';
import type { Order } from '@/types/order';

const ORDER_STATUS_LABEL: Record<string, string> = {
  CREATED: '주문 생성',
  PAID: '결제 완료',
  PREPARING: '상품 준비중',
  SHIPPED: '배송중',
  DELIVERED: '배송 완료',
  CANCELLED: '주문 취소',
  REFUNDED: '환불 완료',
};

export default function OrderDetailPage() {
  const params = useParams();
  const router = useRouter();
  const orderId = Number(params.id);

  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [cancelling, setCancelling] = useState(false);

  useEffect(() => {
    if (!orderId) return;
    setLoading(true);
    getOrder(orderId)
      .then((res) => {
        setOrder(res.data.data || null);
      })
      .catch(() => {
        setOrder(null);
      })
      .finally(() => {
        setLoading(false);
      });
  }, [orderId]);

  const handleCancel = async () => {
    if (!order || !confirm('주문을 취소하시겠습니까?')) return;
    setCancelling(true);
    try {
      const res = await cancelOrder(order.id);
      if (res.data.success && res.data.data) {
        setOrder(res.data.data);
        alert('주문이 취소되었습니다.');
      }
    } catch (e: any) {
      alert(e.response?.data?.error?.message || '주문 취소에 실패했습니다.');
    } finally {
      setCancelling(false);
    }
  };

  if (loading) {
    return (
      <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-8">주문 상세</h1>
        <div className="space-y-6 animate-pulse">
          <div className="bg-gray-50 rounded-lg p-6">
            <div className="h-5 bg-gray-200 rounded w-1/3 mb-4" />
            <div className="h-4 bg-gray-200 rounded w-1/4" />
          </div>
          <div>
            <div className="h-5 bg-gray-200 rounded w-1/4 mb-4" />
            {[1, 2].map((i) => (
              <div key={i} className="flex gap-4 py-4 border-b border-gray-200">
                <div className="w-20 h-20 bg-gray-200 rounded" />
                <div className="flex-1 space-y-2">
                  <div className="h-4 bg-gray-200 rounded w-1/2" />
                  <div className="h-3 bg-gray-200 rounded w-1/4" />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  if (!order) {
    return (
      <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-8">주문 상세</h1>
        <div className="text-center py-16 text-gray-500">
          <p className="text-lg">주문을 찾을 수 없습니다.</p>
          <Link
            href="/orders"
            className="inline-block mt-4 text-sm text-blue-600 hover:underline"
          >
            주문 내역으로 돌아가기
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center gap-2 mb-8">
        <Link href="/orders" className="text-gray-500 hover:text-gray-700 text-sm">
          주문 내역
        </Link>
        <span className="text-gray-300">/</span>
        <h1 className="text-2xl font-bold text-gray-900">주문 상세</h1>
      </div>

      <div className="space-y-6">
        {/* Order Info */}
        <div className="bg-gray-50 rounded-lg p-6">
          <div className="flex justify-between items-center mb-2">
            <p className="font-medium text-gray-900">주문번호: {order.orderNumber}</p>
            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-200 text-gray-800">
              {ORDER_STATUS_LABEL[order.status] || order.status}
            </span>
          </div>
          <p className="text-sm text-gray-500">{formatDateTime(order.orderedAt)}</p>
        </div>

        {/* Order Items */}
        <div>
          <h2 className="text-lg font-bold mb-4">주문 상품</h2>
          <div className="space-y-4">
            {order.items?.map((item) => (
              <div key={item.id} className="flex gap-4 py-4 border-b border-gray-200">
                <Link href={`/products/${item.productId}`} className="flex-shrink-0">
                  <div className="w-20 h-20 bg-gray-200 rounded flex items-center justify-center">
                    <span className="text-xs text-gray-400">#{item.productId}</span>
                  </div>
                </Link>
                <div className="flex-1 min-w-0">
                  <Link href={`/products/${item.productId}`} className="hover:underline">
                    <p className="text-sm font-medium text-gray-900 truncate">{item.productName}</p>
                  </Link>
                  <p className="text-xs text-gray-500 mt-1">{item.optionName}</p>
                  <p className="text-xs text-gray-500 mt-1">수량: {item.quantity}</p>
                </div>
                <div className="text-right">
                  <p className="text-sm font-bold text-gray-900">
                    {formatPriceWithCurrency(item.totalPrice)}
                  </p>
                  <p className="text-xs text-gray-500 mt-1">
                    {formatPriceWithCurrency(item.unitPrice)} x {item.quantity}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Shipping Address */}
        <div>
          <h2 className="text-lg font-bold mb-4">배송 정보</h2>
          <div className="bg-gray-50 rounded-lg p-4 space-y-1 text-sm">
            <p className="text-gray-900 font-medium">{order.receiverName}</p>
            <p className="text-gray-600">{order.receiverPhone}</p>
          </div>
        </div>

        {/* Payment Summary */}
        <div>
          <h2 className="text-lg font-bold mb-4">결제 정보</h2>
          <div className="bg-gray-50 rounded-lg p-4 space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-gray-600">상품 금액</span>
              <span>{formatPriceWithCurrency(order.totalAmount)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-600">배송비</span>
              <span>{formatPriceWithCurrency(order.shippingFee)}</span>
            </div>
            {order.discountAmount > 0 && (
              <div className="flex justify-between text-red-600">
                <span>할인</span>
                <span>-{formatPriceWithCurrency(order.discountAmount)}</span>
              </div>
            )}
            <div className="border-t border-gray-200 pt-2 mt-2">
              <div className="flex justify-between font-bold text-base">
                <span>결제 금액</span>
                <span>{formatPriceWithCurrency(order.paymentAmount)}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Actions */}
        {(order.status === 'CREATED' || order.status === 'PAID') && (
          <div className="flex justify-end">
            <button
              onClick={handleCancel}
              disabled={cancelling}
              className="px-4 py-2 text-sm border border-red-300 text-red-600 rounded-lg hover:bg-red-50 disabled:opacity-50 transition-colors"
            >
              {cancelling ? '취소 처리 중...' : '주문 취소'}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
