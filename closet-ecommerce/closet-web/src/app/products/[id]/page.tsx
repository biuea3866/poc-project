'use client';

export default function ProductDetailPage() {
  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-12">
        {/* Product Images */}
        <div>
          <div className="aspect-square bg-gray-200 rounded-lg animate-pulse" />
          <div className="grid grid-cols-4 gap-2 mt-2">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="aspect-square bg-gray-200 rounded animate-pulse" />
            ))}
          </div>
        </div>

        {/* Product Info */}
        <div className="space-y-6">
          {/* Brand */}
          <div className="h-4 bg-gray-200 rounded w-1/4 animate-pulse" />

          {/* Name */}
          <div className="h-8 bg-gray-200 rounded w-3/4 animate-pulse" />

          {/* Price */}
          <div className="h-6 bg-gray-200 rounded w-1/3 animate-pulse" />

          {/* Options */}
          <div>
            <h3 className="text-sm font-semibold mb-2">옵션</h3>
            <div className="flex gap-2">
              {['S', 'M', 'L', 'XL'].map((size) => (
                <button
                  key={size}
                  className="w-12 h-12 border border-gray-300 rounded-lg flex items-center justify-center text-sm hover:border-black"
                >
                  {size}
                </button>
              ))}
            </div>
          </div>

          {/* Quantity */}
          <div>
            <h3 className="text-sm font-semibold mb-2">수량</h3>
            <div className="flex items-center gap-2">
              <button className="w-10 h-10 border border-gray-300 rounded-lg flex items-center justify-center">
                -
              </button>
              <span className="w-12 text-center">1</span>
              <button className="w-10 h-10 border border-gray-300 rounded-lg flex items-center justify-center">
                +
              </button>
            </div>
          </div>

          {/* Add to Cart */}
          <button className="w-full bg-black text-white py-4 rounded-lg font-medium hover:bg-gray-800 transition-colors">
            장바구니 담기
          </button>

          {/* Description */}
          <div>
            <h3 className="text-sm font-semibold mb-2">상품 설명</h3>
            <div className="space-y-2">
              <div className="h-4 bg-gray-200 rounded w-full animate-pulse" />
              <div className="h-4 bg-gray-200 rounded w-5/6 animate-pulse" />
              <div className="h-4 bg-gray-200 rounded w-4/6 animate-pulse" />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
