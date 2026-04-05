'use client';

import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { getOrder } from '@/lib/api/order';
import { createReturn } from '@/lib/api/claim';
import { useAuthStore } from '@/stores/authStore';
import { formatPriceWithCurrency } from '@/lib/utils/format';
import type { Order, OrderItem } from '@/types/order';
import type { ReturnReason, ClaimStatus } from '@/types/claim';

const RETURN_REASONS: { value: ReturnReason; label: string }[] = [
  { value: 'CHANGE_OF_MIND', label: '단순 변심' },
  { value: 'WRONG_SIZE', label: '사이즈가 맞지 않음' },
  { value: 'DEFECTIVE', label: '상품 불량/하자' },
  { value: 'WRONG_ITEM', label: '주문과 다른 상품 배송' },
  { value: 'DAMAGED_IN_DELIVERY', label: '배송 중 파손' },
  { value: 'OTHER', label: '기타' },
];

const CUSTOMER_FAULT_REASONS: ReturnReason[] = [
  'CHANGE_OF_MIND',
  'WRONG_SIZE',
];

const STATUS_LABELS: Record<ClaimStatus, string> = {
  REQUESTED: '반품 접수',
  APPROVED: '반품 승인',
  COLLECTING: '수거 중',
  COLLECTED: '수거 완료',
  COMPLETED: '반품 완료',
  REJECTED: '반품 거절',
};

function ReturnSkeleton() {
  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="h-8 bg-gray-200 rounded w-1/4 mb-8 animate-pulse" />
      <div className="space-y-6">
        {[1, 2, 3].map((i) => (
          <div
            key={i}
            className="border border-gray-200 rounded-lg p-6 animate-pulse"
          >
            <div className="h-5 bg-gray-200 rounded w-1/3 mb-4" />
            <div className="h-4 bg-gray-200 rounded w-2/3" />
          </div>
        ))}
      </div>
    </div>
  );
}

export default function ReturnPage() {
  const params = useParams();
  const router = useRouter();
  const orderId = Number(params.id);
  const { isAuthenticated } = useAuthStore();

  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [selectedItem, setSelectedItem] = useState<OrderItem | null>(null);
  const [reason, setReason] = useState<ReturnReason | ''>('');
  const [reasonDetail, setReasonDetail] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [returnResult, setReturnResult] = useState<{
    status: ClaimStatus;
    shippingFee: number;
    refundAmount: number;
  } | null>(null);

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/auth/login');
      return;
    }
    if (!orderId) return;

    setLoading(true);
    getOrder(orderId)
      .then((res) => setOrder(res.data.data))
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  }, [orderId, isAuthenticated, router]);

  const isCustomerFault =
    reason !== '' && CUSTOMER_FAULT_REASONS.includes(reason as ReturnReason);
  const shippingFeeEstimate = isCustomerFault ? 3000 : 0;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedItem || !reason) return;

    setSubmitting(true);
    try {
      const res = await createReturn({
        orderId,
        orderItemId: selectedItem.id,
        reason: reason as ReturnReason,
        reasonDetail: reasonDetail || undefined,
      });
      if (res.data.data) {
        setReturnResult({
          status: res.data.data.status,
          shippingFee: res.data.data.shippingFee,
          refundAmount: res.data.data.refundAmount,
        });
        setSubmitted(true);
      }
    } catch {
      alert('반품 신청에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <ReturnSkeleton />;

  if (error || !order) {
    return (
      <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-16 text-center">
        <p className="text-lg text-gray-500">주문을 찾을 수 없습니다.</p>
        <button
          onClick={() => router.push('/orders')}
          className="mt-4 text-sm text-black underline"
        >
          주문 내역으로
        </button>
      </div>
    );
  }

  // Success screen
  if (submitted && returnResult) {
    return (
      <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-16 text-center">
        <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-6">
          <svg
            className="h-8 w-8 text-green-600"
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
        </div>
        <h1 className="text-2xl font-bold text-gray-900 mb-2">
          반품 신청이 완료되었습니다
        </h1>
        <p className="text-sm text-gray-500 mb-6">
          처리 상태: {STATUS_LABELS[returnResult.status]}
        </p>
        <div className="bg-gray-50 rounded-lg p-6 max-w-sm mx-auto mb-8 text-sm space-y-2">
          <div className="flex justify-between">
            <span className="text-gray-600">반품 배송비</span>
            <span>
              {returnResult.shippingFee === 0
                ? '무료'
                : formatPriceWithCurrency(returnResult.shippingFee)}
            </span>
          </div>
          <div className="flex justify-between font-bold text-base border-t border-gray-200 pt-2 mt-2">
            <span>환불 예정 금액</span>
            <span>{formatPriceWithCurrency(returnResult.refundAmount)}</span>
          </div>
        </div>
        <div className="flex gap-3 max-w-sm mx-auto">
          <Link
            href={`/orders/${orderId}`}
            className="flex-1 py-3 border border-gray-300 rounded-lg text-sm font-medium text-center hover:bg-gray-50 transition-colors"
          >
            주문 상세
          </Link>
          <Link
            href="/orders"
            className="flex-1 py-3 bg-black text-white rounded-lg text-sm font-medium text-center hover:bg-gray-800 transition-colors"
          >
            주문 내역
          </Link>
        </div>
      </div>
    );
  }

  const returnableItems = order.items.filter((item) =>
    ['DELIVERED', 'CONFIRMED'].includes(item.status),
  );

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="flex items-center gap-4 mb-8">
        <button
          onClick={() => router.push(`/orders/${orderId}`)}
          className="text-gray-500 hover:text-gray-900"
        >
          <svg
            className="h-5 w-5"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth={2}
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M15 19l-7-7 7-7"
            />
          </svg>
        </button>
        <h1 className="text-2xl font-bold text-gray-900">반품 신청</h1>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Select Item */}
        <section className="border border-gray-200 rounded-lg p-4 sm:p-6">
          <h2 className="text-lg font-bold mb-4">반품할 상품 선택</h2>
          {returnableItems.length === 0 ? (
            <p className="text-sm text-gray-500">
              반품 가능한 상품이 없습니다.
            </p>
          ) : (
            <div className="space-y-3">
              {returnableItems.map((item) => (
                <label
                  key={item.id}
                  className={`flex items-start gap-3 p-3 sm:p-4 border rounded-lg cursor-pointer transition-colors ${
                    selectedItem?.id === item.id
                      ? 'border-black bg-gray-50'
                      : 'border-gray-200 hover:border-gray-400'
                  }`}
                >
                  <input
                    type="radio"
                    name="returnItem"
                    checked={selectedItem?.id === item.id}
                    onChange={() => setSelectedItem(item)}
                    className="mt-1 accent-black"
                  />
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-gray-900">
                      {item.productName}
                    </p>
                    <p className="text-xs text-gray-500 mt-1">
                      {item.optionName} / {item.quantity}개
                    </p>
                    <p className="text-sm font-bold text-gray-900 mt-1">
                      {formatPriceWithCurrency(item.totalPrice)}
                    </p>
                  </div>
                </label>
              ))}
            </div>
          )}
        </section>

        {/* Reason Selection */}
        {selectedItem && (
          <section className="border border-gray-200 rounded-lg p-4 sm:p-6">
            <h2 className="text-lg font-bold mb-4">반품 사유</h2>
            <div className="space-y-2">
              {RETURN_REASONS.map((r) => (
                <label
                  key={r.value}
                  className={`flex items-center gap-3 p-3 border rounded-lg cursor-pointer transition-colors ${
                    reason === r.value
                      ? 'border-black bg-gray-50'
                      : 'border-gray-200 hover:border-gray-400'
                  }`}
                >
                  <input
                    type="radio"
                    name="reason"
                    value={r.value}
                    checked={reason === r.value}
                    onChange={() => setReason(r.value)}
                    className="accent-black"
                  />
                  <span className="text-sm text-gray-700">{r.label}</span>
                </label>
              ))}
            </div>

            {/* Detail Input */}
            {reason && (
              <div className="mt-4">
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  상세 사유 (선택)
                </label>
                <textarea
                  value={reasonDetail}
                  onChange={(e) => setReasonDetail(e.target.value)}
                  rows={3}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-black resize-none text-sm"
                  placeholder="상세 사유를 입력해주세요"
                />
              </div>
            )}
          </section>
        )}

        {/* Shipping Fee Info */}
        {reason && (
          <section className="bg-gray-50 rounded-lg p-4 sm:p-6">
            <h2 className="text-lg font-bold mb-3">배송비 안내</h2>
            <div className="text-sm space-y-2">
              {isCustomerFault ? (
                <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-3">
                  <p className="text-yellow-800 font-medium">
                    고객 사유 반품 시 왕복 배송비{' '}
                    {formatPriceWithCurrency(3000)}가 차감됩니다.
                  </p>
                  <p className="text-yellow-700 text-xs mt-1">
                    단순 변심, 사이즈 불일치 등
                  </p>
                </div>
              ) : (
                <div className="bg-green-50 border border-green-200 rounded-lg p-3">
                  <p className="text-green-800 font-medium">
                    판매자 사유 반품은 배송비가 무료입니다.
                  </p>
                  <p className="text-green-700 text-xs mt-1">
                    상품 불량, 오배송, 배송 파손 등
                  </p>
                </div>
              )}
              {selectedItem && (
                <div className="border-t border-gray-200 pt-3 mt-3 space-y-1">
                  <div className="flex justify-between">
                    <span className="text-gray-600">상품 금액</span>
                    <span>
                      {formatPriceWithCurrency(selectedItem.totalPrice)}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600">반품 배송비</span>
                    <span className={isCustomerFault ? 'text-red-600' : ''}>
                      {isCustomerFault
                        ? `-${formatPriceWithCurrency(shippingFeeEstimate)}`
                        : '무료'}
                    </span>
                  </div>
                  <div className="flex justify-between font-bold text-base pt-2 border-t border-gray-200">
                    <span>예상 환불 금액</span>
                    <span>
                      {formatPriceWithCurrency(
                        selectedItem.totalPrice - shippingFeeEstimate,
                      )}
                    </span>
                  </div>
                </div>
              )}
            </div>
          </section>
        )}

        {/* Submit */}
        <div className="flex gap-3">
          <Link
            href={`/orders/${orderId}`}
            className="flex-1 py-3 border border-gray-300 rounded-lg text-sm font-medium text-center text-gray-700 hover:bg-gray-50 transition-colors"
          >
            취소
          </Link>
          <button
            type="submit"
            disabled={!selectedItem || !reason || submitting}
            className="flex-1 py-3 bg-black text-white rounded-lg text-sm font-medium hover:bg-gray-800 disabled:bg-gray-300 disabled:cursor-not-allowed transition-colors"
          >
            {submitting ? '처리 중...' : '반품 신청'}
          </button>
        </div>
      </form>
    </div>
  );
}
