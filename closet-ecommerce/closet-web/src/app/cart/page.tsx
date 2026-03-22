'use client';

import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useCartStore } from '@/stores/cartStore';
import { useAuthStore } from '@/stores/authStore';
import { formatPriceWithCurrency } from '@/lib/utils/format';

const SHIPPING_FEE = 3000;
const FREE_SHIPPING_THRESHOLD = 50000;

export default function CartPage() {
  const router = useRouter();
  const { items, removeItem, updateQuantity, totalPrice, totalCount } = useCartStore();
  const { isAuthenticated } = useAuthStore();

  const subtotal = totalPrice();
  const count = totalCount();
  const shippingFee = subtotal >= FREE_SHIPPING_THRESHOLD ? 0 : (count > 0 ? SHIPPING_FEE : 0);
  const total = subtotal + shippingFee;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-8">장바구니</h1>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Cart Items */}
        <div className="lg:col-span-2">
          {items.length === 0 ? (
            <div className="text-center py-16 text-gray-500">
              <p className="text-lg mb-4">장바구니가 비어있습니다</p>
              <Link
                href="/products"
                className="inline-block bg-black text-white px-6 py-2 rounded-lg font-medium hover:bg-gray-800 transition-colors"
              >
                쇼핑 계속하기
              </Link>
            </div>
          ) : (
            <div className="space-y-4">
              {items.map((item) => (
                <div key={item.id} className="flex gap-4 p-4 border border-gray-200 rounded-lg">
                  {/* Product Link */}
                  <Link href={`/products/${item.productId}`} className="flex-shrink-0">
                    <div className="w-24 h-24 bg-gray-200 rounded-lg flex items-center justify-center">
                      <span className="text-xs text-gray-400">
                        상품 #{item.productId}
                      </span>
                    </div>
                  </Link>

                  {/* Product Info */}
                  <div className="flex-1 min-w-0">
                    <Link href={`/products/${item.productId}`} className="hover:underline">
                      <h3 className="text-sm font-medium text-gray-900 truncate">
                        상품 #{item.productId}
                      </h3>
                    </Link>
                    <p className="text-xs text-gray-500 mt-1">
                      옵션 #{item.productOptionId}
                    </p>
                    <p className="text-sm font-bold text-gray-900 mt-2">
                      {formatPriceWithCurrency(item.unitPrice)}
                    </p>

                    {/* Quantity Controls */}
                    <div className="flex items-center gap-2 mt-3">
                      <button
                        onClick={() => updateQuantity(item.id, Math.max(1, item.quantity - 1))}
                        className="w-8 h-8 border border-gray-300 rounded flex items-center justify-center text-sm hover:bg-gray-50"
                      >
                        -
                      </button>
                      <span className="w-8 text-center text-sm">{item.quantity}</span>
                      <button
                        onClick={() => updateQuantity(item.id, item.quantity + 1)}
                        className="w-8 h-8 border border-gray-300 rounded flex items-center justify-center text-sm hover:bg-gray-50"
                      >
                        +
                      </button>
                    </div>
                  </div>

                  {/* Item Total & Delete */}
                  <div className="flex flex-col items-end justify-between">
                    <button
                      onClick={() => removeItem(item.id)}
                      className="text-gray-400 hover:text-gray-600 text-sm"
                    >
                      삭제
                    </button>
                    <span className="text-sm font-bold text-gray-900">
                      {formatPriceWithCurrency(item.unitPrice * item.quantity)}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Order Summary */}
        <div>
          <div className="bg-gray-50 rounded-lg p-6 sticky top-8">
            <h3 className="text-lg font-bold mb-4">주문 요약</h3>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-gray-600">상품 금액 ({count}개)</span>
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
              {count > 0 && subtotal < FREE_SHIPPING_THRESHOLD && (
                <p className="text-xs text-gray-400">
                  {formatPriceWithCurrency(FREE_SHIPPING_THRESHOLD - subtotal)} 더 구매 시 무료배송
                </p>
              )}
              <div className="border-t border-gray-200 pt-2 mt-2">
                <div className="flex justify-between font-bold text-base">
                  <span>합계</span>
                  <span>{formatPriceWithCurrency(total)}</span>
                </div>
              </div>
            </div>
            <button
              disabled={count === 0}
              onClick={() => {
                if (!isAuthenticated) {
                  router.push('/login');
                  return;
                }
                router.push('/checkout');
              }}
              className={`w-full mt-4 py-3 rounded-lg font-medium transition-colors ${
                count === 0
                  ? 'bg-gray-300 text-white cursor-not-allowed'
                  : 'bg-black text-white hover:bg-gray-800'
              }`}
            >
              주문하기
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
