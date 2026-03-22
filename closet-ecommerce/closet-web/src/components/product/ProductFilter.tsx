'use client';

import type { Category, Brand, ProductListParams } from '@/types/product';

interface ProductFilterProps {
  categories: Category[];
  brands: Brand[];
  filters: ProductListParams;
  onFilterChange: (filters: Partial<ProductListParams>) => void;
}

export default function ProductFilter({ categories, brands, filters, onFilterChange }: ProductFilterProps) {
  return (
    <aside className="w-full">
      <h2 className="text-lg font-bold mb-4">필터</h2>

      {/* Category */}
      <div className="mb-6">
        <h3 className="text-sm font-semibold mb-2">카테고리</h3>
        <div className="space-y-1">
          <button
            onClick={() => onFilterChange({ categoryId: undefined, page: 0 })}
            className={`block text-sm w-full text-left px-2 py-1 rounded ${
              !filters.categoryId ? 'bg-black text-white' : 'text-gray-700 hover:bg-gray-100'
            }`}
          >
            전체
          </button>
          {categories.map((cat) => (
            <button
              key={cat.id}
              onClick={() => onFilterChange({ categoryId: cat.id, page: 0 })}
              className={`block text-sm w-full text-left px-2 py-1 rounded ${
                filters.categoryId === cat.id ? 'bg-black text-white' : 'text-gray-700 hover:bg-gray-100'
              }`}
            >
              {cat.name}
            </button>
          ))}
        </div>
      </div>

      {/* Price Range */}
      <div className="mb-6">
        <h3 className="text-sm font-semibold mb-2">가격대</h3>
        <div className="flex gap-2">
          <input
            type="number"
            placeholder="최소"
            value={filters.minPrice || ''}
            onChange={(e) => onFilterChange({ minPrice: e.target.value ? Number(e.target.value) : undefined, page: 0 })}
            className="w-full px-2 py-1 border border-gray-300 rounded text-sm"
          />
          <span className="text-gray-400 flex-shrink-0">~</span>
          <input
            type="number"
            placeholder="최대"
            value={filters.maxPrice || ''}
            onChange={(e) => onFilterChange({ maxPrice: e.target.value ? Number(e.target.value) : undefined, page: 0 })}
            className="w-full px-2 py-1 border border-gray-300 rounded text-sm"
          />
        </div>
      </div>

      {/* Brand */}
      <div className="mb-6">
        <h3 className="text-sm font-semibold mb-2">브랜드</h3>
        <div className="space-y-1">
          <button
            onClick={() => onFilterChange({ brandId: undefined, page: 0 })}
            className={`block text-sm w-full text-left px-2 py-1 rounded ${
              !filters.brandId ? 'bg-black text-white' : 'text-gray-700 hover:bg-gray-100'
            }`}
          >
            전체
          </button>
          {brands.map((brand) => (
            <button
              key={brand.id}
              onClick={() => onFilterChange({ brandId: brand.id, page: 0 })}
              className={`block text-sm w-full text-left px-2 py-1 rounded ${
                filters.brandId === brand.id ? 'bg-black text-white' : 'text-gray-700 hover:bg-gray-100'
              }`}
            >
              {brand.name}
            </button>
          ))}
        </div>
      </div>

      {/* Reset */}
      <button
        onClick={() => onFilterChange({ categoryId: undefined, brandId: undefined, minPrice: undefined, maxPrice: undefined, page: 0 })}
        className="w-full text-sm text-gray-500 border border-gray-300 rounded py-2 hover:bg-gray-50"
      >
        필터 초기화
      </button>
    </aside>
  );
}
