'use client';

import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { getProduct } from '@/lib/api/product';
import { addCartItem } from '@/lib/api/cart';
import { formatPriceWithCurrency } from '@/lib/utils/format';
import { useCartStore } from '@/stores/cartStore';
import { useAuthStore } from '@/stores/authStore';
import type { Product, ProductOptionValue } from '@/types/product';

const BRAND_COLORS = [
  'bg-rose-400', 'bg-sky-400', 'bg-amber-400', 'bg-emerald-400',
  'bg-violet-400', 'bg-pink-400', 'bg-teal-400', 'bg-orange-400',
];

function ProductDetailSkeleton() {
  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-12">
        <div>
          <div className="aspect-square bg-gray-200 rounded-lg animate-pulse" />
        </div>
        <div className="space-y-6">
          <div className="h-4 bg-gray-200 rounded w-1/4 animate-pulse" />
          <div className="h-8 bg-gray-200 rounded w-3/4 animate-pulse" />
          <div className="h-6 bg-gray-200 rounded w-1/3 animate-pulse" />
          <div className="space-y-2">
            <div className="h-4 bg-gray-200 rounded w-full animate-pulse" />
            <div className="h-4 bg-gray-200 rounded w-5/6 animate-pulse" />
          </div>
        </div>
      </div>
    </div>
  );
}

export default function ProductDetailPage() {
  const params = useParams();
  const router = useRouter();
  const productId = Number(params.id);

  const [product, setProduct] = useState<Product | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [selectedOptions, setSelectedOptions] = useState<Record<string, ProductOptionValue | null>>({});
  const [quantity, setQuantity] = useState(1);
  const [addingToCart, setAddingToCart] = useState(false);
  const [cartMessage, setCartMessage] = useState<string | null>(null);

  const { isAuthenticated } = useAuthStore();
  const { addItem } = useCartStore();

  useEffect(() => {
    if (!productId) return;
    setLoading(true);
    getProduct(productId)
      .then((res) => setProduct(res.data.data))
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  }, [productId]);

  if (loading) return <ProductDetailSkeleton />;

  if (error || !product) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 text-center">
        <p className="text-lg text-gray-500">상품을 찾을 수 없습니다.</p>
        <button
          onClick={() => router.push('/products')}
          className="mt-4 text-sm text-black underline"
        >
          상품 목록으로 돌아가기
        </button>
      </div>
    );
  }

  const thumbnail = product.images?.find((img) => img.isThumbnail)?.url || product.images?.[0]?.url;
  const brandColor = BRAND_COLORS[product.brandId % BRAND_COLORS.length];
  const brandInitial = product.brandName ? product.brandName.charAt(0).toUpperCase() : '?';
  const discountRate = product.discountPrice && product.price
    ? Math.round(((product.price - product.discountPrice) / product.price) * 100)
    : 0;
  const displayPrice = product.discountPrice || product.price;

  // Calculate additional price from selected options
  const additionalPrice = Object.values(selectedOptions)
    .filter(Boolean)
    .reduce((sum, opt) => sum + (opt?.additionalPrice || 0), 0);
  const totalPrice = (displayPrice + additionalPrice) * quantity;

  const handleOptionSelect = (optionName: string, value: ProductOptionValue) => {
    setSelectedOptions((prev) => ({
      ...prev,
      [optionName]: prev[optionName]?.id === value.id ? null : value,
    }));
  };

  const handleAddToCart = async () => {
    if (product.options && product.options.length > 0) {
      const unselected = product.options.find((opt) => !selectedOptions[opt.name]);
      if (unselected) {
        setCartMessage(`${unselected.name}을(를) 선택해주세요.`);
        return;
      }
    }

    setAddingToCart(true);
    setCartMessage(null);

    try {
      // Get the first selected option value id for the API
      const selectedOptionValues = Object.values(selectedOptions).filter(Boolean);
      const optionId = selectedOptionValues.length > 0 ? selectedOptionValues[0]?.id : undefined;

      if (isAuthenticated) {
        await addCartItem({ productId: product.id, optionId, quantity });
      }

      // Also add to local cart store
      addItem({
        id: Date.now(), // temp id for local store
        productId: product.id,
        productName: product.name,
        productImage: thumbnail || '',
        optionId: optionId || null,
        optionName: selectedOptionValues.map((v) => v?.value).join(', ') || null,
        quantity,
        unitPrice: displayPrice + additionalPrice,
        totalPrice,
      });

      setCartMessage('장바구니에 추가되었습니다.');
    } catch {
      setCartMessage('장바구니 추가에 실패했습니다.');
    } finally {
      setAddingToCart(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-12">
        {/* Product Image */}
        <div>
          <div className="aspect-square rounded-lg overflow-hidden">
            {thumbnail ? (
              <img src={thumbnail} alt={product.name} className="w-full h-full object-cover" />
            ) : (
              <div className={`w-full h-full flex items-center justify-center ${brandColor}`}>
                <span className="text-8xl font-bold text-white/80">{brandInitial}</span>
              </div>
            )}
          </div>
        </div>

        {/* Product Info */}
        <div className="space-y-6">
          {/* Brand */}
          <p className="text-sm text-gray-500">{product.brandName}</p>

          {/* Name */}
          <h1 className="text-2xl font-bold text-gray-900">{product.name}</h1>

          {/* Price */}
          <div className="flex items-center gap-3">
            {discountRate > 0 && (
              <span className="text-2xl font-bold text-red-600">{discountRate}%</span>
            )}
            <span className="text-2xl font-bold text-gray-900">
              {formatPriceWithCurrency(displayPrice)}
            </span>
            {product.discountPrice && (
              <span className="text-lg text-gray-400 line-through">
                {formatPriceWithCurrency(product.price)}
              </span>
            )}
          </div>

          {/* Options */}
          {product.options && product.options.length > 0 && (
            <div className="space-y-4">
              {product.options.map((option) => (
                <div key={option.id}>
                  <h3 className="text-sm font-semibold mb-2">{option.name}</h3>
                  <div className="flex flex-wrap gap-2">
                    {option.values.map((value) => {
                      const isSelected = selectedOptions[option.name]?.id === value.id;
                      const isOutOfStock = value.stockQuantity <= 0;
                      return (
                        <button
                          key={value.id}
                          disabled={isOutOfStock}
                          onClick={() => handleOptionSelect(option.name, value)}
                          className={`px-4 py-2 border rounded-lg text-sm transition-colors ${
                            isSelected
                              ? 'border-black bg-black text-white'
                              : isOutOfStock
                                ? 'border-gray-200 text-gray-300 cursor-not-allowed line-through'
                                : 'border-gray-300 hover:border-black'
                          }`}
                        >
                          {value.value}
                          {value.additionalPrice > 0 && (
                            <span className="ml-1 text-xs">
                              (+{formatPriceWithCurrency(value.additionalPrice)})
                            </span>
                          )}
                        </button>
                      );
                    })}
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Quantity */}
          <div>
            <h3 className="text-sm font-semibold mb-2">수량</h3>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                className="w-10 h-10 border border-gray-300 rounded-lg flex items-center justify-center hover:bg-gray-50"
              >
                -
              </button>
              <span className="w-12 text-center font-medium">{quantity}</span>
              <button
                onClick={() => setQuantity((q) => q + 1)}
                className="w-10 h-10 border border-gray-300 rounded-lg flex items-center justify-center hover:bg-gray-50"
              >
                +
              </button>
            </div>
          </div>

          {/* Total Price */}
          <div className="border-t border-gray-200 pt-4">
            <div className="flex justify-between items-center">
              <span className="text-sm text-gray-600">총 상품 금액</span>
              <span className="text-xl font-bold text-gray-900">
                {formatPriceWithCurrency(totalPrice)}
              </span>
            </div>
          </div>

          {/* Cart Message */}
          {cartMessage && (
            <p className={`text-sm ${cartMessage.includes('실패') || cartMessage.includes('선택') ? 'text-red-500' : 'text-green-600'}`}>
              {cartMessage}
            </p>
          )}

          {/* Add to Cart */}
          <button
            onClick={handleAddToCart}
            disabled={addingToCart || product.status === 'SOLD_OUT'}
            className={`w-full py-4 rounded-lg font-medium transition-colors ${
              product.status === 'SOLD_OUT'
                ? 'bg-gray-300 text-white cursor-not-allowed'
                : 'bg-black text-white hover:bg-gray-800'
            }`}
          >
            {product.status === 'SOLD_OUT'
              ? '품절'
              : addingToCart
                ? '추가 중...'
                : '장바구니 담기'}
          </button>

          {/* Description */}
          {product.description && (
            <div>
              <h3 className="text-sm font-semibold mb-2">상품 설명</h3>
              <p className="text-sm text-gray-600 whitespace-pre-line">{product.description}</p>
            </div>
          )}

          {/* Info */}
          <div className="text-xs text-gray-400 space-y-1">
            <p>카테고리: {product.categoryName}</p>
            {product.stockQuantity !== undefined && (
              <p>재고: {product.stockQuantity > 0 ? `${product.stockQuantity}개` : '품절'}</p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
