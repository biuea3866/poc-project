'use client';

import { useState, useMemo, useCallback, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useCartStore } from '@/stores/cartStore';
import { useAuthStore } from '@/stores/authStore';
import { getCart, updateCartItem, removeCartItem } from '@/lib/api/cart';
import { formatPriceWithCurrency } from '@/lib/utils/format';

const SHIPPING_FEE = 3000;
const FREE_SHIPPING_THRESHOLD = 50000;

function CartSkeleton() {
  return (
    <div className="space-y-4">
      {[1, 2, 3].map((i) => (
        <div key={i} className="flex gap-4 p-4 border border-gray-200 rounded-lg animate-pulse">
          <div className="w-5 h-5 bg-gray-200 rounded mt-1" />
          <div className="w-20 h-20 sm:w-24 sm:h-24 bg-gray-200 rounded-lg" />
          <div className="flex-1 space-y-2">
            <div className="h-4 bg-gray-200 rounded w-1/2" />
            <div className="h-3 bg-gray-200 rounded w-1/4" />
            <div className="h-4 bg-gray-200 rounded w-1/3 mt-2" />
          </div>
        </div>
      ))}
    </div>
  );
}

export default function CartPage() {
  const router = useRouter();
  const { items, setItems, removeItem, updateQuantity } = useCartStore();
  const { isAuthenticated } = useAuthStore();
  const [selectedIds, setSelectedIds] = useState<Set<number>>(
    new Set(items.map((item) => item.id))
  );
  const [loading, setLoading] = useState(false);
  const [updatingIds, setUpdatingIds] = useState<Set<number>>(new Set());

  // Sync cart with server on mount if authenticated
  useEffect(() => {
    if (isAuthenticated) {
      setLoading(true);
      getCart()
        .then((res) => {
          const cart = res.data.data;
          if (cart) {
            setItems(cart.items);
            setSelectedIds(new Set(cart.items.map((item) => item.id)));
          }
        })
        .catch(() => {})
        .finally(() => setLoading(false));
    }
  }, [isAuthenticated, setItems]);

  // Keep selectedIds in sync when items change
  useEffect(() => {
    setSelectedIds((prev) => {
      const itemIds = new Set(items.map((item) => item.id));
      const next = new Set(Array.from(prev).filter((id) => itemIds.has(id)));
      return next;
    });
  }, [items]);

  const allSelected = items.length > 0 && selectedIds.size === items.length;

  const handleToggleAll = () => {
    if (allSelected) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(items.map((item) => item.id)));
    }
  };

  const handleToggleItem = (itemId: number) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(itemId)) {
        next.delete(itemId);
      } else {
        next.add(itemId);
      }
      return next;
    });
  };

  const handleUpdateQuantity = useCallback(async (itemId: number, quantity: number) => {
    if (quantity < 1) return;
    setUpdatingIds((prev) => new Set(prev).add(itemId));
    updateQuantity(itemId, quantity);

    if (isAuthenticated) {
      try {
        await updateCartItem(itemId, { quantity });
      } catch {
        // Silently fail -- optimistic update stays
      }
    }
    setUpdatingIds((prev) => {
      const next = new Set(prev);
      next.delete(itemId);
      return next;
    });
  }, [isAuthenticated, updateQuantity]);

  const handleRemoveItem = useCallback(async (itemId: number) => {
    removeItem(itemId);
    setSelectedIds((prev) => {
      const next = new Set(prev);
      next.delete(itemId);
      return next;
    });

    if (isAuthenticated) {
      try {
        await removeCartItem(itemId);
      } catch {
        // Silently fail
      }
    }
  }, [isAuthenticated, removeItem]);

  const handleRemoveSelected = () => {
    selectedIds.forEach((id) => handleRemoveItem(id));
  };

  const selectedItems = useMemo(
    () => items.filter((item) => selectedIds.has(item.id)),
    [items, selectedIds],
  );

  const selectedSubtotal = useMemo(
    () => selectedItems.reduce((sum, item) => sum + item.unitPrice * item.quantity, 0),
    [selectedItems],
  );

  const selectedCount = useMemo(
    () => selectedItems.reduce((sum, item) => sum + item.quantity, 0),
    [selectedItems],
  );

  const shippingFee =
    selectedSubtotal >= FREE_SHIPPING_THRESHOLD ? 0 : selectedCount > 0 ? SHIPPING_FEE : 0;
  const total = selectedSubtotal + shippingFee;

  const handleCheckout = () => {
    if (!isAuthenticated) {
      router.push('/auth/login');
      return;
    }
    if (selectedItems.length === 0) {
      return;
    }
    router.push('/orders/checkout');
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-8">장바구니</h1>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Cart Items */}
        <div className="lg:col-span-2">
          {loading ? (
            <CartSkeleton />
          ) : items.length === 0 ? (
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
                  d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 100 4 2 2 0 000-4z"
                />
              </svg>
              <p className="text-lg mb-4">장바구니가 비어있습니다</p>
              <Link
                href="/products"
                className="inline-block bg-black text-white px-6 py-2 rounded-lg font-medium hover:bg-gray-800 transition-colors"
              >
                쇼핑 계속하기
              </Link>
            </div>
          ) : (
            <>
              {/* Select All / Delete Selected */}
              <div className="flex items-center justify-between py-3 border-b border-gray-200 mb-4">
                <label className="flex items-center gap-3 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={allSelected}
                    onChange={handleToggleAll}
                    className="w-5 h-5 accent-black rounded"
                  />
                  <span className="text-sm font-medium">
                    전체 선택 ({selectedIds.size}/{items.length})
                  </span>
                </label>
                <button
                  onClick={handleRemoveSelected}
                  disabled={selectedIds.size === 0}
                  className="text-sm text-gray-500 hover:text-red-500 disabled:text-gray-300 disabled:cursor-not-allowed transition-colors"
                >
                  선택 삭제
                </button>
              </div>

              {/* Item List */}
              <div className="space-y-4">
                {items.map((item) => (
                  <div key={item.id} className="flex gap-3 sm:gap-4 p-3 sm:p-4 border border-gray-200 rounded-lg">
                    {/* Checkbox */}
                    <label className="flex items-start pt-1 cursor-pointer flex-shrink-0">
                      <input
                        type="checkbox"
                        checked={selectedIds.has(item.id)}
                        onChange={() => handleToggleItem(item.id)}
                        className="w-5 h-5 accent-black rounded"
                      />
                    </label>

                    {/* Product Thumbnail */}
                    <Link href={`/products/${item.productId}`} className="flex-shrink-0">
                      <div className="w-20 h-20 sm:w-24 sm:h-24 bg-gray-200 rounded-lg flex items-center justify-center">
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
                          onClick={() => handleUpdateQuantity(item.id, Math.max(1, item.quantity - 1))}
                          disabled={updatingIds.has(item.id)}
                          className="w-8 h-8 border border-gray-300 rounded flex items-center justify-center text-sm hover:bg-gray-50 disabled:opacity-50 transition-colors"
                        >
                          -
                        </button>
                        <span className="w-8 text-center text-sm">{item.quantity}</span>
                        <button
                          onClick={() => handleUpdateQuantity(item.id, Math.min(99, item.quantity + 1))}
                          disabled={updatingIds.has(item.id)}
                          className="w-8 h-8 border border-gray-300 rounded flex items-center justify-center text-sm hover:bg-gray-50 disabled:opacity-50 transition-colors"
                        >
                          +
                        </button>
                      </div>
                    </div>

                    {/* Item Total & Delete */}
                    <div className="flex flex-col items-end justify-between flex-shrink-0">
                      <button
                        onClick={() => handleRemoveItem(item.id)}
                        className="text-gray-400 hover:text-gray-600 text-sm transition-colors"
                        aria-label="삭제"
                      >
                        <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                          <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                        </svg>
                      </button>
                      <span className="text-sm font-bold text-gray-900">
                        {formatPriceWithCurrency(item.unitPrice * item.quantity)}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </>
          )}
        </div>

        {/* Order Summary */}
        {items.length > 0 && (
          <div>
            <div className="bg-gray-50 rounded-lg p-6 sticky top-24">
              <h3 className="text-lg font-bold mb-4">주문 요약</h3>
              <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-gray-600">상품 금액 ({selectedCount}개)</span>
                  <span>{formatPriceWithCurrency(selectedSubtotal)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">배송비</span>
                  <span>
                    {shippingFee === 0 && selectedCount > 0 ? (
                      <span className="text-green-600">무료</span>
                    ) : (
                      formatPriceWithCurrency(shippingFee)
                    )}
                  </span>
                </div>
                {selectedCount > 0 && selectedSubtotal < FREE_SHIPPING_THRESHOLD && (
                  <p className="text-xs text-gray-400">
                    {formatPriceWithCurrency(FREE_SHIPPING_THRESHOLD - selectedSubtotal)} 더 구매 시
                    무료배송
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
                disabled={selectedItems.length === 0}
                onClick={handleCheckout}
                className={`w-full mt-4 py-3 rounded-lg font-medium transition-colors ${
                  selectedItems.length === 0
                    ? 'bg-gray-300 text-white cursor-not-allowed'
                    : 'bg-black text-white hover:bg-gray-800'
                }`}
              >
                주문하기 ({selectedItems.length}개)
              </button>
              <Link
                href="/products"
                className="block text-center mt-3 text-sm text-gray-500 hover:text-gray-900 transition-colors"
              >
                쇼핑 계속하기
              </Link>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
