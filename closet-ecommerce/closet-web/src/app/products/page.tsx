'use client';

import ProductFilter from '@/components/product/ProductFilter';

export default function ProductsPage() {
  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-8">상품 목록</h1>
      <div className="flex gap-8">
        {/* Sidebar Filter */}
        <div className="w-64 flex-shrink-0 hidden lg:block">
          <ProductFilter />
        </div>

        {/* Product Grid */}
        <div className="flex-1">
          {/* Sort bar */}
          <div className="flex justify-between items-center mb-6">
            <p className="text-sm text-gray-500">상품을 불러오는 중...</p>
            <select className="px-3 py-1.5 border border-gray-300 rounded-lg text-sm">
              <option>신상품순</option>
              <option>가격 낮은순</option>
              <option>가격 높은순</option>
              <option>인기순</option>
            </select>
          </div>

          {/* Skeleton Grid */}
          <div className="grid grid-cols-2 md:grid-cols-3 gap-6">
            {[1, 2, 3, 4, 5, 6].map((i) => (
              <div key={i} className="animate-pulse">
                <div className="aspect-[3/4] bg-gray-200 rounded-lg mb-3" />
                <div className="h-3 bg-gray-200 rounded w-1/3 mb-2" />
                <div className="h-4 bg-gray-200 rounded w-2/3 mb-2" />
                <div className="h-4 bg-gray-200 rounded w-1/4" />
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
