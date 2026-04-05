'use client';

import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { getShippingTracking, confirmDelivery } from '@/lib/api/shipping';
import { useAuthStore } from '@/stores/authStore';
import { formatDateTime } from '@/lib/utils/format';
import type { ShippingTracking, ShippingStatus } from '@/types/shipping';

const STATUS_CONFIG: Record<
  ShippingStatus,
  { label: string; description: string }
> = {
  READY: { label: '배송 준비', description: '상품이 배송 준비 중입니다' },
  PICKED_UP: {
    label: '집하 완료',
    description: '택배사가 상품을 수거했습니다',
  },
  IN_TRANSIT: {
    label: '배송 중',
    description: '상품이 배송 중입니다',
  },
  OUT_FOR_DELIVERY: {
    label: '배달 출발',
    description: '배달원이 배송지로 이동 중입니다',
  },
  DELIVERED: {
    label: '배송 완료',
    description: '상품이 배송 완료되었습니다',
  },
};

const STATUS_ORDER: ShippingStatus[] = [
  'READY',
  'PICKED_UP',
  'IN_TRANSIT',
  'OUT_FOR_DELIVERY',
  'DELIVERED',
];

function TrackingSkeleton() {
  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="h-6 bg-gray-200 rounded w-1/3 mb-8 animate-pulse" />
      <div className="space-y-8">
        <div className="bg-gray-50 rounded-lg p-6 animate-pulse">
          <div className="h-5 bg-gray-200 rounded w-1/4 mb-4" />
          <div className="flex justify-between">
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className="flex flex-col items-center gap-2">
                <div className="w-10 h-10 bg-gray-200 rounded-full" />
                <div className="h-3 bg-gray-200 rounded w-12" />
              </div>
            ))}
          </div>
        </div>
        <div className="space-y-4">
          {[1, 2, 3].map((i) => (
            <div key={i} className="flex gap-4 animate-pulse">
              <div className="w-3 h-3 bg-gray-200 rounded-full mt-1" />
              <div className="flex-1 space-y-2">
                <div className="h-4 bg-gray-200 rounded w-1/2" />
                <div className="h-3 bg-gray-200 rounded w-1/3" />
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

export default function TrackingPage() {
  const params = useParams();
  const router = useRouter();
  const orderId = Number(params.id);
  const { isAuthenticated } = useAuthStore();

  const [tracking, setTracking] = useState<ShippingTracking | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [confirming, setConfirming] = useState(false);

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/auth/login');
      return;
    }
    if (!orderId) return;

    setLoading(true);
    getShippingTracking(orderId)
      .then((res) => setTracking(res.data.data))
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  }, [orderId, isAuthenticated, router]);

  const handleConfirmDelivery = async () => {
    if (!confirm('구매확정 하시겠습니까? 구매확정 후에는 반품/교환이 불가합니다.'))
      return;
    setConfirming(true);
    try {
      await confirmDelivery(orderId);
      alert('구매확정이 완료되었습니다.');
      router.push(`/orders/${orderId}`);
    } catch {
      alert('구매확정에 실패했습니다.');
    } finally {
      setConfirming(false);
    }
  };

  if (loading) return <TrackingSkeleton />;

  if (error || !tracking) {
    return (
      <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-16 text-center">
        <p className="text-lg text-gray-500">배송 정보를 찾을 수 없습니다.</p>
        <button
          onClick={() => router.push(`/orders/${orderId}`)}
          className="mt-4 text-sm text-black underline"
        >
          주문 상세로 돌아가기
        </button>
      </div>
    );
  }

  const currentStatusIndex = STATUS_ORDER.indexOf(tracking.status);

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Header */}
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
        <h1 className="text-2xl font-bold text-gray-900">배송 추적</h1>
      </div>

      {/* Shipping Info */}
      <section className="bg-gray-50 rounded-lg p-4 sm:p-6 mb-6">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
          <div>
            <p className="text-sm text-gray-500">택배사</p>
            <p className="text-base font-bold text-gray-900">
              {tracking.carrier}
            </p>
          </div>
          <div className="sm:text-right">
            <p className="text-sm text-gray-500">운송장 번호</p>
            <p className="text-base font-bold text-gray-900">
              {tracking.trackingNumber}
            </p>
          </div>
        </div>
        {tracking.estimatedDeliveryDate && (
          <div className="mt-4 pt-4 border-t border-gray-200">
            <p className="text-sm text-gray-500">예상 배송일</p>
            <p className="text-base font-semibold text-gray-900">
              {formatDateTime(tracking.estimatedDeliveryDate)}
            </p>
          </div>
        )}
      </section>

      {/* Status Timeline */}
      <section className="mb-8">
        <h2 className="text-lg font-bold text-gray-900 mb-4">배송 상태</h2>
        <div className="bg-white border border-gray-200 rounded-lg p-4 sm:p-6">
          {/* Desktop Timeline */}
          <div className="hidden sm:flex items-center justify-between relative">
            {/* Connection line */}
            <div className="absolute top-5 left-5 right-5 h-0.5 bg-gray-200" />
            <div
              className="absolute top-5 left-5 h-0.5 bg-black transition-all"
              style={{
                width: `${(currentStatusIndex / (STATUS_ORDER.length - 1)) * 100}%`,
                maxWidth: 'calc(100% - 40px)',
              }}
            />
            {STATUS_ORDER.map((status, idx) => {
              const isCompleted = idx <= currentStatusIndex;
              const isCurrent = idx === currentStatusIndex;
              return (
                <div
                  key={status}
                  className="relative z-10 flex flex-col items-center"
                >
                  <div
                    className={`w-10 h-10 rounded-full flex items-center justify-center border-2 transition-colors ${
                      isCurrent
                        ? 'bg-black border-black text-white'
                        : isCompleted
                          ? 'bg-black border-black text-white'
                          : 'bg-white border-gray-300 text-gray-400'
                    }`}
                  >
                    {isCompleted ? (
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
                          d="M5 13l4 4L19 7"
                        />
                      </svg>
                    ) : (
                      <span className="text-xs font-bold">{idx + 1}</span>
                    )}
                  </div>
                  <span
                    className={`mt-2 text-xs font-medium text-center ${
                      isCurrent
                        ? 'text-black'
                        : isCompleted
                          ? 'text-gray-700'
                          : 'text-gray-400'
                    }`}
                  >
                    {STATUS_CONFIG[status].label}
                  </span>
                </div>
              );
            })}
          </div>

          {/* Mobile Vertical Timeline */}
          <div className="sm:hidden space-y-4">
            {STATUS_ORDER.map((status, idx) => {
              const isCompleted = idx <= currentStatusIndex;
              const isCurrent = idx === currentStatusIndex;
              return (
                <div key={status} className="flex items-start gap-3">
                  <div className="flex flex-col items-center">
                    <div
                      className={`w-8 h-8 rounded-full flex items-center justify-center border-2 flex-shrink-0 ${
                        isCurrent
                          ? 'bg-black border-black text-white'
                          : isCompleted
                            ? 'bg-black border-black text-white'
                            : 'bg-white border-gray-300 text-gray-400'
                      }`}
                    >
                      {isCompleted ? (
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
                      ) : (
                        <span className="text-xs font-bold">{idx + 1}</span>
                      )}
                    </div>
                    {idx < STATUS_ORDER.length - 1 && (
                      <div
                        className={`w-0.5 h-6 ${isCompleted ? 'bg-black' : 'bg-gray-200'}`}
                      />
                    )}
                  </div>
                  <div className="pt-1">
                    <p
                      className={`text-sm font-medium ${isCurrent ? 'text-black' : isCompleted ? 'text-gray-700' : 'text-gray-400'}`}
                    >
                      {STATUS_CONFIG[status].label}
                    </p>
                    <p className="text-xs text-gray-400">
                      {STATUS_CONFIG[status].description}
                    </p>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* Tracking Logs */}
      <section className="mb-8">
        <h2 className="text-lg font-bold text-gray-900 mb-4">추적 내역</h2>
        {tracking.logs.length === 0 ? (
          <div className="text-center py-8 text-gray-400 text-sm border border-gray-200 rounded-lg">
            배송 추적 내역이 없습니다.
          </div>
        ) : (
          <div className="border border-gray-200 rounded-lg divide-y divide-gray-100">
            {tracking.logs
              .sort(
                (a, b) =>
                  new Date(b.timestamp).getTime() -
                  new Date(a.timestamp).getTime(),
              )
              .map((log) => (
                <div key={log.id} className="p-4 flex gap-4">
                  <div className="flex-shrink-0 w-2 h-2 bg-gray-400 rounded-full mt-2" />
                  <div className="flex-1 min-w-0">
                    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-1">
                      <p className="text-sm font-medium text-gray-900">
                        {log.description}
                      </p>
                      <time className="text-xs text-gray-400 flex-shrink-0">
                        {formatDateTime(log.timestamp)}
                      </time>
                    </div>
                    <p className="text-xs text-gray-500 mt-0.5">
                      {log.location}
                    </p>
                  </div>
                </div>
              ))}
          </div>
        )}
      </section>

      {/* Action Buttons */}
      <div className="flex gap-3">
        {tracking.status === 'DELIVERED' && (
          <button
            onClick={handleConfirmDelivery}
            disabled={confirming}
            className="flex-1 py-3 bg-black text-white rounded-lg text-sm font-medium hover:bg-gray-800 disabled:bg-gray-400 transition-colors"
          >
            {confirming ? '처리 중...' : '구매확정'}
          </button>
        )}
        <Link
          href={`/orders/${orderId}`}
          className="flex-1 py-3 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 text-center hover:bg-gray-50 transition-colors"
        >
          주문 상세
        </Link>
      </div>
    </div>
  );
}
