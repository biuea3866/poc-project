'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useCartStore } from '@/stores/cartStore';
import { useAuthStore } from '@/stores/authStore';
import { getAddresses } from '@/lib/api/member';
import { createOrder } from '@/lib/api/order';
import { formatPriceWithCurrency } from '@/lib/utils/format';
import type { ShippingAddress } from '@/types/member';

const SHIPPING_FEE = 3000;
const FREE_SHIPPING_THRESHOLD = 50000;

export default function CheckoutPage() {
  const router = useRouter();
  const { items, totalPrice, totalCount, clearCart } = useCartStore();
  const { memberId, isAuthenticated } = useAuthStore();

  const [addresses, setAddresses] = useState<ShippingAddress[]>([]);
  const [selectedAddressId, setSelectedAddressId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [ordering, setOrdering] = useState(false);

  const subtotal = totalPrice();
  const count = totalCount();
  const shippingFee = subtotal >= FREE_SHIPPING_THRESHOLD ? 0 : (count > 0 ? SHIPPING_FEE : 0);
  const total = subtotal + shippingFee;

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/login');
      return;
    }
    if (count === 0) {
      router.push('/cart');
      return;
    }

    getAddresses()
      .then((res) => {
        const addrs = res.data.data || [];
        setAddresses(addrs);
        const defaultAddr = addrs.find((a) => a.isDefault);
        if (defaultAddr) {
          setSelectedAddressId(defaultAddr.id);
        } else if (addrs.length > 0) {
          setSelectedAddressId(addrs[0].id);
        }
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [isAuthenticated, count, router]);

  const selectedAddress = addresses.find((a) => a.id === selectedAddressId) || null;

  const handleOrder = async () => {
    if (!selectedAddress) {
      alert('배송지를 선택해주세요.');
      return;
    }
    setOrdering(true);
    try {
      const res = await createOrder({
        memberId: memberId!,
        sellerId: 1,
        items: items.map((item) => ({
          productId: item.productId,
          productOptionId: item.productOptionId,
          quantity: item.quantity,
          unitPrice: item.unitPrice,
        })),
        receiverName: selectedAddress.name,
        receiverPhone: selectedAddress.phone,
        zipCode: selectedAddress.zipCode,
        address: selectedAddress.address,
        detailAddress: selectedAddress.detailAddress,
        shippingFee,
      });

      if (res.data.success && res.data.data) {
        clearCart();
        alert(`주문이 완료되었습니다! 주문번호: ${res.data.data.orderNumber}`);
        router.push(`/orders/${res.data.data.id}`);
      }
    } catch (e: any) {
      alert(e.response?.data?.error?.message || '주문에 실패했습니다.');
    } finally {
      setOrdering(false);
    }
  };

  if (loading) {
    return (
      <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-8">주문/결제</h1>
        <div className="space-y-6 animate-pulse">
          <div className="h-32 bg-gray-200 rounded-lg" />
          <div className="h-48 bg-gray-200 rounded-lg" />
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-8">주문/결제</h1>

      <div className="space-y-8">
        {/* Order Items Summary */}
        <div>
          <h2 className="text-lg font-bold mb-4">주문 상품 ({count}개)</h2>
          <div className="border border-gray-200 rounded-lg divide-y divide-gray-200">
            {items.map((item) => (
              <div key={item.id} className="flex justify-between items-center p-4">
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-900 truncate">
                    상품 #{item.productId}
                  </p>
                  <p className="text-xs text-gray-500 mt-1">
                    옵션 #{item.productOptionId} / 수량: {item.quantity}
                  </p>
                </div>
                <p className="text-sm font-bold text-gray-900 ml-4">
                  {formatPriceWithCurrency(item.unitPrice * item.quantity)}
                </p>
              </div>
            ))}
          </div>
        </div>

        {/* Shipping Address Selection */}
        <div>
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-lg font-bold">배송지 선택</h2>
            <button
              onClick={() => router.push('/mypage/addresses')}
              className="text-sm text-blue-600 hover:underline"
            >
              배송지 관리
            </button>
          </div>
          {addresses.length === 0 ? (
            <div className="border border-gray-200 rounded-lg p-6 text-center text-gray-500">
              <p className="text-sm">등록된 배송지가 없습니다.</p>
              <button
                onClick={() => router.push('/mypage/addresses')}
                className="mt-2 text-sm text-blue-600 hover:underline"
              >
                배송지 추가하기
              </button>
            </div>
          ) : (
            <div className="space-y-3">
              {addresses.map((addr) => (
                <label
                  key={addr.id}
                  className={`block border rounded-lg p-4 cursor-pointer transition-colors ${
                    selectedAddressId === addr.id
                      ? 'border-black bg-gray-50'
                      : 'border-gray-200 hover:border-gray-400'
                  }`}
                >
                  <div className="flex items-start gap-3">
                    <input
                      type="radio"
                      name="address"
                      checked={selectedAddressId === addr.id}
                      onChange={() => setSelectedAddressId(addr.id)}
                      className="mt-1"
                    />
                    <div className="flex-1">
                      <div className="flex items-center gap-2">
                        <p className="text-sm font-medium text-gray-900">{addr.name}</p>
                        {addr.isDefault && (
                          <span className="text-xs bg-black text-white px-1.5 py-0.5 rounded">
                            기본
                          </span>
                        )}
                      </div>
                      <p className="text-sm text-gray-600 mt-1">{addr.phone}</p>
                      <p className="text-sm text-gray-600">
                        [{addr.zipCode}] {addr.address} {addr.detailAddress}
                      </p>
                    </div>
                  </div>
                </label>
              ))}
            </div>
          )}
        </div>

        {/* Payment Summary */}
        <div>
          <h2 className="text-lg font-bold mb-4">결제 금액</h2>
          <div className="bg-gray-50 rounded-lg p-6">
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-600">상품 금액</span>
                <span>{formatPriceWithCurrency(subtotal)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">배송비</span>
                <span>
                  {shippingFee === 0 ? (
                    <span className="text-green-600">무료</span>
                  ) : (
                    formatPriceWithCurrency(shippingFee)
                  )}
                </span>
              </div>
              <div className="border-t border-gray-200 pt-2 mt-2">
                <div className="flex justify-between font-bold text-lg">
                  <span>합계</span>
                  <span>{formatPriceWithCurrency(total)}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Order Button */}
        <button
          disabled={ordering || !selectedAddress}
          onClick={handleOrder}
          className={`w-full py-4 rounded-lg font-medium text-lg transition-colors ${
            ordering || !selectedAddress
              ? 'bg-gray-300 text-white cursor-not-allowed'
              : 'bg-black text-white hover:bg-gray-800'
          }`}
        >
          {ordering ? '주문 처리 중...' : `${formatPriceWithCurrency(total)} 결제하기`}
        </button>
      </div>
    </div>
  );
}
