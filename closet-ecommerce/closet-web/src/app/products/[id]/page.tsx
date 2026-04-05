'use client';

import { useEffect, useState, useMemo } from 'react';
import { useParams, useRouter } from 'next/navigation';
import Image from 'next/image';
import Link from 'next/link';
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
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8 lg:gap-12">
        <div>
          <div className="aspect-square bg-gray-200 rounded-lg animate-pulse" />
          <div className="flex gap-2 mt-3">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="w-16 h-16 sm:w-20 sm:h-20 bg-gray-200 rounded-lg animate-pulse" />
            ))}
          </div>
        </div>
        <div className="space-y-6">
          <div className="h-4 bg-gray-200 rounded w-1/4 animate-pulse" />
          <div className="h-8 bg-gray-200 rounded w-3/4 animate-pulse" />
          <div className="h-6 bg-gray-200 rounded w-1/3 animate-pulse" />
          <div className="space-y-2">
            <div className="h-4 bg-gray-200 rounded w-full animate-pulse" />
            <div className="h-4 bg-gray-200 rounded w-5/6 animate-pulse" />
          </div>
          <div className="h-12 bg-gray-200 rounded animate-pulse" />
          <div className="h-12 bg-gray-200 rounded animate-pulse" />
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
  const [selectedImageIndex, setSelectedImageIndex] = useState(0);
  const [showSizeGuide, setShowSizeGuide] = useState(false);
  const [activeTab, setActiveTab] = useState<'description' | 'sizeGuide' | 'shipping'>('description');

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
        <svg xmlns="http://www.w3.org/2000/svg" className="h-16 w-16 mx-auto text-gray-300 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
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

  const images = product.images?.sort((a, b) => a.sortOrder - b.sortOrder) || [];
  const brandColor = BRAND_COLORS[product.brandId % BRAND_COLORS.length];
  const brandName = brandMap[product.brandId] || `Brand #${product.brandId}`;
  const brandInitial = brandName.charAt(0).toUpperCase();
  const displayPrice = product.salePrice;

  const additionalPrice = selectedOption?.additionalPrice || 0;
  const totalPrice = (displayPrice + additionalPrice) * quantity;

  // Group options by color for better UX
  const uniqueSizes = Array.from(new Set(product.options?.map(o => o.size) || []));
  const uniqueColors = Array.from(
    new Map((product.options || []).map(o => [o.colorName, { name: o.colorName, hex: o.colorHex }])).values()
  );

  const handleOptionSelect = (option: ProductOption) => {
    setSelectedOption((prev) => prev?.id === option.id ? null : option);
  };

  const handleAddToCart = async () => {
    if (product.options && product.options.length > 0 && !selectedOption) {
      setCartMessage('옵션을 선택해주세요.');
      setTimeout(() => setCartMessage(null), 3000);
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

      addItem({
        id: Date.now(),
        productId: product.id,
        productOptionId: optionId,
        quantity,
        unitPrice: displayPrice + additionalPrice,
      });

      setCartMessage('장바구니에 추가되었습니다.');
      setTimeout(() => setCartMessage(null), 3000);
    } catch {
      setCartMessage('장바구니 추가에 실패했습니다.');
      setTimeout(() => setCartMessage(null), 3000);
    } finally {
      setAddingToCart(false);
    }
  };

  const handleBuyNow = async () => {
    if (product.options && product.options.length > 0 && !selectedOption) {
      setCartMessage('옵션을 선택해주세요.');
      setTimeout(() => setCartMessage(null), 3000);
      return;
    }

    if (!isAuthenticated) {
      router.push('/auth/login');
      return;
    }

    const optionId = selectedOption?.id || product.options?.[0]?.id || 0;

    addItem({
      id: Date.now(),
      productId: product.id,
      productOptionId: optionId,
      quantity,
      unitPrice: displayPrice + additionalPrice,
    });

    router.push('/orders/checkout');
  };

  const isSoldOut = product.status === 'SOLD_OUT';
  const isInactive = product.status === 'INACTIVE';
  const isDisabled = isSoldOut || isInactive;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 sm:py-8">
      {/* Breadcrumb */}
      <nav className="flex items-center gap-2 text-sm text-gray-400 mb-6">
        <Link href="/" className="hover:text-gray-700">홈</Link>
        <span>/</span>
        <Link href="/products" className="hover:text-gray-700">상품</Link>
        <span>/</span>
        <span className="text-gray-600 truncate max-w-[200px]">{product.name}</span>
      </nav>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8 lg:gap-12">
        {/* Product Image Gallery */}
        <div>
          {/* Main Image */}
          <div className="aspect-square rounded-lg overflow-hidden mb-3 bg-gray-100">
            {images.length > 0 ? (
              <Image
                src={images[selectedImageIndex]?.imageUrl}
                alt={product.name}
                width={800}
                height={800}
                className="w-full h-full object-cover"
                priority
              />
            ) : (
              <div className={`w-full h-full flex items-center justify-center ${brandColor}`}>
                <span className="text-6xl sm:text-8xl font-bold text-white/80">{brandInitial}</span>
              </div>
            )}
          </div>

          {/* Thumbnail Gallery */}
          {images.length > 1 && (
            <div className="flex gap-2 overflow-x-auto pb-2">
              {images.map((image, index) => (
                <button
                  key={image.id}
                  onClick={() => setSelectedImageIndex(index)}
                  className={`flex-shrink-0 w-16 h-16 sm:w-20 sm:h-20 rounded-lg overflow-hidden border-2 transition-colors ${
                    selectedImageIndex === index ? 'border-black' : 'border-transparent hover:border-gray-300'
                  }`}
                >
                  <Image
                    src={image.imageUrl}
                    alt={`${product.name} ${index + 1}`}
                    width={80}
                    height={80}
                    className="w-full h-full object-cover"
                  />
                </button>
              ))}
            </div>
          )}

          {/* Placeholder thumbnails when no images */}
          {images.length === 0 && (
            <div className="flex gap-2">
              {[0, 1, 2, 3].map((i) => (
                <div
                  key={i}
                  className={`w-16 h-16 sm:w-20 sm:h-20 rounded-lg ${brandColor} ${i === 0 ? 'opacity-100' : 'opacity-40'} flex items-center justify-center`}
                >
                  <span className="text-sm font-bold text-white/80">{brandInitial}</span>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Product Info */}
        <div className="space-y-5">
          {/* Brand */}
          <p className="text-sm text-gray-500">{brandName}</p>

          {/* Name */}
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900">{product.name}</h1>

          {/* Price */}
          <div className="flex items-baseline gap-3">
            {product.discountRate > 0 && (
              <span className="text-xl sm:text-2xl font-bold text-red-600">{product.discountRate}%</span>
            )}
            <span className="text-xl sm:text-2xl font-bold text-gray-900">
              {formatPriceWithCurrency(product.salePrice)}
            </span>
            {product.discountRate > 0 && (
              <span className="text-base sm:text-lg text-gray-400 line-through">
                {formatPriceWithCurrency(product.basePrice)}
              </span>
            )}
          </div>

          {/* Product meta */}
          <div className="flex flex-wrap gap-2">
            {product.season && (
              <span className="text-xs px-2 py-1 bg-gray-100 text-gray-600 rounded-full">시즌: {product.season}</span>
            )}
            {product.fitType && (
              <span className="text-xs px-2 py-1 bg-gray-100 text-gray-600 rounded-full">핏: {product.fitType}</span>
            )}
            {product.gender && (
              <span className="text-xs px-2 py-1 bg-gray-100 text-gray-600 rounded-full">성별: {product.gender}</span>
            )}
          </div>

          {/* Separator */}
          <div className="border-t border-gray-200" />

          {/* Color preview */}
          {uniqueColors.length > 0 && (
            <div>
              <h3 className="text-sm font-semibold mb-2">색상</h3>
              <div className="flex flex-wrap gap-3">
                {uniqueColors.map((color) => (
                  <div
                    key={color.name}
                    className="flex items-center gap-1.5"
                  >
                    <span
                      className="inline-block w-6 h-6 rounded-full border border-gray-300"
                      style={{ backgroundColor: color.hex }}
                      title={color.name}
                    />
                    <span className="text-xs text-gray-600">{color.name}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Options (size + color combined) */}
          {product.options && product.options.length > 0 && (
            <div className="space-y-4">
              <h3 className="text-sm font-semibold">옵션 선택</h3>
              <div className="flex flex-wrap gap-2">
                {product.options.map((option) => {
                  const isSelected = selectedOption?.id === option.id;
                  return (
                    <button
                      key={option.id}
                      onClick={() => handleOptionSelect(option)}
                      className={`px-3 sm:px-4 py-2 border rounded-lg text-sm transition-colors ${
                        isSelected
                          ? 'border-black bg-black text-white'
                          : 'border-gray-300 hover:border-black'
                      }`}
                    >
                      <span
                        className="inline-block w-3 h-3 rounded-full mr-1.5 border border-gray-300 align-middle"
                        style={{ backgroundColor: option.colorHex }}
                      />
                      {option.colorName} / {option.size}
                      {option.additionalPrice > 0 && (
                        <span className="ml-1 text-xs opacity-75">
                          (+{formatPriceWithCurrency(option.additionalPrice)})
                        </span>
                      )}
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {/* Size Guide Toggle */}
          {product.sizeGuides && product.sizeGuides.length > 0 && (
            <button
              onClick={() => setShowSizeGuide(!showSizeGuide)}
              className="flex items-center gap-1 text-sm text-gray-500 hover:text-gray-900 transition-colors"
            >
              <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
              </svg>
              사이즈 가이드 {showSizeGuide ? '접기' : '보기'}
            </button>
          )}

          {/* Size Guide Table */}
          {showSizeGuide && product.sizeGuides && product.sizeGuides.length > 0 && (
            <div className="border border-gray-200 rounded-lg p-4">
              <h3 className="text-sm font-semibold mb-3">사이즈 가이드 (cm)</h3>
              <div className="overflow-x-auto">
                <table className="w-full text-xs text-gray-600 border-collapse">
                  <thead>
                    <tr className="border-b border-gray-200 bg-gray-50">
                      <th className="py-2 px-3 text-left font-medium">사이즈</th>
                      <th className="py-2 px-3 text-center font-medium">어깨</th>
                      <th className="py-2 px-3 text-center font-medium">가슴</th>
                      <th className="py-2 px-3 text-center font-medium">총장</th>
                      <th className="py-2 px-3 text-center font-medium">소매</th>
                    </tr>
                  </thead>
                  <tbody>
                    {product.sizeGuides.map((sg) => (
                      <tr key={sg.id} className="border-b border-gray-100">
                        <td className="py-2 px-3 font-medium">{sg.size}</td>
                        <td className="py-2 px-3 text-center">{sg.shoulderWidth}</td>
                        <td className="py-2 px-3 text-center">{sg.chestWidth}</td>
                        <td className="py-2 px-3 text-center">{sg.totalLength}</td>
                        <td className="py-2 px-3 text-center">{sg.sleeveLength}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              {uniqueSizes.length > 0 && (
                <p className="text-xs text-gray-400 mt-2">
                  * 사이즈: {uniqueSizes.join(', ')}
                </p>
              )}
            </div>
          )}

          {/* Quantity */}
          <div>
            <h3 className="text-sm font-semibold mb-2">수량</h3>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                className="w-10 h-10 border border-gray-300 rounded-lg flex items-center justify-center hover:bg-gray-50 transition-colors"
              >
                -
              </button>
              <span className="w-12 text-center font-medium">{quantity}</span>
              <button
                onClick={() => setQuantity((q) => Math.min(99, q + 1))}
                className="w-10 h-10 border border-gray-300 rounded-lg flex items-center justify-center hover:bg-gray-50 transition-colors"
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
            <div className={`text-sm p-3 rounded-lg ${
              cartMessage.includes('실패') || cartMessage.includes('선택')
                ? 'bg-red-50 text-red-600'
                : 'bg-green-50 text-green-600'
            }`}>
              {cartMessage}
              {cartMessage === '장바구니에 추가되었습니다.' && (
                <Link href="/cart" className="ml-2 underline font-medium">
                  장바구니 보기
                </Link>
              )}
            </div>
          )}

          {/* Action Buttons */}
          <div className="flex gap-3">
            <button
              onClick={handleAddToCart}
              disabled={addingToCart || isDisabled}
              className={`flex-1 py-3 sm:py-4 rounded-lg font-medium transition-colors ${
                isDisabled
                  ? 'bg-gray-300 text-white cursor-not-allowed'
                  : 'border-2 border-black text-black hover:bg-gray-50'
              }`}
            >
              {isSoldOut
                ? '품절'
                : isInactive
                  ? '판매 중지'
                  : addingToCart
                    ? '추가 중...'
                    : '장바구니 담기'}
            </button>
            <button
              onClick={handleBuyNow}
              disabled={isDisabled}
              className={`flex-1 py-3 sm:py-4 rounded-lg font-medium transition-colors ${
                isDisabled
                  ? 'bg-gray-300 text-white cursor-not-allowed'
                  : 'bg-black text-white hover:bg-gray-800'
              }`}
            >
              바로 구매
            </button>
          </div>
        </div>
      </div>

      {/* Product Detail Tabs */}
      <div className="mt-12 sm:mt-16">
        <div className="border-b border-gray-200">
          <div className="flex">
            <button
              onClick={() => setActiveTab('description')}
              className={`px-4 sm:px-6 py-3 text-sm font-medium border-b-2 transition-colors ${
                activeTab === 'description'
                  ? 'border-black text-black'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              상품 설명
            </button>
            {product.sizeGuides && product.sizeGuides.length > 0 && (
              <button
                onClick={() => setActiveTab('sizeGuide')}
                className={`px-4 sm:px-6 py-3 text-sm font-medium border-b-2 transition-colors ${
                  activeTab === 'sizeGuide'
                    ? 'border-black text-black'
                    : 'border-transparent text-gray-500 hover:text-gray-700'
                }`}
              >
                사이즈 정보
              </button>
            )}
            <button
              onClick={() => setActiveTab('shipping')}
              className={`px-4 sm:px-6 py-3 text-sm font-medium border-b-2 transition-colors ${
                activeTab === 'shipping'
                  ? 'border-black text-black'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              배송/교환/반품
            </button>
          </div>
        </div>

        <div className="py-8">
          {activeTab === 'description' && (
            <div className="prose prose-sm max-w-none">
              {product.description ? (
                <p className="text-sm text-gray-600 whitespace-pre-line leading-relaxed">
                  {product.description}
                </p>
              ) : (
                <p className="text-sm text-gray-400">상품 설명이 등록되지 않았습니다.</p>
              )}
            </div>
          )}

          {activeTab === 'sizeGuide' && product.sizeGuides && product.sizeGuides.length > 0 && (
            <div className="overflow-x-auto">
              <table className="w-full text-sm text-gray-600 border-collapse max-w-lg">
                <thead>
                  <tr className="border-b border-gray-200 bg-gray-50">
                    <th className="py-3 px-4 text-left font-medium">사이즈</th>
                    <th className="py-3 px-4 text-center font-medium">어깨(cm)</th>
                    <th className="py-3 px-4 text-center font-medium">가슴(cm)</th>
                    <th className="py-3 px-4 text-center font-medium">총장(cm)</th>
                    <th className="py-3 px-4 text-center font-medium">소매(cm)</th>
                  </tr>
                </thead>
                <tbody>
                  {product.sizeGuides.map((sg) => (
                    <tr key={sg.id} className="border-b border-gray-100">
                      <td className="py-3 px-4 font-medium">{sg.size}</td>
                      <td className="py-3 px-4 text-center">{sg.shoulderWidth}</td>
                      <td className="py-3 px-4 text-center">{sg.chestWidth}</td>
                      <td className="py-3 px-4 text-center">{sg.totalLength}</td>
                      <td className="py-3 px-4 text-center">{sg.sleeveLength}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {activeTab === 'shipping' && (
            <div className="space-y-6 text-sm text-gray-600">
              <div>
                <h3 className="font-semibold text-gray-900 mb-2">배송 안내</h3>
                <ul className="space-y-1 list-disc list-inside">
                  <li>배송비: 3,000원 (50,000원 이상 구매 시 무료배송)</li>
                  <li>배송 기간: 결제 완료 후 1~3일 이내 출고</li>
                  <li>도서산간 지역은 추가 배송비가 발생할 수 있습니다.</li>
                </ul>
              </div>
              <div>
                <h3 className="font-semibold text-gray-900 mb-2">교환/반품 안내</h3>
                <ul className="space-y-1 list-disc list-inside">
                  <li>수령 후 7일 이내 교환/반품 신청 가능</li>
                  <li>단순 변심에 의한 교환/반품 시 배송비는 고객 부담</li>
                  <li>상품 하자로 인한 교환/반품 시 배송비 무료</li>
                  <li>착용 또는 세탁한 상품은 교환/반품이 불가합니다.</li>
                </ul>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
