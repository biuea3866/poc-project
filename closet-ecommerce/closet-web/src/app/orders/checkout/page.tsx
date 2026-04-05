'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useCartStore } from '@/stores/cartStore';
import { useAuthStore } from '@/stores/authStore';
import { createOrder } from '@/lib/api/order';
import { getAddresses } from '@/lib/api/member';
import { formatPriceWithCurrency } from '@/lib/utils/format';
import type { ShippingAddress } from '@/types/member';
import type { PaymentMethod } from '@/types/payment';

const SHIPPING_FEE = 3000;
const FREE_SHIPPING_THRESHOLD = 50000;

const PAYMENT_METHODS: { value: PaymentMethod; label: string; icon: string }[] = [
  { value: 'CARD', label: '신용/체크카드', icon: 'C' },
  { value: 'BANK_TRANSFER', label: '무통장입금', icon: 'B' },
  { value: 'MOBILE', label: '간편결제', icon: 'M' },
  { value: 'VIRTUAL_ACCOUNT', label: '가상계좌', icon: 'V' },
];

function CheckoutSkeleton() {
  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-8">주문/결제</h1>
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-6">
          {[1, 2, 3].map((i) => (
            <div key={i} className="border border-gray-200 rounded-lg p-6 animate-pulse">
              <div className="h-5 bg-gray-200 rounded w-1/4 mb-4" />
              <div className="space-y-3">
                <div className="h-4 bg-gray-200 rounded w-full" />
                <div className="h-4 bg-gray-200 rounded w-2/3" />
              </div>
            </div>
          ))}
        </div>
        <div>
          <div className="bg-gray-50 rounded-lg p-6 animate-pulse">
            <div className="h-5 bg-gray-200 rounded w-1/3 mb-4" />
            <div className="space-y-2">
              <div className="h-4 bg-gray-200 rounded w-full" />
              <div className="h-4 bg-gray-200 rounded w-full" />
              <div className="h-10 bg-gray-200 rounded w-full mt-4" />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default function CheckoutPage() {
  const router = useRouter();
  const { items, totalPrice, totalCount, clearCart } = useCartStore();
  const { memberId, isAuthenticated } = useAuthStore();

  const [addresses, setAddresses] = useState<ShippingAddress[]>([]);
  const [selectedAddressId, setSelectedAddressId] = useState<number | null>(null);
  const [showNewAddress, setShowNewAddress] = useState(false);
  const [newAddress, setNewAddress] = useState({
    name: '',
    phone: '',
    zipCode: '',
    address: '',
    detailAddress: '',
  });
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('CARD');
  const [couponCode, setCouponCode] = useState('');
  const [couponDiscount, setCouponDiscount] = useState(0);
  const [usePoints, setUsePoints] = useState(0);
  const [ordering, setOrdering] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pageLoading, setPageLoading] = useState(true);

  const subtotal = totalPrice();
  const count = totalCount();
  const shippingFee = subtotal >= FREE_SHIPPING_THRESHOLD ? 0 : count > 0 ? SHIPPING_FEE : 0;
  const discountTotal = couponDiscount + usePoints;
  const paymentAmount = Math.max(0, subtotal + shippingFee - discountTotal);

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/auth/login');
      return;
    }
    if (items.length === 0) {
      router.push('/cart');
      return;
    }

    // Fetch saved addresses
    getAddresses()
      .then((res) => {
        const addrs = res.data.data || [];
        setAddresses(addrs);
        const defaultAddr = addrs.find((a) => a.isDefault);
        if (defaultAddr) {
          setSelectedAddressId(defaultAddr.id);
        } else if (addrs.length > 0) {
          setSelectedAddressId(addrs[0].id);
        } else {
          setShowNewAddress(true);
        }
      })
      .catch(() => {
        setShowNewAddress(true);
      })
      .finally(() => setPageLoading(false));
  }, [isAuthenticated, items.length, router]);

  const selectedAddress = addresses.find((a) => a.id === selectedAddressId);

  const getShippingInfo = () => {
    if (selectedAddress && !showNewAddress) {
      return {
        receiverName: selectedAddress.name,
        receiverPhone: selectedAddress.phone,
        zipCode: selectedAddress.zipCode,
        address: selectedAddress.address,
        detailAddress: selectedAddress.detailAddress,
      };
    }
    return {
      receiverName: newAddress.name,
      receiverPhone: newAddress.phone,
      zipCode: newAddress.zipCode,
      address: newAddress.address,
      detailAddress: newAddress.detailAddress,
    };
  };

  const handleApplyCoupon = () => {
    if (couponCode.trim()) {
      setCouponDiscount(5000);
    }
  };

  const handleOrder = async () => {
    setError(null);
    const shipping = getShippingInfo();

    if (!shipping.receiverName || !shipping.receiverPhone || !shipping.address) {
      setError('배송지 정보를 모두 입력해주세요.');
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
        receiverName: shipping.receiverName,
        receiverPhone: shipping.receiverPhone,
        zipCode: shipping.zipCode,
        address: shipping.address,
        detailAddress: shipping.detailAddress,
        shippingFee,
      });

      if (res.data.success && res.data.data) {
        clearCart();
        router.push(`/orders/${res.data.data.id}`);
      }
    } catch (e: unknown) {
      const err = e as { response?: { data?: { error?: { message?: string } } } };
      setError(err.response?.data?.error?.message || '주문에 실패했습니다.');
    } finally {
      setOrdering(false);
    }
  };

  if (!isAuthenticated || items.length === 0) {
    return null;
  }

  if (pageLoading) {
    return <CheckoutSkeleton />;
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-8">주문/결제</h1>

      {error && (
        <div className="bg-red-50 text-red-600 text-sm p-4 rounded-lg mb-6">{error}</div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left Column: Address + Items + Payment Method */}
        <div className="lg:col-span-2 space-y-6">
          {/* Shipping Address */}
          <section className="border border-gray-200 rounded-lg p-4 sm:p-6">
            <h2 className="text-lg font-bold mb-4">배송지</h2>

            {/* Saved Addresses */}
            {addresses.length > 0 && !showNewAddress && (
              <div className="space-y-3">
                {addresses.map((addr) => (
                  <label
                    key={addr.id}
                    className={`flex items-start gap-3 p-3 sm:p-4 border rounded-lg cursor-pointer transition-colors ${
                      selectedAddressId === addr.id
                        ? 'border-black bg-gray-50'
                        : 'border-gray-200 hover:border-gray-400'
                    }`}
                  >
                    <input
                      type="radio"
                      name="address"
                      checked={selectedAddressId === addr.id}
                      onChange={() => setSelectedAddressId(addr.id)}
                      className="mt-1 accent-black"
                    />
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="text-sm font-medium">{addr.name}</span>
                        {addr.isDefault && (
                          <span className="text-xs bg-black text-white px-2 py-0.5 rounded-full">
                            기본
                          </span>
                        )}
                      </div>
                      <p className="text-sm text-gray-600 mt-1">{addr.phone}</p>
                      <p className="text-sm text-gray-600 break-words">
                        ({addr.zipCode}) {addr.address} {addr.detailAddress}
                      </p>
                    </div>
                  </label>
                ))}
                <button
                  onClick={() => setShowNewAddress(true)}
                  className="w-full py-3 border border-dashed border-gray-300 rounded-lg text-sm text-gray-500 hover:text-gray-900 hover:border-gray-400 transition-colors"
                >
                  + 새 배송지 입력
                </button>
              </div>
            )}

            {/* New Address Form */}
            {showNewAddress && (
              <div className="space-y-4">
                {addresses.length > 0 && (
                  <button
                    onClick={() => setShowNewAddress(false)}
                    className="text-sm text-gray-500 hover:text-gray-900"
                  >
                    저장된 배송지에서 선택
                  </button>
                )}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      수령인
                    </label>
                    <input
                      type="text"
                      value={newAddress.name}
                      onChange={(e) => setNewAddress((p) => ({ ...p, name: e.target.value }))}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-black"
                      placeholder="수령인 이름"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      연락처
                    </label>
                    <input
                      type="tel"
                      value={newAddress.phone}
                      onChange={(e) => setNewAddress((p) => ({ ...p, phone: e.target.value }))}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-black"
                      placeholder="010-0000-0000"
                    />
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    우편번호
                  </label>
                  <div className="flex gap-2">
                    <input
                      type="text"
                      value={newAddress.zipCode}
                      onChange={(e) => setNewAddress((p) => ({ ...p, zipCode: e.target.value }))}
                      className="w-32 px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-black"
                      placeholder="우편번호"
                    />
                    <button className="px-4 py-2 bg-gray-100 text-sm rounded-lg hover:bg-gray-200 transition-colors">
                      주소 검색
                    </button>
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">주소</label>
                  <input
                    type="text"
                    value={newAddress.address}
                    onChange={(e) => setNewAddress((p) => ({ ...p, address: e.target.value }))}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-black"
                    placeholder="기본 주소"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    상세 주소
                  </label>
                  <input
                    type="text"
                    value={newAddress.detailAddress}
                    onChange={(e) =>
                      setNewAddress((p) => ({ ...p, detailAddress: e.target.value }))
                    }
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-black"
                    placeholder="상세 주소 입력"
                  />
                </div>
              </div>
            )}
          </section>

          {/* Order Items Summary */}
          <section className="border border-gray-200 rounded-lg p-4 sm:p-6">
            <h2 className="text-lg font-bold mb-4">주문 상품 ({count}개)</h2>
            <div className="space-y-3">
              {items.map((item) => (
                <div
                  key={item.id}
                  className="flex items-center gap-3 sm:gap-4 py-3 border-b border-gray-100 last:border-b-0"
                >
                  <div className="w-14 h-14 sm:w-16 sm:h-16 bg-gray-200 rounded-lg flex items-center justify-center flex-shrink-0">
                    <span className="text-xs text-gray-400">#{item.productId}</span>
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-gray-900 truncate">
                      상품 #{item.productId}
                    </p>
                    <p className="text-xs text-gray-500">
                      옵션 #{item.productOptionId} / 수량: {item.quantity}
                    </p>
                  </div>
                  <span className="text-sm font-bold text-gray-900 flex-shrink-0">
                    {formatPriceWithCurrency(item.unitPrice * item.quantity)}
                  </span>
                </div>
              ))}
            </div>
          </section>

          {/* Payment Method */}
          <section className="border border-gray-200 rounded-lg p-4 sm:p-6">
            <h2 className="text-lg font-bold mb-4">결제 수단</h2>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              {PAYMENT_METHODS.map((method) => (
                <button
                  key={method.value}
                  onClick={() => setPaymentMethod(method.value)}
                  className={`p-3 sm:p-4 border rounded-lg text-center transition-colors ${
                    paymentMethod === method.value
                      ? 'border-black bg-black text-white'
                      : 'border-gray-200 hover:border-gray-400'
                  }`}
                >
                  <div
                    className={`w-8 h-8 sm:w-10 sm:h-10 rounded-full mx-auto mb-2 flex items-center justify-center text-base sm:text-lg font-bold ${
                      paymentMethod === method.value
                        ? 'bg-white text-black'
                        : 'bg-gray-100 text-gray-500'
                    }`}
                  >
                    {method.icon}
                  </div>
                  <span className="text-xs font-medium">{method.label}</span>
                </button>
              ))}
            </div>
          </section>

          {/* Coupon / Points */}
          <section className="border border-gray-200 rounded-lg p-4 sm:p-6">
            <h2 className="text-lg font-bold mb-4">쿠폰 / 적립금</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">쿠폰 코드</label>
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={couponCode}
                    onChange={(e) => setCouponCode(e.target.value)}
                    className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-black"
                    placeholder="쿠폰 코드를 입력하세요"
                  />
                  <button
                    onClick={handleApplyCoupon}
                    className="px-4 py-2 bg-gray-100 text-sm rounded-lg hover:bg-gray-200 transition-colors flex-shrink-0"
                  >
                    적용
                  </button>
                </div>
                {couponDiscount > 0 && (
                  <p className="text-xs text-green-600 mt-1">
                    쿠폰 할인: -{formatPriceWithCurrency(couponDiscount)}
                  </p>
                )}
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  적립금 사용
                </label>
                <div className="flex gap-2 items-center">
                  <input
                    type="number"
                    value={usePoints || ''}
                    onChange={(e) => setUsePoints(Number(e.target.value) || 0)}
                    min={0}
                    className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-black"
                    placeholder="사용할 적립금"
                  />
                  <span className="text-sm text-gray-500 flex-shrink-0">보유: 0P</span>
                </div>
              </div>
            </div>
          </section>
        </div>

        {/* Right Column: Payment Summary */}
        <div>
          <div className="bg-gray-50 rounded-lg p-6 sticky top-24">
            <h3 className="text-lg font-bold mb-4">결제 금액</h3>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-600">상품 금액</span>
                <span>{formatPriceWithCurrency(subtotal)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-600">배송비</span>
                <span>
                  {shippingFee === 0 && count > 0 ? (
                    <span className="text-green-600">무료</span>
                  ) : (
                    formatPriceWithCurrency(shippingFee)
                  )}
                </span>
              </div>
              {discountTotal > 0 && (
                <div className="flex justify-between text-red-600">
                  <span>할인</span>
                  <span>-{formatPriceWithCurrency(discountTotal)}</span>
                </div>
              )}
              <div className="border-t border-gray-200 pt-3 mt-3">
                <div className="flex justify-between font-bold text-lg">
                  <span>총 결제 금액</span>
                  <span>{formatPriceWithCurrency(paymentAmount)}</span>
                </div>
              </div>
            </div>

            <button
              onClick={handleOrder}
              disabled={ordering || count === 0}
              className={`w-full mt-6 py-4 rounded-lg font-medium text-base transition-colors ${
                ordering || count === 0
                  ? 'bg-gray-300 text-white cursor-not-allowed'
                  : 'bg-black text-white hover:bg-gray-800'
              }`}
            >
              {ordering ? '결제 처리 중...' : `${formatPriceWithCurrency(paymentAmount)} 결제하기`}
            </button>

            <p className="text-xs text-gray-400 mt-3 text-center">
              주문 내용을 확인하였으며, 결제에 동의합니다.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
