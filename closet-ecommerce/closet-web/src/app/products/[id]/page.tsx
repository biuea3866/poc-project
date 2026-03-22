'use client';

import { useEffect, useState, useMemo } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { getProduct, getBrands } from '@/lib/api/product';
import { addCartItem } from '@/lib/api/cart';
import { formatPriceWithCurrency } from '@/lib/utils/format';
import { useCartStore } from '@/stores/cartStore';
import { useAuthStore } from '@/stores/authStore';
import type { Product, ProductOption, Brand } from '@/types/product';

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
  const [brands, setBrands] = useState<Brand[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [selectedOption, setSelectedOption] = useState<ProductOption | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [addingToCart, setAddingToCart] = useState(false);
  const [cartMessage, setCartMessage] = useState<string | null>(null);

  const { isAuthenticated } = useAuthStore();
  const { addItem } = useCartStore();

  useEffect(() => {
    if (!productId) return;
    setLoading(true);
    Promise.all([getProduct(productId), getBrands()])
      .then(([prodRes, brandRes]) => {
        setProduct(prodRes.data.data);
        setBrands(brandRes.data.data || []);
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false));
  }, [productId]);

  const brandMap = useMemo(() => {
    const map: Record<number, string> = {};
    brands.forEach((b) => { map[b.id] = b.name; });
    return map;
  }, [brands]);

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

  const thumbnail = product.images?.[0]?.imageUrl;
  const brandColor = BRAND_COLORS[product.brandId % BRAND_COLORS.length];
  const brandName = brandMap[product.brandId] || `Brand #${product.brandId}`;
  const brandInitial = brandName.charAt(0).toUpperCase();
  const displayPrice = product.salePrice;

  // Calculate additional price from selected option
  const additionalPrice = selectedOption?.additionalPrice || 0;
  const totalPrice = (displayPrice + additionalPrice) * quantity;


  const handleOptionSelect = (option: ProductOption) => {
    setSelectedOption((prev) => prev?.id === option.id ? null : option);
  };

  const handleAddToCart = async () => {
    if (product.options && product.options.length > 0 && !selectedOption) {
      setCartMessage('옵션을 선택해주세요.');
      return;
    }

    setAddingToCart(true);
    setCartMessage(null);

    try {
      const optionId = selectedOption?.id || product.options?.[0]?.id || 0;

      if (isAuthenticated) {
        await addCartItem({
          productId: product.id,
          productOptionId: optionId,
          quantity,
          unitPrice: displayPrice + additionalPrice,
        });
      }

      // Also add to local cart store
      addItem({
        id: Date.now(), // temp id for local store
        productId: product.id,
        productOptionId: optionId,
        quantity,
        unitPrice: displayPrice + additionalPrice,
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
          <p className="text-sm text-gray-500">{brandName}</p>

          {/* Name */}
          <h1 className="text-2xl font-bold text-gray-900">{product.name}</h1>

          {/* Price */}
          <div className="flex items-center gap-3">
            {product.discountRate > 0 && (
              <span className="text-2xl font-bold text-red-600">{product.discountRate}%</span>
            )}
            <span className="text-2xl font-bold text-gray-900">
              {formatPriceWithCurrency(product.salePrice)}
            </span>
            {product.discountRate > 0 && (
              <span className="text-lg text-gray-400 line-through">
                {formatPriceWithCurrency(product.basePrice)}
              </span>
            )}
          </div>

          {/* Product meta */}
          <div className="flex gap-4 text-xs text-gray-400">
            {product.season && <span>시즌: {product.season}</span>}
            {product.fitType && <span>핏: {product.fitType}</span>}
            {product.gender && <span>성별: {product.gender}</span>}
          </div>

          {/* Options (size + color) */}
          {product.options && product.options.length > 0 && (
            <div className="space-y-4">
              <h3 className="text-sm font-semibold mb-2">옵션 선택</h3>
              <div className="flex flex-wrap gap-2">
                {product.options.map((option) => {
                  const isSelected = selectedOption?.id === option.id;
                  return (
                    <button
                      key={option.id}
                      onClick={() => handleOptionSelect(option)}
                      className={`px-4 py-2 border rounded-lg text-sm transition-colors ${
                        isSelected
                          ? 'border-black bg-black text-white'
                          : 'border-gray-300 hover:border-black'
                      }`}
                    >
                      <span
                        className="inline-block w-3 h-3 rounded-full mr-2 border border-gray-300"
                        style={{ backgroundColor: option.colorHex }}
                      />
                      {option.colorName} / {option.size}
                      {option.additionalPrice > 0 && (
                        <span className="ml-1 text-xs">
                          (+{formatPriceWithCurrency(option.additionalPrice)})
                        </span>
                      )}
                    </button>
                  );
                })}
              </div>
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
            disabled={addingToCart || product.status === 'SOLD_OUT' || product.status === 'INACTIVE'}
            className={`w-full py-4 rounded-lg font-medium transition-colors ${
              product.status === 'SOLD_OUT' || product.status === 'INACTIVE'
                ? 'bg-gray-300 text-white cursor-not-allowed'
                : 'bg-black text-white hover:bg-gray-800'
            }`}
          >
            {product.status === 'SOLD_OUT'
              ? '품절'
              : product.status === 'INACTIVE'
                ? '판매 중지'
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

          {/* Size Guide */}
          {product.sizeGuides && product.sizeGuides.length > 0 && (
            <div>
              <h3 className="text-sm font-semibold mb-2">사이즈 가이드</h3>
              <table className="w-full text-xs text-gray-600 border-collapse">
                <thead>
                  <tr className="border-b border-gray-200">
                    <th className="py-2 text-left">사이즈</th>
                    <th className="py-2 text-center">어깨</th>
                    <th className="py-2 text-center">가슴</th>
                    <th className="py-2 text-center">총장</th>
                    <th className="py-2 text-center">소매</th>
                  </tr>
                </thead>
                <tbody>
                  {product.sizeGuides.map((sg) => (
                    <tr key={sg.id} className="border-b border-gray-100">
                      <td className="py-2">{sg.size}</td>
                      <td className="py-2 text-center">{sg.shoulderWidth}</td>
                      <td className="py-2 text-center">{sg.chestWidth}</td>
                      <td className="py-2 text-center">{sg.totalLength}</td>
                      <td className="py-2 text-center">{sg.sleeveLength}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
